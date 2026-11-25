package com.bufflist.fabric;

import com.bufflist.Platform;
import com.bufflist.fabric.mixin.AbstractContainerScreenAccessor;
import com.bufflist.fabric.mixin.MobEffectInstanceAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

final class FabricPlatform implements Platform {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String modName(String namespace) {
        return FabricLoader.getInstance().getModContainer(namespace)
                .map(c -> c.getMetadata().getName()).orElse(namespace);
    }

    @Override
    public @Nullable ResourceLocation effectId(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.getKey(effect);
    }

    @Override
    public boolean visibleInInventory(MobEffectInstance inst) {
        return true;
    }

    @Override
    public boolean visibleInGui(MobEffectInstance inst) {
        return true;
    }

    @Override
    public void setShowIcon(MobEffectInstance inst, boolean value) {
        ((MobEffectInstanceAccessor) inst).bufflist$setShowIcon(value);
    }

    @Override
    public int guiLeft(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).bufflist$leftPos();
    }

    @Override
    public int guiTop(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).bufflist$topPos();
    }

    @Override
    public int imageWidth(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).bufflist$imageWidth();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
