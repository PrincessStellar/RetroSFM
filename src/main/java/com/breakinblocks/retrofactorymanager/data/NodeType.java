package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;
import java.util.Locale;

public enum NodeType {
    TIMER_TRIGGER("Timer", NodeCategory.TRIGGER, List.of(PinRole.EXEC_OUT)),
    REDSTONE_TRIGGER("Redstone Pulse", NodeCategory.TRIGGER, List.of(PinRole.EXEC_OUT)),
    INPUT("Input", NodeCategory.IO, List.of(PinRole.EXEC_IN, PinRole.EXEC_OUT)),
    OUTPUT("Output", NodeCategory.IO, List.of(PinRole.EXEC_IN, PinRole.EXEC_OUT)),
    IF("If", NodeCategory.FLOW, List.of(PinRole.EXEC_IN, PinRole.TRUE_OUT, PinRole.FALSE_OUT, PinRole.AFTER_OUT)),
    FORGET("Forget", NodeCategory.FLOW, List.of(PinRole.EXEC_IN, PinRole.EXEC_OUT)),
    COMMENT("Comment", NodeCategory.MISC, List.of(PinRole.EXEC_IN, PinRole.EXEC_OUT));

    public static final Codec<NodeType> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown node type: " + name);
                }
            },
            type -> type.name().toLowerCase(Locale.ROOT)
    );

    private final String displayName;
    private final NodeCategory category;
    private final List<PinRole> pins;
    private final List<PinRole> outputPins;
    private final String translationKey;

    NodeType(String displayName, NodeCategory category, List<PinRole> pins) {
        this.displayName = displayName;
        this.category = category;
        this.pins = pins;
        this.outputPins = pins.stream().filter(PinRole::isOutput).toList();
        this.translationKey = "node.retrofactorymanager." + name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public String translationKey() {
        return translationKey;
    }

    public NodeCategory category() {
        return category;
    }

    public List<PinRole> pins() {
        return pins;
    }

    public List<PinRole> outputPins() {
        return outputPins;
    }

    public boolean hasPin(PinRole role) {
        return pins.contains(role);
    }

    public boolean isTrigger() {
        return category == NodeCategory.TRIGGER;
    }

    /// The pin that continues the statement sequence after this node.
    public PinRole continuationPin() {
        return this == IF ? PinRole.AFTER_OUT : PinRole.EXEC_OUT;
    }
}
