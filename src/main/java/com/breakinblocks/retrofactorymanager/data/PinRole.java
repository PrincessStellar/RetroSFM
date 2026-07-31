package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum PinRole {
    EXEC_IN(false, ""),
    EXEC_OUT(true, ""),
    TRUE_OUT(true, "pin.retrofactorymanager.true"),
    FALSE_OUT(true, "pin.retrofactorymanager.false"),
    AFTER_OUT(true, "pin.retrofactorymanager.after");

    public static final Codec<PinRole> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown pin role: " + name);
                }
            },
            role -> role.name().toLowerCase(Locale.ROOT)
    );

    private final boolean output;
    private final String labelKey;

    PinRole(boolean output, String labelKey) {
        this.output = output;
        this.labelKey = labelKey;
    }

    public boolean isOutput() {
        return output;
    }

    public String labelKey() {
        return labelKey;
    }

    public boolean hasLabel() {
        return !labelKey.isEmpty();
    }
}
