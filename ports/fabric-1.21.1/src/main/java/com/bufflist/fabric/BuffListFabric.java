package com.bufflist.fabric;

import com.bufflist.BuffListConfig;
import com.bufflist.BuffListDebug;
import com.bufflist.EffectListRenderer;
import com.bufflist.Platform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

public final class BuffListFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Platform.set(new FabricPlatform());
        BuffListConfig.init(Platform.get().configDir());
        EffectListRenderer r = EffectListRenderer.INSTANCE;

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof EffectRenderingInventoryScreen<?>)) return;
            ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> r.renderScreen(s, g, mx, my));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> !r.mousePressed(s, mx, my, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mx, my, button) -> !r.mouseReleased(s));
            ScreenEvents.remove(screen).register(s -> r.screenClosing());
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> r.clientTick());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
                ClientCommandManager.literal("bufflist")
                        .then(ClientCommandManager.literal("dump").executes(c -> BuffListDebug.dump()))));
    }
}
