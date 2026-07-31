package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum IoSide {
    NORTH("N"),
    EAST("E"),
    SOUTH("S"),
    WEST("W"),
    TOP("Top"),
    BOTTOM("Bot"),
    LEFT("L"),
    RIGHT("R"),
    FRONT("Fr"),
    BACK("Bk");

    public static final Codec<IoSide> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown side: " + name);
                }
            },
            side -> side.name().toLowerCase(Locale.ROOT)
    );

    private final String shortName;

    IoSide(String shortName) {
        this.shortName = shortName;
    }

    public String shortName() {
        return shortName;
    }

    public String translationKey() {
        return "side.retrofactorymanager." + name().toLowerCase(Locale.ROOT);
    }
}
