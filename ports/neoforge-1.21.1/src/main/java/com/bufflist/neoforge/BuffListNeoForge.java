package com.bufflist.neoforge;

import com.bufflist.BuffListConfig;
import com.bufflist.BuffListSettingsScreen;
import com.bufflist.Platform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "bufflist", dist = Dist.CLIENT)
public final class BuffListNeoForge {
    public BuffListNeoForge(IEventBus modBus, ModContainer container) {
        Platform.set(new NeoForgePlatform());
        BuffListConfig.init(Platform.get().configDir());
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> new BuffListSettingsScreen(parent));
        NeoForge.EVENT_BUS.register(new NeoForgeEvents());
    }
}
