package com.bufflist.neoforge;

import com.bufflist.BuffListDebug;
import com.bufflist.EffectListRenderer;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** 把 NeoForge 事件转给公共代码。 */
public final class NeoForgeEvents {
    private final EffectListRenderer r = EffectListRenderer.INSTANCE;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onVanillaInventoryEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (EffectListRenderer.replacesInventoryEffects()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onScreenRender(ScreenEvent.Render.Post event) {
        r.renderScreen(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (r.mousePressed(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (r.mouseDragged(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (r.mouseReleased(event.getScreen())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onScreenClosing(ScreenEvent.Closing event) {
        r.screenClosing();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiPre(RenderGuiEvent.Pre event) {
        r.guiPre();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onGuiPost(RenderGuiEvent.Post event) {
        r.guiPost();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        r.clientTick();
    }

    @SubscribeEvent
    public void onLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.EFFECTS)) return;
        var g = event.getGuiGraphics();
        if (r.renderHud(g, g.guiWidth(), g.guiHeight())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bufflist")
                .then(Commands.literal("dump").executes(ctx -> BuffListDebug.dump())));
    }
}
