package com.bufflist.forge;

import com.bufflist.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

final class ForgePlatform implements Platform {
    private static final Field SHOW_ICON = ObfuscationReflectionHelper.findField(MobEffectInstance.class, "f_19509_");

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
        return ForgeRegistries.MOB_EFFECTS.getKey(effect);
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
            // 背包约定：图标在 (x, y+7)；HUD 约定：背景格在 (x,y)，图标在 (x+3,y+3)
            boolean custom = false;
            if (screen != null) custom = ext.renderInventoryIcon(inst, screen, g, 0, -7, 0);
            if (!custom) custom = ext.renderGuiIcon(inst, Minecraft.getInstance().gui, g, -3, -3, 0, 1f);
            return custom;
        } finally {
            g.disableScissor();
        }
    }

    @Override
    public @Nullable List<Component> gatherTooltip(EffectRenderingInventoryScreen<?> screen, MobEffectInstance inst) {
        return ApothicCompat.gather(screen, inst);
    }

    @Override
    public String debugInfo(MobEffectInstance inst) {
        return " | ext=" + IClientMobEffectExtensions.of(inst).getClass().getName();
    }

    @Override
    public void setShowIcon(MobEffectInstance inst, boolean value) {
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
