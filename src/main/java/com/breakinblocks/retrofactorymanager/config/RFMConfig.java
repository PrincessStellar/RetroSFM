package com.breakinblocks.retrofactorymanager.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class RFMConfig {
    public static final RFMConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<RFMConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(RFMConfig::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    public final ModConfigSpec.BooleanValue setBlueprintAsDefaultEditor;

    private RFMConfig(ModConfigSpec.Builder builder) {
        setBlueprintAsDefaultEditor = builder
                .comment("When true, the Blueprint editor is set as SFM's preferred program editor on game load.",
                        "Switching editors in-game sets this to false so your choice sticks.")
                .define("setBlueprintAsDefaultEditor", true);
    }
}
