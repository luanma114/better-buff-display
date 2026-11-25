package com.bufflist.fabric.mixin;

import com.bufflist.EffectListRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    private int screenWidth;
    @Shadow
    private int screenHeight;

    @Inject(method = "render", at = @At("HEAD"))
    private void bufflist$renderHead(GuiGraphics g, float partialTick, CallbackInfo ci) {
        EffectListRenderer.INSTANCE.guiPre();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void bufflist$renderReturn(GuiGraphics g, float partialTick, CallbackInfo ci) {
        EffectListRenderer.INSTANCE.guiPost();
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void bufflist$renderEffects(GuiGraphics g, CallbackInfo ci) {
        if (EffectListRenderer.INSTANCE.renderHud(g, screenWidth, screenHeight)) ci.cancel();
    }
}
