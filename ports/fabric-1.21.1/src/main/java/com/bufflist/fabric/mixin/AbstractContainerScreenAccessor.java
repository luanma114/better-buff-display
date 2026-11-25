package com.bufflist.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int bufflist$leftPos();

    @Accessor("topPos")
    int bufflist$topPos();

    @Accessor("imageWidth")
    int bufflist$imageWidth();
}
