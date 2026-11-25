package com.bufflist.forge;

import com.bufflist.BuffListConfig;
import com.bufflist.BuffListSettingsScreen;
import com.bufflist.Platform;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

/** 客户端初始化，单独成类避免服务端加载客户端类。 */
public final class ForgeEntry {
    private ForgeEntry() {}

    public static void init() {
        Platform.set(new ForgePlatform());
        BuffListConfig.init(Platform.get().configDir());
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new BuffListSettingsScreen(parent)));
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
    }
}
