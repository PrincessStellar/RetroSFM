package com.breakinblocks.retrofactorymanager;

import com.breakinblocks.retrofactorymanager.client.RFMClient;
import com.breakinblocks.retrofactorymanager.config.RFMConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(RetroFactoryManager.MOD_ID)
public class RetroFactoryManager {
    public static final String MOD_ID = "retrofactorymanager";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public RetroFactoryManager(IEventBus bus, ModContainer container) {
        if (FMLEnvironment.dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, RFMConfig.CLIENT_SPEC);
            RFMClient.init(bus);
        }
    }
}
