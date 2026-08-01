package com.breakinblocks.retrofactorymanager.datagen;

import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = RetroFactoryManager.MOD_ID)
public class RFMDataGen {
    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        event.createProvider(RFMLanguageProvider::new);
    }
}
