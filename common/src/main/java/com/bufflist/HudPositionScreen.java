package com.bufflist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 调整常驻列表位置：按住列表拖到任意位置，完成/Esc 时保存。 */
public class HudPositionScreen extends Screen {
    @Nullable
    private final Screen parent;
    private EffectListRenderer.Layout layout;
    private int boxX, boxY, boxW, boxH;
    private boolean dragging, moved, reset;
    private double offX, offY;

    public HudPositionScreen(@Nullable Screen parent) {
        super(Component.translatable("bufflist.settings.hud_position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<MobEffectInstance> effects = EffectListRenderer.hudEffects(minecraft);
        if (effects.isEmpty()) { // 身上没效果时用几条示例，方便看大小
            effects = List.of(
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1),
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0),
                    new MobEffectInstance(MobEffects.REGENERATION, 600, 2),
                    new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
        }
        layout = EffectListRenderer.hudLayout(minecraft, effects, width, height);
        boxW = EffectListRenderer.layoutWidth(layout);
        boxH = EffectListRenderer.layoutHeight(layout);
        if (!moved) {
            int[] p = reset ? new int[]{width - 2 - boxW, 2} : EffectListRenderer.hudPos(layout, width, height);
            boxX = p[0];
            boxY = p[1];
        }
        clampBox();

        int y = height / 2 + 20;
        addRenderableWidget(Button.builder(Component.translatable("bufflist.settings.reset_position"), b -> {
            reset = true;
            moved = false;
            boxX = width - 2 - boxW;
            boxY = 2;
        }).bounds(width / 2 - 102, y, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 2, y, 100, 20).build());
    }

    private void clampBox() {
        boxX = Math.max(0, Math.min(boxX, width - boxW));
        boxY = Math.max(0, Math.min(boxY, height - boxH));
    }

    private boolean overBox(double mx, double my) {
        return mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x40000000);
        EffectListRenderer.drawPanel(g, minecraft, layout, boxX, boxY);
        if (dragging || overBox(mouseX, mouseY)) {
            int c = 0xFFFFFFFF;
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY, c);
            g.fill(boxX - 1, boxY + boxH, boxX + boxW + 1, boxY + boxH + 1, c);
            g.fill(boxX - 1, boxY, boxX, boxY + boxH, c);
            g.fill(boxX + boxW, boxY, boxX + boxW + 1, boxY + boxH, c);
            g.pose().popPose();
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && overBox(mx, my)) {
            dragging = true;
            offX = mx - boxX;
            offY = my - boxY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            boxX = (int) Math.round(mx - offX);
            boxY = (int) Math.round(my - offY);
            clampBox();
            moved = true;
            reset = false;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        if (reset) {
            BuffListConfig.setHudPos(-1, -1);
        } else if (moved) {
            double fx = width > boxW ? (double) boxX / (width - boxW) : 0;
            double fy = height > boxH ? (double) boxY / (height - boxH) : 0;
            BuffListConfig.setHudPos(Math.max(0, Math.min(1, fx)), Math.max(0, Math.min(1, fy)));
        }
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
