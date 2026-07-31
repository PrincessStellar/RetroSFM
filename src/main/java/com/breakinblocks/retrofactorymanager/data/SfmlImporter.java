package com.breakinblocks.retrofactorymanager.data;

import ca.teamdman.sfml.ast.Block;
import ca.teamdman.sfml.ast.BoolConjunction;
import ca.teamdman.sfml.ast.BoolDisjunction;
import ca.teamdman.sfml.ast.BoolExpr;
import ca.teamdman.sfml.ast.BoolHas;
import ca.teamdman.sfml.ast.BoolNegation;
import ca.teamdman.sfml.ast.BoolParen;
import ca.teamdman.sfml.ast.BoolRedstone;
import ca.teamdman.sfml.ast.ForgetStatement;
import ca.teamdman.sfml.ast.IfStatement;
import ca.teamdman.sfml.ast.InputStatement;
import ca.teamdman.sfml.ast.Interval;
import ca.teamdman.sfml.ast.Label;
import ca.teamdman.sfml.ast.LabelAccess;
import ca.teamdman.sfml.ast.Limit;
import ca.teamdman.sfml.ast.NumberRange;
import ca.teamdman.sfml.ast.OutputStatement;
import ca.teamdman.sfml.ast.Program;
import ca.teamdman.sfml.ast.RedstoneTrigger;
import ca.teamdman.sfml.ast.ResourceIdentifier;
import ca.teamdman.sfml.ast.ResourceLimit;
import ca.teamdman.sfml.ast.ResourceLimits;
import ca.teamdman.sfml.ast.Side;
import ca.teamdman.sfml.ast.SideQualifier;
import ca.teamdman.sfml.ast.Statement;
import ca.teamdman.sfml.ast.TimerTrigger;
import ca.teamdman.sfml.ast.Trigger;
import ca.teamdman.sfml.ast.With;
import ca.teamdman.sfml.ast.WithAlwaysTrue;
import ca.teamdman.sfml.ast.WithClause;
import ca.teamdman.sfml.ast.WithConjunction;
import ca.teamdman.sfml.ast.WithDisjunction;
import ca.teamdman.sfml.ast.WithParen;
import ca.teamdman.sfml.ast.WithTag;
import ca.teamdman.sfml.program_builder.ProgramBuildResult;
import ca.teamdman.sfml.program_builder.ProgramBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SfmlImporter {
    public static final double TRIGGER_SPACING_Y = 220;
    public static final double NODE_SPACING_X = 240;
    public static final double BRANCH_SPACING_Y = 110;

    public record Result(BlueprintGraph graph, boolean lossless, List<String> notes) {
    }

    private SfmlImporter() {
    }

    public static Optional<Result> importProgram(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        try {
            ProgramBuildResult built = new ProgramBuilder(source).useCache(false).build();
            if (!built.metadata().errors().isEmpty() || built.program() == null) {
                return Optional.empty();
            }
            return Optional.of(importProgram(built.program(), SfmlImporter::reparse));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static Program reparse(String source) {
        ProgramBuildResult built = new ProgramBuilder(source).useCache(false).build();
        return built.metadata().errors().isEmpty() ? built.program() : null;
    }

    /// AST-only import path. {@code reparser} re-parses generated SFML for the fidelity check
    /// and may return null; it is separated so the walk can be exercised without a game environment.
    public static Result importProgram(Program program, java.util.function.Function<String, Program> reparser) {
        List<String> notes = new ArrayList<>();
        BlueprintGraph graph = new BlueprintGraph();
        if (program.name() != null) {
            graph.setName(program.name());
        }
        double y = 0;
        for (Trigger trigger : program.triggers()) {
            BlueprintNode triggerNode = triggerNode(trigger, y, notes);
            graph.addNode(triggerNode);
            layoutBlock(graph, trigger.getBlock(), triggerNode, PinRole.EXEC_OUT, NODE_SPACING_X, y, notes);
            y += TRIGGER_SPACING_Y;
        }

        boolean lossless = notes.isEmpty() && matchesOriginal(program, graph, reparser);
        return new Result(graph, lossless, notes);
    }

    private static boolean matchesOriginal(
            Program original,
            BlueprintGraph graph,
            java.util.function.Function<String, Program> reparser
    ) {
        try {
            Program rebuilt = reparser.apply(SfmlGenerator.generate(graph));
            return rebuilt != null && canonical(original).equals(canonical(rebuilt));
        } catch (Throwable t) {
            return false;
        }
    }

    private static String canonical(Program program) {
        StringBuilder sb = new StringBuilder();
        if (program.name() != null) {
            sb.append(program.name()).append('\n');
        }
        for (Trigger trigger : program.triggers()) {
            sb.append(trigger).append('\n');
        }
        return sb.toString()
                .replace("(", "")
                .replace(")", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static BlueprintNode triggerNode(Trigger trigger, double y, List<String> notes) {
        if (trigger instanceof TimerTrigger timer) {
            Interval interval = timer.interval();
            boolean global = interval.alignment() == Interval.IntervalAlignment.GLOBAL;
            return new BlueprintNode(
                    java.util.UUID.randomUUID(),
                    0,
                    y,
                    new NodeSettings.Timer(interval.ticks(), false, global, interval.offset())
            );
        }
        if (!(trigger instanceof RedstoneTrigger)) {
            notes.add("Unsupported trigger type: " + trigger.getClass().getSimpleName());
        }
        return BlueprintNode.create(NodeType.REDSTONE_TRIGGER, 0, y);
    }

    private static void layoutBlock(
            BlueprintGraph graph,
            Block block,
            BlueprintNode previousNode,
            PinRole previousPin,
            double x,
            double y,
            List<String> notes
    ) {
        BlueprintNode chainFrom = previousNode;
        PinRole chainPin = previousPin;
        double cursorX = x;
        for (Statement statement : block.statements()) {
            BlueprintNode node = statementNode(statement, cursorX, y, notes);
            if (node == null) {
                notes.add("Skipped unsupported statement: " + statement.getClass().getSimpleName());
                continue;
            }
            graph.addNode(node);
            graph.connect(chainFrom.id(), chainPin, node.id());
            if (statement instanceof IfStatement ifStatement) {
                int trueCount = ifStatement.trueBlock().statements().size();
                int falseCount = ifStatement.falseBlock().statements().size();
                layoutBlock(graph, ifStatement.trueBlock(), node, PinRole.TRUE_OUT,
                        cursorX + NODE_SPACING_X, y - BRANCH_SPACING_Y, notes);
                if (falseCount > 0) {
                    layoutBlock(graph, ifStatement.falseBlock(), node, PinRole.FALSE_OUT,
                            cursorX + NODE_SPACING_X, y + BRANCH_SPACING_Y, notes);
                }
                cursorX += NODE_SPACING_X * (1 + Math.max(trueCount, falseCount));
            } else {
                cursorX += NODE_SPACING_X;
            }
            chainFrom = node;
            chainPin = node.type().continuationPin();
        }
    }

    private static BlueprintNode statementNode(Statement statement, double x, double y, List<String> notes) {
        if (statement instanceof InputStatement input) {
            NodeSettings.Io io = (NodeSettings.Io) NodeSettings.defaultFor(NodeType.INPUT);
            io = applyLabelAccess(io, input.labelAccess(), notes).withEach(input.each());
            io = applyLimits(io, input.resourceLimits(), true, notes);
            return new BlueprintNode(java.util.UUID.randomUUID(), x, y, io);
        }
        if (statement instanceof OutputStatement output) {
            NodeSettings.Output settings = (NodeSettings.Output) NodeSettings.defaultFor(NodeType.OUTPUT);
            NodeSettings.Io io = applyLabelAccess(settings, output.labelAccess(), notes).withEach(output.each());
            io = applyLimits(io, output.resourceLimits(), false, notes);
            io = ((NodeSettings.Output) io).withEmptySlotsOnly(output.emptySlotsOnly());
            return new BlueprintNode(java.util.UUID.randomUUID(), x, y, io);
        }
        if (statement instanceof ForgetStatement forget) {
            List<String> labels = forget.labelToForget().stream().map(Label::name).sorted().toList();
            return new BlueprintNode(java.util.UUID.randomUUID(), x, y, new NodeSettings.Forget(labels));
        }
        if (statement instanceof IfStatement ifStatement) {
            return new BlueprintNode(java.util.UUID.randomUUID(), x, y, importCondition(ifStatement.condition()));
        }
        return null;
    }

    private static NodeSettings.If importCondition(BoolExpr condition) {
        List<ConditionRow> rows = new ArrayList<>();
        Boolean joinOr = flatten(condition, rows, null);
        if (joinOr == null) {
            return new NodeSettings.If(condition.toString(), List.of(), false, true);
        }
        return new NodeSettings.If("", rows, joinOr, false);
    }

    /// Returns TRUE if the tree joins with OR, FALSE for AND, null when it cannot be flattened.
    private static Boolean flatten(BoolExpr expr, List<ConditionRow> rows, Boolean joinOr) {
        if (expr instanceof BoolParen paren) {
            return flatten(paren.inner(), rows, joinOr);
        }
        if (expr instanceof BoolConjunction and) {
            if (Boolean.TRUE.equals(joinOr)) {
                return null;
            }
            Boolean left = flatten(and.left(), rows, Boolean.FALSE);
            if (left == null) {
                return null;
            }
            Boolean right = flatten(and.right(), rows, Boolean.FALSE);
            return right == null ? null : Boolean.FALSE;
        }
        if (expr instanceof BoolDisjunction or) {
            if (Boolean.FALSE.equals(joinOr)) {
                return null;
            }
            Boolean left = flatten(or.left(), rows, Boolean.TRUE);
            if (left == null) {
                return null;
            }
            Boolean right = flatten(or.right(), rows, Boolean.TRUE);
            return right == null ? null : Boolean.TRUE;
        }
        ConditionRow row = leafRow(expr);
        if (row == null) {
            return null;
        }
        rows.add(row);
        return joinOr == null ? Boolean.FALSE : joinOr;
    }

    private static ConditionRow leafRow(BoolExpr expr) {
        boolean negate = false;
        BoolExpr inner = expr;
        while (inner instanceof BoolNegation negation) {
            negate = !negate;
            inner = negation.inner();
        }
        while (inner instanceof BoolParen paren) {
            inner = paren.inner();
        }
        if (inner instanceof BoolRedstone redstone) {
            return new ConditionRow(true, negate, "", "", redstone.operator().toString(), (int) redstone.number(), "");
        }
        if (inner instanceof BoolHas has) {
            LabelAccess access = has.labelAccess();
            if (access.labels().size() != 1
                    || !access.sides().equals(SideQualifier.NULL)
                    || access.roundRobin().isEnabled()
                    || hasSlots(access)) {
                return null;
            }
            if (!has.with().equals(With.ALWAYS_TRUE) || has.except().stream().findAny().isPresent()) {
                return null;
            }
            List<String> resources = has.resourceIdSet().stream()
                    .filter(id -> !id.equals(ResourceIdentifier.MATCH_ALL))
                    .map(ResourceIdentifier::toStringCondensed)
                    .toList();
            if (resources.size() > 1) {
                return null;
            }
            String setOp = has.setOperator().name().toUpperCase(Locale.ROOT);
            if (setOp.equals("OVERALL")) {
                setOp = "";
            }
            if (!ConditionRow.SET_OPS.contains(setOp)) {
                return null;
            }
            return new ConditionRow(
                    false,
                    negate,
                    setOp,
                    access.labels().getFirst().name(),
                    has.comparisonOperator().toString(),
                    (int) has.quantity(),
                    resources.isEmpty() ? "" : resources.getFirst()
            );
        }
        return null;
    }

    private static boolean hasSlots(LabelAccess access) {
        NumberRange[] ranges = access.slots().ranges();
        return ranges.length > 0 && !(ranges.length == 1 && ranges[0].equals(NumberRange.MAX_RANGE));
    }

    private static NodeSettings.Io applyLabelAccess(NodeSettings.Io io, LabelAccess access, List<String> notes) {
        NodeSettings.Io result = io.withLabels(access.labels().stream().map(Label::name).toList());
        result = result.withRoundRobin(switch (access.roundRobin().getBehaviour()) {
            case BY_LABEL -> RoundRobinMode.BY_LABEL;
            case BY_BLOCK -> RoundRobinMode.BY_BLOCK;
            default -> RoundRobinMode.NONE;
        });
        SideQualifier sides = access.sides();
        if (sides.equals(SideQualifier.ALL)) {
            result = result.withEachSide(true);
        } else if (!sides.equals(SideQualifier.NULL)) {
            List<IoSide> mapped = new ArrayList<>();
            for (Side side : sides.sides()) {
                if (side == Side.NULL) {
                    continue;
                }
                try {
                    mapped.add(IoSide.valueOf(side.name()));
                } catch (IllegalArgumentException e) {
                    notes.add("Unsupported side: " + side.name());
                }
            }
            result = result.withSides(mapped);
        }
        NumberRange[] ranges = access.slots().ranges();
        if (hasSlots(access)) {
            List<String> parts = new ArrayList<>();
            for (NumberRange range : ranges) {
                parts.add(range.start() == range.end()
                        ? String.valueOf(range.start())
                        : range.start() + "-" + range.end());
            }
            result = result.withSlots(String.join(",", parts));
        }
        return result;
    }

    private static NodeSettings.Io applyLimits(
            NodeSettings.Io io,
            ResourceLimits limits,
            boolean input,
            List<String> notes
    ) {
        List<String> exclusions = limits.exclusions().stream()
                .filter(id -> !id.equals(ResourceIdentifier.MATCH_ALL))
                .map(ResourceIdentifier::toStringCondensed)
                .toList();
        if (!exclusions.isEmpty()) {
            io = io.withExcept(exclusions);
        }
        List<ResourceLimit> list = limits.resourceLimitList();
        if (list.isEmpty()) {
            return io;
        }
        List<LimitEntry> entries = new ArrayList<>();
        for (ResourceLimit limit : list) {
            List<String> resources = limit.resourceIds().stream()
                    .filter(id -> !id.equals(ResourceIdentifier.MATCH_ALL))
                    .map(ResourceIdentifier::toStringCondensed)
                    .toList();
            Limit values = limit.limit();
            long quantity = values.quantity().number() == null ? -1 : values.quantity().number().value();
            long retention = values.retention().number() == null ? -1 : values.retention().number().value();
            int importedQuantity = quantity >= 0 && quantity != Long.MAX_VALUE
                    ? (int) Math.min(quantity, Integer.MAX_VALUE)
                    : -1;
            boolean defaultRetention = input ? retention == 0 : retention == Long.MAX_VALUE;
            int importedRetention = retention >= 0 && !defaultRetention
                    ? (int) Math.min(retention, Integer.MAX_VALUE)
                    : -1;
            LimitEntry entry = new LimitEntry(importedQuantity, importedRetention, resources);
            entry = applyTagFilter(entry, limit.with(), notes);
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries.isEmpty() ? io : io.withLimits(entries);
    }

    private static LimitEntry applyTagFilter(LimitEntry entry, With with, List<String> notes) {
        if (with == null || with.equals(With.ALWAYS_TRUE) || with.condition() instanceof WithAlwaysTrue) {
            return entry;
        }
        List<String> tags = new ArrayList<>();
        Boolean joinOr = flattenTags(with.condition(), tags, null);
        if (joinOr == null || tags.isEmpty()) {
            notes.add("A WITH tag expression was too complex to import and was dropped");
            return entry;
        }
        TagMode mode = with.mode() == With.WithMode.WITHOUT ? TagMode.WITHOUT : TagMode.WITH;
        return entry.withTags(tags).withTagsOr(joinOr).withTagMode(mode);
    }

    /// Flattens a tag expression into a homogeneous AND/OR list; null when it cannot be represented.
    private static Boolean flattenTags(WithClause clause, List<String> tags, Boolean joinOr) {
        if (clause instanceof WithParen paren) {
            return flattenTags(paren.inner(), tags, joinOr);
        }
        if (clause instanceof WithConjunction and) {
            if (Boolean.TRUE.equals(joinOr)) {
                return null;
            }
            return flattenTags(and.left(), tags, Boolean.FALSE) == null
                    || flattenTags(and.right(), tags, Boolean.FALSE) == null
                    ? null
                    : Boolean.FALSE;
        }
        if (clause instanceof WithDisjunction or) {
            if (Boolean.FALSE.equals(joinOr)) {
                return null;
            }
            return flattenTags(or.left(), tags, Boolean.TRUE) == null
                    || flattenTags(or.right(), tags, Boolean.TRUE) == null
                    ? null
                    : Boolean.TRUE;
        }
        if (clause instanceof WithTag tag) {
            tags.add(tag.tagMatcher().toString());
            return joinOr == null ? Boolean.FALSE : joinOr;
        }
        return null;
    }
}
