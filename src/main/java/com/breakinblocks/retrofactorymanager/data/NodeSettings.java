package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public sealed interface NodeSettings
        permits NodeSettings.Timer, NodeSettings.RedstonePulse, NodeSettings.Io,
        NodeSettings.If, NodeSettings.Forget, NodeSettings.Comment {

    Codec<NodeSettings> CODEC = NodeType.CODEC.dispatch(NodeSettings::type, NodeSettings::codecFor);

    NodeType type();

    static MapCodec<? extends NodeSettings> codecFor(NodeType type) {
        return switch (type) {
            case TIMER_TRIGGER -> Timer.MAP_CODEC;
            case REDSTONE_TRIGGER -> RedstonePulse.MAP_CODEC;
            case INPUT -> Input.MAP_CODEC;
            case OUTPUT -> Output.MAP_CODEC;
            case IF -> If.MAP_CODEC;
            case FORGET -> Forget.MAP_CODEC;
            case COMMENT -> Comment.MAP_CODEC;
        };
    }

    /// Older embedded graphs stored a single limit clause as flat fields; fold those into the list form.
    private static List<LimitEntry> mergeLegacyLimits(
            List<LimitEntry> limits,
            int legacyQuantity,
            int legacyRetain,
            List<String> legacyResources
    ) {
        if (!limits.isEmpty()) {
            return limits;
        }
        LimitEntry legacy = new LimitEntry(legacyQuantity, legacyRetain, legacyResources);
        return legacy.isEmpty() ? List.of() : List.of(legacy);
    }

    static NodeSettings defaultFor(NodeType type) {
        return switch (type) {
            case TIMER_TRIGGER -> new Timer(20, false, false, 0);
            case REDSTONE_TRIGGER -> new RedstonePulse();
            case INPUT -> new Input(List.of(), false, List.of(), List.of(), RoundRobinMode.NONE, List.of(), false, "");
            case OUTPUT -> new Output(List.of(), false, false, List.of(), List.of(), RoundRobinMode.NONE, List.of(), false, "");
            case IF -> new If("", List.of(ConditionRow.DEFAULT), false, false);
            case FORGET -> new Forget(List.of());
            case COMMENT -> new Comment("comment");
        };
    }

    sealed interface Io extends NodeSettings permits Input, Output {
        List<String> labels();

        boolean each();

        List<LimitEntry> limits();

        List<String> except();

        RoundRobinMode roundRobin();

        List<IoSide> sides();

        boolean eachSide();

        String slots();

        Io withLabels(List<String> labels);

        Io withEach(boolean each);

        Io withLimits(List<LimitEntry> limits);

        Io withExcept(List<String> except);

        Io withRoundRobin(RoundRobinMode roundRobin);

        Io withSides(List<IoSide> sides);

        Io withEachSide(boolean eachSide);

        Io withSlots(String slots);

        /// Limit clauses with anything configured; empty when the statement takes SFM's defaults.
        default List<LimitEntry> activeLimits() {
            return limits().stream().filter(entry -> !entry.isEmpty()).toList();
        }

        default LimitEntry limitAt(int index) {
            List<LimitEntry> current = limits();
            return index >= 0 && index < current.size() ? current.get(index) : LimitEntry.EMPTY;
        }

        default Io withLimitAt(int index, LimitEntry entry) {
            List<LimitEntry> updated = new java.util.ArrayList<>(limits());
            while (updated.size() <= index) {
                updated.add(LimitEntry.EMPTY);
            }
            updated.set(index, entry);
            return withLimits(updated);
        }

        default Io withLimitRemoved(int index) {
            if (index < 0 || index >= limits().size()) {
                return this;
            }
            List<LimitEntry> updated = new java.util.ArrayList<>(limits());
            updated.remove(index);
            return withLimits(updated);
        }

        default Io withLimitAdded() {
            List<LimitEntry> updated = new java.util.ArrayList<>(limits());
            updated.add(LimitEntry.EMPTY);
            return withLimits(updated);
        }
    }

    record Timer(int amount, boolean seconds, boolean global, int offset) implements NodeSettings {
        public static final MapCodec<Timer> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("amount").forGetter(Timer::amount),
                Codec.BOOL.optionalFieldOf("seconds", false).forGetter(Timer::seconds),
                Codec.BOOL.optionalFieldOf("global", false).forGetter(Timer::global),
                Codec.INT.optionalFieldOf("offset", 0).forGetter(Timer::offset)
        ).apply(instance, Timer::new));

        @Override
        public NodeType type() {
            return NodeType.TIMER_TRIGGER;
        }

    }

    record RedstonePulse() implements NodeSettings {
        public static final MapCodec<RedstonePulse> MAP_CODEC = MapCodec.unit(new RedstonePulse());

        @Override
        public NodeType type() {
            return NodeType.REDSTONE_TRIGGER;
        }

    }

    record Input(
            List<String> labels,
            boolean each,
            List<LimitEntry> limits,
            List<String> except,
            RoundRobinMode roundRobin,
            List<IoSide> sides,
            boolean eachSide,
            String slots
    ) implements Io {
        public static final MapCodec<Input> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("labels", List.of()).forGetter(Input::labels),
                Codec.BOOL.optionalFieldOf("each", false).forGetter(Input::each),
                LimitEntry.CODEC.listOf().optionalFieldOf("limits", List.of()).forGetter(Input::limits),
                Codec.STRING.listOf().optionalFieldOf("except", List.of()).forGetter(Input::except),
                RoundRobinMode.CODEC.optionalFieldOf("round_robin", RoundRobinMode.NONE).forGetter(Input::roundRobin),
                Codec.INT.optionalFieldOf("quantity", -1).forGetter(ignored -> -1),
                Codec.INT.optionalFieldOf("retain", -1).forGetter(ignored -> -1),
                Codec.STRING.listOf().optionalFieldOf("resources", List.of()).forGetter(ignored -> List.of()),
                IoSide.CODEC.listOf().optionalFieldOf("sides", List.of()).forGetter(Input::sides),
                Codec.BOOL.optionalFieldOf("each_side", false).forGetter(Input::eachSide),
                Codec.STRING.optionalFieldOf("slots", "").forGetter(Input::slots)
        ).apply(instance, (labels, each, limits, except, roundRobin, legacyQuantity, legacyRetain, legacyResources, sides, eachSide, slots) ->
                new Input(
                        labels,
                        each,
                        mergeLegacyLimits(limits, legacyQuantity, legacyRetain, legacyResources),
                        except,
                        roundRobin,
                        sides,
                        eachSide,
                        slots
                )));

        @Override
        public NodeType type() {
            return NodeType.INPUT;
        }


        @Override
        public Io withLabels(List<String> labels) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withEach(boolean each) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withLimits(List<LimitEntry> limits) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withSides(List<IoSide> sides) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withEachSide(boolean eachSide) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withSlots(String slots) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withExcept(List<String> except) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withRoundRobin(RoundRobinMode roundRobin) {
            return new Input(labels, each, limits, except, roundRobin, sides, eachSide, slots);
        }
    }

    record Output(
            List<String> labels,
            boolean each,
            boolean emptySlotsOnly,
            List<LimitEntry> limits,
            List<String> except,
            RoundRobinMode roundRobin,
            List<IoSide> sides,
            boolean eachSide,
            String slots
    ) implements Io {
        public static final MapCodec<Output> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("labels", List.of()).forGetter(Output::labels),
                Codec.BOOL.optionalFieldOf("each", false).forGetter(Output::each),
                Codec.BOOL.optionalFieldOf("empty_slots_only", false).forGetter(Output::emptySlotsOnly),
                LimitEntry.CODEC.listOf().optionalFieldOf("limits", List.of()).forGetter(Output::limits),
                Codec.STRING.listOf().optionalFieldOf("except", List.of()).forGetter(Output::except),
                RoundRobinMode.CODEC.optionalFieldOf("round_robin", RoundRobinMode.NONE).forGetter(Output::roundRobin),
                Codec.INT.optionalFieldOf("quantity", -1).forGetter(ignored -> -1),
                Codec.INT.optionalFieldOf("retain", -1).forGetter(ignored -> -1),
                Codec.STRING.listOf().optionalFieldOf("resources", List.of()).forGetter(ignored -> List.of()),
                IoSide.CODEC.listOf().optionalFieldOf("sides", List.of()).forGetter(Output::sides),
                Codec.BOOL.optionalFieldOf("each_side", false).forGetter(Output::eachSide),
                Codec.STRING.optionalFieldOf("slots", "").forGetter(Output::slots)
        ).apply(instance, (labels, each, emptyOnly, limits, except, roundRobin, legacyQuantity, legacyRetain, legacyResources, sides, eachSide, slots) ->
                new Output(
                        labels,
                        each,
                        emptyOnly,
                        mergeLegacyLimits(limits, legacyQuantity, legacyRetain, legacyResources),
                        except,
                        roundRobin,
                        sides,
                        eachSide,
                        slots
                )));

        @Override
        public NodeType type() {
            return NodeType.OUTPUT;
        }


        public Output withEmptySlotsOnly(boolean emptySlotsOnly) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withLabels(List<String> labels) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withEach(boolean each) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withLimits(List<LimitEntry> limits) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withSides(List<IoSide> sides) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withEachSide(boolean eachSide) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withSlots(String slots) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withExcept(List<String> except) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }

        @Override
        public Io withRoundRobin(RoundRobinMode roundRobin) {
            return new Output(labels, each, emptySlotsOnly, limits, except, roundRobin, sides, eachSide, slots);
        }
    }

    record If(String condition, List<ConditionRow> rows, boolean joinOr, boolean useText) implements NodeSettings {
        public static final MapCodec<If> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("condition", "").forGetter(If::condition),
                ConditionRow.CODEC.listOf().optionalFieldOf("rows", List.of()).forGetter(If::rows),
                Codec.BOOL.optionalFieldOf("join_or", false).forGetter(If::joinOr),
                Codec.BOOL.optionalFieldOf("use_text", true).forGetter(If::useText)
        ).apply(instance, If::new));

        @Override
        public NodeType type() {
            return NodeType.IF;
        }

        public String conditionSfml() {
            if (useText) {
                return condition.isBlank() ? "TRUE" : condition.strip();
            }
            if (rows.isEmpty()) {
                return "TRUE";
            }
            return rows.stream()
                    .map(row -> (row.negate() ? "NOT " : "") + "(" + row.toSfml() + ")")
                    .reduce((a, b) -> a + (joinOr ? " OR " : " AND ") + b)
                    .orElse("TRUE");
        }

    }

    record Forget(List<String> labels) implements NodeSettings {
        public static final MapCodec<Forget> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("labels", List.of()).forGetter(Forget::labels)
        ).apply(instance, Forget::new));

        @Override
        public NodeType type() {
            return NodeType.FORGET;
        }

    }

    record Comment(String text) implements NodeSettings {
        public static final MapCodec<Comment> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("text", "").forGetter(Comment::text)
        ).apply(instance, Comment::new));

        @Override
        public NodeType type() {
            return NodeType.COMMENT;
        }

    }
}
