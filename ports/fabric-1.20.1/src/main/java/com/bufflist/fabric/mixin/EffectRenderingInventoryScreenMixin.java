package com.bufflist.fabric.mixin;

import com.bufflist.EffectListRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 列表开启时不画原版背包里的效果图标。 */
@Mixin(EffectRenderingInventoryScreen.class)
public abstract class EffectRenderingInventoryScreenMixin {
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void bufflist$renderEffects(GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
        if (EffectListRenderer.replacesInventoryEffects()) ci.cancel();
    }
}
