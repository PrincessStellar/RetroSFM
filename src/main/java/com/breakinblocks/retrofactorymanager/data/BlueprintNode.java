package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public final class BlueprintNode {
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(UUID.fromString(value));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Invalid UUID: " + value);
                }
            },
            UUID::toString
    );

    public static final Codec<BlueprintNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("id").forGetter(BlueprintNode::id),
            Codec.DOUBLE.fieldOf("x").forGetter(BlueprintNode::x),
            Codec.DOUBLE.fieldOf("y").forGetter(BlueprintNode::y),
            NodeSettings.CODEC.fieldOf("settings").forGetter(BlueprintNode::settings)
    ).apply(instance, BlueprintNode::new));

    private final UUID id;
    private double x;
    private double y;
    private NodeSettings settings;

    public BlueprintNode(UUID id, double x, double y, NodeSettings settings) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.settings = settings;
    }

    public static BlueprintNode create(NodeType type, double x, double y) {
        return new BlueprintNode(UUID.randomUUID(), x, y, NodeSettings.defaultFor(type));
    }

    public UUID id() {
        return id;
    }

    public NodeType type() {
        return settings.type();
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public NodeSettings settings() {
        return settings;
    }

    public void setSettings(NodeSettings settings) {
        if (settings.type() != type()) {
            throw new IllegalArgumentException("Settings type mismatch: " + settings.type() + " != " + type());
        }
        this.settings = settings;
    }
}
