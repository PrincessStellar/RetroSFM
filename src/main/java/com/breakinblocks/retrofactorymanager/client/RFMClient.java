package com.breakinblocks.retrofactorymanager.client;

import com.breakinblocks.retrofactorymanager.integration.RFMTextEditors;
import net.neoforged.bus.api.IEventBus;

public final class RFMClient {
    private RFMClient() {
    }

    public static void init(IEventBus bus) {
        RFMTextEditors.register(bus);
    }
}
