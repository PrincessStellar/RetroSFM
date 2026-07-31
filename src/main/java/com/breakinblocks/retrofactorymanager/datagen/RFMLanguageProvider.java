package com.breakinblocks.retrofactorymanager.datagen;

import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import com.breakinblocks.retrofactorymanager.common.RFMTranslations;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class RFMLanguageProvider extends LanguageProvider {
    public RFMLanguageProvider(PackOutput output) {
        super(output, RetroFactoryManager.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (RFMTranslations.Entry entry : RFMTranslations.entries()) {
            add(entry.key(), entry.english());
        }
    }
}
