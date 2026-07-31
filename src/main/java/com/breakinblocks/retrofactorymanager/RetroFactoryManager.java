package com.breakinblocks.retrofactorymanager;

import com.breakinblocks.retrofactorymanager.client.RFMClient;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(RetroFactoryManager.MOD_ID)
public class RetroFactoryManager {
    public static final String MOD_ID = "retrofactorymanager";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public RetroFactoryManager(IEventBus bus) {
        if (FMLEnvironment.getDist().isClient()) {
            RFMClient.init(bus);
        }
    }
}
