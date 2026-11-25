package com.bufflist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/** 各加载器（Forge / Fabric / NeoForge）各自实现的那一小部分。公共代码只通过这里访问加载器 API。 */
public interface Platform {
    boolean isModLoaded(String modId);

    /** 模组显示名，找不到时返回 namespace。 */
    String modName(String namespace);

    @Nullable
    ResourceLocation effectId(MobEffect effect);

    boolean visibleInInventory(MobEffectInstance inst);

    boolean visibleInGui(MobEffectInstance inst);

    /** 效果没有标准贴图时交给模组自己画（Forge 的 IClientMobEffectExtensions）。画了返回 true。 */
    default boolean renderCustomIcon(GuiGraphics g, MobEffectInstance inst, @Nullable EffectRenderingInventoryScreen<?> screen,
                                     int x, int y, int size) {
        return false;
    }

    /** 其他模组往原版效果提示里加的行（Apothic Attributes）；没有返回 null。 */
    @Nullable
    default List<Component> gatherTooltip(EffectRenderingInventoryScreen<?> screen, MobEffectInstance inst) {
        return null;
    }

    /** /bufflist dump 里附加的加载器相关信息。 */
    default String debugInfo(MobEffectInstance inst) {
        return "";
    }

    void setShowIcon(MobEffectInstance inst, boolean value);

    int guiLeft(AbstractContainerScreen<?> screen);

    int guiTop(AbstractContainerScreen<?> screen);

    int imageWidth(AbstractContainerScreen<?> screen);

    Path configDir();

    final class Holder {
        static Platform INSTANCE;

        private Holder() {}
    }

    static Platform get() {
        return Holder.INSTANCE;
    }

    static void set(Platform platform) {
        Holder.INSTANCE = platform;
    }
}
