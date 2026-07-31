package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum TagMode {
    NONE,
    WITH,
    WITHOUT;

    public static final Codec<TagMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown tag mode: " + name);
                }
            },
            mode -> mode.name().toLowerCase(Locale.ROOT)
    );

    public String translationKey() {
        return "tag_mode.retrofactorymanager." + name().toLowerCase(Locale.ROOT);
    }

    public TagMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
