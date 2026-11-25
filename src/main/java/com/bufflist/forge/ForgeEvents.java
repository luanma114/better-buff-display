package com.bufflist.forge;

import com.bufflist.BuffListDebug;
import com.bufflist.EffectListRenderer;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 把 Forge 事件转给公共代码。 */
public final class ForgeEvents {
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
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) r.clientTick();
    }

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.POTION_ICONS.id())) return;
        int sw = event.getWindow().getGuiScaledWidth();
        int sh = event.getWindow().getGuiScaledHeight();
        if (r.renderHud(event.getGuiGraphics(), sw, sh)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bufflist")
                .then(Commands.literal("dump").executes(ctx -> BuffListDebug.dump())));
    }
}
