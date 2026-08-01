package com.breakinblocks.retrofactorymanager.client;

import ca.teamdman.sfm.common.config.SFMConfig;
import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import com.breakinblocks.retrofactorymanager.config.RFMConfig;
import com.breakinblocks.retrofactorymanager.integration.RFMTextEditors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

public final class RFMDefaultEditorHandler {
    private RFMDefaultEditorHandler() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(FMLLoadCompleteEvent.class, RFMDefaultEditorHandler::onLoadComplete);
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            if (!RFMConfig.CLIENT.setBlueprintAsDefaultEditor.get()) {
                return;
            }
            String blueprintId = RFMTextEditors.BLUEPRINT.getId().toString();
            var preferredEditor = SFMConfig.CLIENT_TEXT_EDITOR_CONFIG.preferredEditor;
            if (!blueprintId.equals(preferredEditor.get())) {
                preferredEditor.set(blueprintId);
                preferredEditor.save();
                RetroFactoryManager.LOGGER.info("Set SFM preferred editor to {}", blueprintId);
            }
        });
    }
}
