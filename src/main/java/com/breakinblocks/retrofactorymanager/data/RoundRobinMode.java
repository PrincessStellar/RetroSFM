package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum RoundRobinMode {
    NONE(""),
    BY_LABEL("ROUND ROBIN BY LABEL"),
    BY_BLOCK("ROUND ROBIN BY BLOCK");

    public static final Codec<RoundRobinMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown round robin mode: " + name);
                }
            },
            mode -> mode.name().toLowerCase(Locale.ROOT)
    );

    private final String sfml;

    RoundRobinMode(String sfml) {
        this.sfml = sfml;
    }

    public String sfml() {
        return sfml;
    }

    public String translationKey() {
        return "round_robin.retrofactorymanager." + name().toLowerCase(Locale.ROOT);
    }

    public RoundRobinMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
