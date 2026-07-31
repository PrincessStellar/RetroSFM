package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public record BlueprintEdge(UUID fromNode, PinRole fromPin, UUID toNode, PinRole toPin) {
    public static final Codec<BlueprintEdge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlueprintNode.UUID_CODEC.fieldOf("from_node").forGetter(BlueprintEdge::fromNode),
            PinRole.CODEC.fieldOf("from_pin").forGetter(BlueprintEdge::fromPin),
            BlueprintNode.UUID_CODEC.fieldOf("to_node").forGetter(BlueprintEdge::toNode),
            PinRole.CODEC.optionalFieldOf("to_pin", PinRole.EXEC_IN).forGetter(BlueprintEdge::toPin)
    ).apply(instance, BlueprintEdge::new));

    public boolean touches(UUID nodeId) {
        return fromNode.equals(nodeId) || toNode.equals(nodeId);
    }

    public boolean touchesPin(UUID nodeId, PinRole role) {
        return (fromNode.equals(nodeId) && fromPin == role) || (toNode.equals(nodeId) && toPin == role);
    }
}
