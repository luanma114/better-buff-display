package com.bufflist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 设置界面：背包面板右上角齿轮按钮、模组列表的「配置」按钮都打开这里。改动立即生效并写入配置文件。 */
public class BuffListSettingsScreen extends Screen {
    private enum Pos { AUTO, LEFT, RIGHT, CUSTOM }

    @Nullable
    private final Screen parent;

    public BuffListSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("bufflist.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 150, h = 20, gap = 4;
        int lx = width / 2 - w - gap / 2, rx = width / 2 + gap / 2;
        int y = Math.max(28, height / 2 - 80);

        addRenderableWidget(CycleButton.onOffBuilder(BuffListConfig.INVENTORY_ENABLED.get())
                .create(lx, y, w, h, Component.translatable("bufflist.settings.inventory"),
                        (b, v) -> { BuffListConfig.INVENTORY_ENABLED.set(v); BuffListConfig.save(); }));

        List<Pos> posValues = new ArrayList<>(List.of(Pos.AUTO, Pos.LEFT, Pos.RIGHT));
        Pos current;
        if (BuffListConfig.hasCustomPos()) {
            posValues.add(Pos.CUSTOM);
            current = Pos.CUSTOM;
        } else {
            current = Pos.valueOf(BuffListConfig.INVENTORY_SIDE.get().name());
        }
        addRenderableWidget(CycleButton.<Pos>builder(p -> option("position", p))
                .withValues(posValues).withInitialValue(current)
                .create(rx, y, w, h, Component.translatable("bufflist.settings.position"), (b, v) -> {
                    if (v == Pos.CUSTOM) return;
                    BuffListConfig.INVENTORY_SIDE.set(BuffListConfig.Side.valueOf(v.name()));
                    BuffListConfig.INVENTORY_X.set(-1.0);
                    BuffListConfig.INVENTORY_Y.set(-1.0);
                    BuffListConfig.save();
                }));

        // 常驻显示：开关 + 样式；样式和位置在关闭时变灰，图标样式（原版）不能移动
        y += h + gap;
        if (BuffListConfig.HUD_MODE.get() == BuffListConfig.HudMode.HIDDEN) { // 旧配置迁移
            BuffListConfig.HUD_ENABLED.set(false);
            BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.VANILLA);
            BuffListConfig.save();
        }
        CycleButton<BuffListConfig.HudMode> hudStyle = CycleButton.<BuffListConfig.HudMode>builder(m -> option("hud", m))
                .withValues(BuffListConfig.HudMode.VANILLA, BuffListConfig.HudMode.LIST)
                .withInitialValue(BuffListConfig.HUD_MODE.get())
                .create(rx, y, w, h, Component.translatable("bufflist.settings.hud_style"), (b, v) -> {
                    BuffListConfig.HUD_MODE.set(v);
                    BuffListConfig.save();
                    refreshHud();
                });
        hudPosButton = Button.builder(Component.translatable("bufflist.settings.hud_position"),
                b -> minecraft.setScreen(new HudPositionScreen(this))).bounds(lx, y + h + gap, w, h).build();
        hudStyleButton = hudStyle;
        addRenderableWidget(CycleButton.onOffBuilder(BuffListConfig.HUD_ENABLED.get())
                .create(lx, y, w, h, Component.translatable("bufflist.settings.hud"), (b, v) -> {
                    BuffListConfig.HUD_ENABLED.set(v);
                    BuffListConfig.save();
                    refreshHud();
                }));
        addRenderableWidget(hudStyle);
        addRenderableWidget(hudPosButton);
        refreshHud();

        y += h + gap;
        // 这一行左边是「调整常驻显示位置」（上面已经加了），右边放「隐藏的效果」
        hudHiddenButton = CycleButton.onOffBuilder(BuffListConfig.HUD_SHOW_HIDDEN.get())
                .create(rx, y, w, h, Component.translatable("bufflist.settings.hud_hidden"),
                        (b, v) -> { BuffListConfig.HUD_SHOW_HIDDEN.set(v); BuffListConfig.save(); });
        addRenderableWidget(hudHiddenButton);
        refreshHud();

        y += h + gap;
        addRenderableWidget(CycleButton.<BuffListConfig.DescMode>builder(m -> option("desc", m))
                .withValues(BuffListConfig.DescMode.values()).withInitialValue(BuffListConfig.DESC_MODE.get())
                .create(lx, y, w, h, Component.translatable("bufflist.settings.desc"),
                        (b, v) -> { BuffListConfig.DESC_MODE.set(v); BuffListConfig.save(); }));
        addRenderableWidget(CycleButton.onOffBuilder(BuffListConfig.SHOW_TOOLTIP.get())
                .create(rx, y, w, h, Component.translatable("bufflist.settings.tooltip"),
                        (b, v) -> { BuffListConfig.SHOW_TOOLTIP.set(v); BuffListConfig.save(); }));

        y += h + gap;
        addRenderableWidget(new IntSlider(lx, y, w, h, "bufflist.settings.icon_size", "", 8, 18,
                BuffListConfig.ICON_SIZE.get(), BuffListConfig.ICON_SIZE::set));
        addRenderableWidget(new IntSlider(rx, y, w, h, "bufflist.settings.background", "%", 0, 100,
                (int) Math.round(BuffListConfig.BACKGROUND_ALPHA.get() * 100 / 255.0),
                v -> BuffListConfig.BACKGROUND_ALPHA.set((int) Math.round(v * 255 / 100.0))));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, Math.min(height - 24, y + h + gap * 3), 200, h).build());
    }

    /** 整数滑块（原版 AbstractSliderButton，三个平台通用）。松开时保存配置。 */
    private static final class IntSlider extends AbstractSliderButton {
        private final String key, suffix;
        private final int min, max;
        private final java.util.function.IntConsumer apply;

        IntSlider(int x, int y, int w, int h, String key, String suffix, int min, int max, int value,
                  java.util.function.IntConsumer apply) {
            super(x, y, w, h, Component.empty(), (double) (value - min) / (max - min));
            this.key = key;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.apply = apply;
            updateMessage();
        }

        private int intValue() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key).append(intValue() + suffix));
        }

        @Override
        protected void applyValue() {
            apply.accept(intValue());
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            BuffListConfig.save();
        }
    }

    private Button hudPosButton;
    private CycleButton<BuffListConfig.HudMode> hudStyleButton;
    private CycleButton<Boolean> hudHiddenButton;

    private void refreshHud() {
        boolean on = BuffListConfig.HUD_ENABLED.get();
        hudStyleButton.active = on;
        hudPosButton.active = on && BuffListConfig.hudList();
        if (hudHiddenButton != null) hudHiddenButton.active = on;
    }

    private static Component option(String group, Enum<?> v) {
        return Component.translatable("bufflist.settings." + group + "." + v.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, Math.max(12, height / 2 - 92), 0xFFFFFF);
    }

    @Override
    public void onClose() {
        BuffListConfig.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
