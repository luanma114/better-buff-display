package com.bufflist.fabric.mixin;

import com.bufflist.EffectListRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void bufflist$renderHead(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        EffectListRenderer.INSTANCE.guiPre();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void bufflist$renderReturn(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        EffectListRenderer.INSTANCE.guiPost();
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void bufflist$renderEffects(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        if (EffectListRenderer.INSTANCE.renderHud(g, g.guiWidth(), g.guiHeight())) ci.cancel();
    }
}
