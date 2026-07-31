package com.breakinblocks.retrofactorymanager.integration;

import ca.teamdman.sfm.client.registry.SFMTextEditors;
import ca.teamdman.sfm.client.text_editor.ISFMTextEditorRegistration;
import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RFMTextEditors {
    private static final DeferredRegister<ISFMTextEditorRegistration> EDITORS =
            DeferredRegister.create(SFMTextEditors.REGISTRY_ID, RetroFactoryManager.MOD_ID);

    public static final DeferredHolder<ISFMTextEditorRegistration, RFMBlueprintEditorRegistration> BLUEPRINT =
            EDITORS.register("blueprint", RFMBlueprintEditorRegistration::new);

    private RFMTextEditors() {
    }

    public static void register(IEventBus bus) {
        EDITORS.register(bus);
    }
}
