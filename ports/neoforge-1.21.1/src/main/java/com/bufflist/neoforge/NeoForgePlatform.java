package com.bufflist.neoforge;

import com.bufflist.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;

final class NeoForgePlatform implements Platform {
    private static final Field SHOW_ICON;

    static {
        Field f = null;
        try {
            f = MobEffectInstance.class.getDeclaredField("showIcon");
            f.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {}
        SHOW_ICON = f;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public String modName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(c -> c.getModInfo().getDisplayName()).orElse(namespace);
    }

    @Override
    public @Nullable ResourceLocation effectId(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.getKey(effect);
    }

    @Override
    public boolean visibleInInventory(MobEffectInstance inst) {
        return IClientMobEffectExtensions.of(inst).isVisibleInInventory(inst);
    }

    @Override
    public boolean visibleInGui(MobEffectInstance inst) {
        return IClientMobEffectExtensions.of(inst).isVisibleInGui(inst);
    }

    @Override
    public boolean renderCustomIcon(GuiGraphics g, MobEffectInstance inst, @Nullable EffectRenderingInventoryScreen<?> screen,
                                    int x, int y, int size) {
        IClientMobEffectExtensions ext = IClientMobEffectExtensions.of(inst);
        g.enableScissor(x, y, x + size, y + size);
        try {
            boolean custom = false;
            if (screen != null) custom = ext.renderInventoryIcon(inst, screen, g, 0, -7, 0);
            if (!custom) custom = ext.renderGuiIcon(inst, Minecraft.getInstance().gui, g, -3, -3, 0, 1f);
            return custom;
        } finally {
            g.disableScissor();
        }
    }

    @Override
    public String debugInfo(MobEffectInstance inst) {
        return " | ext=" + IClientMobEffectExtensions.of(inst).getClass().getName();
    }

    @Override
    public void setShowIcon(MobEffectInstance inst, boolean value) {
        if (SHOW_ICON == null) return;
        try {
            SHOW_ICON.setBoolean(inst, value);
        } catch (IllegalAccessException ignored) {}
    }

    @Override
    public int guiLeft(AbstractContainerScreen<?> screen) {
        return screen.getGuiLeft();
    }

    @Override
    public int guiTop(AbstractContainerScreen<?> screen) {
        return screen.getGuiTop();
    }

    @Override
    public int imageWidth(AbstractContainerScreen<?> screen) {
        return screen.getXSize();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
