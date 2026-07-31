package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ConditionRow(
        boolean redstone,
        boolean negate,
        String setOp,
        String label,
        String comparison,
        int amount,
        String resource
) {
    public static final List<String> COMPARISONS = List.of(">", "<", "=", ">=", "<=");
    public static final List<String> SET_OPS = List.of("", "OVERALL", "SOME", "EVERY", "EACH", "ONE", "LONE");
    public static final ConditionRow DEFAULT = new ConditionRow(true, false, "", "", ">", 0, "");
    public static final ConditionRow DEFAULT_LABEL = new ConditionRow(false, false, "", "", ">", 0, "");

    public static final Codec<ConditionRow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("redstone", false).forGetter(ConditionRow::redstone),
            Codec.BOOL.optionalFieldOf("negate", false).forGetter(ConditionRow::negate),
            Codec.STRING.optionalFieldOf("set_op", "").forGetter(ConditionRow::setOp),
            Codec.STRING.optionalFieldOf("label", "").forGetter(ConditionRow::label),
            Codec.STRING.optionalFieldOf("comparison", ">").forGetter(ConditionRow::comparison),
            Codec.INT.optionalFieldOf("amount", 0).forGetter(ConditionRow::amount),
            Codec.STRING.optionalFieldOf("resource", "").forGetter(ConditionRow::resource)
    ).apply(instance, ConditionRow::new));

    public String toSfml() {
        if (redstone) {
            return "REDSTONE " + comparison + " " + amount;
        }
        StringBuilder sb = new StringBuilder();
        if (!setOp.isEmpty()) {
            sb.append(setOp).append(" ");
        }
        sb.append(SfmlGenerator.quoteLabel(label.isEmpty() ? "_unset" : label));
        sb.append(" HAS ").append(comparison).append(" ").append(amount);
        if (!resource.isBlank()) {
            sb.append(" ").append(resource.trim());
        }
        return sb.toString();
    }
}
