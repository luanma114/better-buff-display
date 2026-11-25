package com.bufflist;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 背包列表、常驻列表的绘制和交互。不直接依赖加载器：各平台在对应的事件 / mixin 里调用这里的方法。
 */
public final class EffectListRenderer {
    public static final EffectListRenderer INSTANCE = new EffectListRenderer();

    private EffectListRenderer() {}
    private static final int PAD = 3;
    private static final int COL_GAP = 6;
    private static final int TEXT_GAP = 3;
    private static final int TIME_GAP = 6;

    private static final int COLOR_GOOD = 0xFF55FF55;
    private static final int COLOR_BAD = 0xFFFF5555;
    private static final int COLOR_NEUTRAL = 0xFF7FB8FF;
    private static final int COLOR_TIME = 0xFFD0D0D0;
    private static final int COLOR_TIME_LOW = 0xFFFFFF55;
    private static final int BORDER = 0x6E5A8C;
    private static final int BG = 0x100818;

    private Boolean recipeViewerLoaded;

    /** 最近一次绘制时每个效果所在行的位置 {x, y, w, h}（GUI 坐标），供自动化测试定位悬停。 */
    public static final java.util.Map<MobEffect, int[]> LAST_ROWS = new java.util.IdentityHashMap<>();
    /** 最近一次绘制的按钮左上角 {设置x, 设置y, 拖动x, 拖动y}，供自动化测试。 */
    public static final int[] LAST_BUTTONS = new int[4];

    // ---------------- 背包 ----------------

    /** 是否取消原版（及其它模组未接管的）背包效果绘制。 */
    public static boolean replacesInventoryEffects() {
        return BuffListConfig.INVENTORY_ENABLED.get();
    }

    // 面板右上角的小标签：两个按钮（设置、拖动）
    private static final int BTN = 9;
    private static final int BTN_GAP = 1;
    private static final int TAB_PAD = 1;
    private static final int TAB_W = TAB_PAD * 2 + BTN * 2 + BTN_GAP + 2; // 含左右边框
    private static final int TAB_H = TAB_PAD + BTN + 1;                   // 含上边框，底边与面板上边框相接

    private static final String[] ICON_GEAR = {
            "..#.#..",
            ".#####.",
            "##...##",
            ".#...#.",
            "##...##",
            ".#####.",
            "..#.#..",
    };
    private static final String[] ICON_MOVE = {
            "...#...",
            "..###..",
            ".#.#.#.",
            "#######",
            ".#.#.#.",
            "..###..",
            "...#...",
    };

    /** 上一帧面板（含标签）的外框与两个按钮位置，供鼠标事件判断。 */
    private int boxX, boxY, boxW, boxH;
    private int gearX, gearY, moveX, moveY;
    private boolean boxVisible;
    /** 拖动中：鼠标相对外框左上角的偏移。 */
    private boolean dragging;
    private double dragOffX, dragOffY, dragMouseX, dragMouseY;

    /** 背包界面画完之后调用。 */
    public void renderScreen(Screen s, GuiGraphics g, int mx, int my) {
        boxVisible = false;
        if (!(s instanceof EffectRenderingInventoryScreen<?> screen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (dragging) { // Fabric 没有拖动事件，靠每帧的鼠标位置跟随
            dragMouseX = mx;
            dragMouseY = my;
        }
        Platform pf = Platform.get();

        // 列表关掉或身上没有效果时，仍然单独显示按钮标签，保证随时能进设置
        List<MobEffectInstance> effects = BuffListConfig.INVENTORY_ENABLED.get()
                ? collect(mc, pf::visibleInInventory)
                : List.of();

        int x, y;
        Layout layout;
        if (dragging || BuffListConfig.hasCustomPos()) {
            layout = effects.isEmpty() ? Layout.empty() : layout(mc.font, effects, screen.width / 2, screen.height - 8 - TAB_H);
            int w = Math.max(layout.width, TAB_W), h = layout.height + TAB_H;
            int bx, by;
            if (dragging) {
                bx = (int) Math.round(dragMouseX - dragOffX);
                by = (int) Math.round(dragMouseY - dragOffY);
            } else {
                // 位置按比例存：0 贴左/顶，1 贴右/底；面板变宽变高时从靠近的那条边往里长
                bx = (int) Math.round(BuffListConfig.INVENTORY_X.get() * (screen.width - w));
                by = (int) Math.round(BuffListConfig.INVENTORY_Y.get() * (screen.height - h));
            }
            bx = Math.max(0, Math.min(bx, screen.width - w));
            by = Math.max(0, Math.min(by, screen.height - h));
            x = bx + w - layout.width;
            y = by + TAB_H;
        } else {
            int guiLeft = pf.guiLeft(screen);
            int guiRight = guiLeft + pf.imageWidth(screen);
            int leftSpace = guiLeft - 4;
            int rightSpace = screen.width - guiRight - 4;

            boolean left = chooseLeft(screen, leftSpace, rightSpace);
            int avail = left ? leftSpace : rightSpace;
            int minW = Math.max(BuffListConfig.ICON_SIZE.get() + PAD * 2, TAB_W);
            if (avail < minW) { // 这边放不下就换另一边
                left = !left;
                avail = left ? leftSpace : rightSpace;
                if (avail < minW) return;
            }

            layout = effects.isEmpty() ? Layout.empty() : layout(mc.font, effects, avail, screen.height - 8 - TAB_H);
            x = left ? guiLeft - 2 - Math.max(layout.width, TAB_W) : guiRight + 2;
            y = Math.max(4 + TAB_H, Math.min(pf.guiTop(screen), screen.height - 4 - layout.height));
        }

        int right = x + Math.max(layout.width, effects.isEmpty() ? TAB_W : 0);
        boxX = Math.min(x, right - TAB_W);
        boxY = y - TAB_H;
        boxW = right - boxX;
        boxH = layout.height + TAB_H;
        moveX = right - 1 - TAB_PAD - BTN;
        gearX = moveX - BTN_GAP - BTN;
        gearY = moveY = boxY + 1 + TAB_PAD;
        boxVisible = true;
        LAST_BUTTONS[0] = gearX; LAST_BUTTONS[1] = gearY; LAST_BUTTONS[2] = moveX; LAST_BUTTONS[3] = moveY;

        if (!effects.isEmpty()) draw(g, mc, layout, x, y, dragging ? -1 : mx, dragging ? -1 : my, screen);
        drawTab(g, right - TAB_W, boxY, mx, my, effects.isEmpty());
    }

    private void drawTab(GuiGraphics g, int tx, int ty, int mx, int my, boolean alone) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);
        int alpha = Math.max(170, BuffListConfig.BACKGROUND_ALPHA.get());
        int b = Math.min(255, alpha + 40) << 24 | BORDER;
        g.fill(tx, ty, tx + TAB_W, ty + TAB_H, alpha << 24 | BG);
        g.fill(tx, ty, tx + TAB_W, ty + 1, b);
        g.fill(tx, ty, tx + 1, ty + TAB_H, b);
        g.fill(tx + TAB_W - 1, ty, tx + TAB_W, ty + TAB_H, b);
        if (alone) g.fill(tx, ty + TAB_H, tx + TAB_W, ty + TAB_H + 1, b); // 没有面板时自己补上底边
        drawButton(g, gearX, gearY, ICON_GEAR, inside(mx, my, gearX, gearY), false);
        drawButton(g, moveX, moveY, ICON_MOVE, inside(mx, my, moveX, moveY), dragging);
        pose.popPose();
    }

    private static void drawButton(GuiGraphics g, int bx, int by, String[] icon, boolean hover, boolean active) {
        if (hover || active) g.fill(bx, by, bx + BTN, by + BTN, active ? 0x806E5A8C : 0x50FFFFFF);
        int color = hover || active ? 0xFFFFFFFF : 0xFFB4A6D2;
        for (int r = 0; r < icon.length; r++) {
            String row = icon[r];
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) == '#') g.fill(bx + 1 + c, by + 1 + r, bx + 2 + c, by + 2 + r, color);
            }
        }
    }

    private static boolean inside(double mx, double my, int bx, int by) {
        return mx >= bx && mx < bx + BTN && my >= by && my < by + BTN;
    }

    /** 鼠标按下；返回 true 表示点到了我们的按钮，界面不要再处理。 */
    public boolean mousePressed(Screen s, double mx, double my, int button) {
        if (!boxVisible || button != 0) return false;
        if (!(s instanceof EffectRenderingInventoryScreen<?> screen)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (inside(mx, my, gearX, gearY)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            mc.setScreen(new BuffListSettingsScreen(screen));
            return true;
        } else if (inside(mx, my, moveX, moveY)) {
            dragging = true;
            dragOffX = mx - boxX;
            dragOffY = my - boxY;
            dragMouseX = mx;
            dragMouseY = my;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my) {
        if (!dragging) return false;
        dragMouseX = mx;
        dragMouseY = my;
        return true;
    }

    public boolean mouseReleased(Screen screen) {
        if (!dragging) return false;
        dragging = false;
        // 按上一帧画出来的外框位置换算成比例
        double fx = screen.width > boxW ? (double) boxX / (screen.width - boxW) : 0;
        double fy = screen.height > boxH ? (double) boxY / (screen.height - boxH) : 0;
        BuffListConfig.setCustomPos(Math.max(0, Math.min(1, fx)), Math.max(0, Math.min(1, fy)));
        return true;
    }

    public void screenClosing() {
        dragging = false;
        boxVisible = false;
    }

    private boolean chooseLeft(EffectRenderingInventoryScreen<?> screen, int leftSpace, int rightSpace) {
        switch (BuffListConfig.INVENTORY_SIDE.get()) {
            case LEFT: return true;
            case RIGHT: return false;
            default:
                if (screen instanceof InventoryScreen inv && inv.getRecipeBookComponent().isVisible()) return false;
                if (recipeViewerLoaded == null) {
                    Platform pf = Platform.get();
                    recipeViewerLoaded = pf.isModLoaded("jei") || pf.isModLoaded("roughlyenoughitems") || pf.isModLoaded("emi");
                }
                return recipeViewerLoaded && leftSpace >= rightSpace / 2;
        }
    }

    // ---------------- HUD ----------------

    /**
     * 原版右上角只画 showIcon=true 的效果；装备、饰品、套装给的效果大多隐藏了粒子（连带 showIcon=false），
     * 于是背包里有、右上角却没有。HUD 绘制前临时把这些效果的 showIcon 打开，画完立刻还原，
     * 这样原版以及接管/改写图标的模组（物品栏HUD+、加倒计时之类）都能照常画出来。
     */
    private final List<MobEffectInstance> forcedIcons = new ArrayList<>();

    private void restoreIcons() {
        for (MobEffectInstance inst : forcedIcons) Platform.get().setShowIcon(inst, false);
        forcedIcons.clear();
    }

    private void forceIcons(Minecraft mc) {
        if (mc.player == null) return;
        Platform pf = Platform.get();
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (inst.showIcon() || !pf.visibleInGui(inst)) continue;
            pf.setShowIcon(inst, true);
            forcedIcons.add(inst);
        }
    }

    /** 整个 HUD 绘制之前调用：临时打开隐藏效果的图标（原版右上角、物品栏HUD+ 之类接管图标的模组都在这段里画）。 */
    public void guiPre() {
        restoreIcons();
        if (BuffListConfig.hudOn() && BuffListConfig.HUD_SHOW_HIDDEN.get()) forceIcons(Minecraft.getInstance());
    }

    /** 整个 HUD 画完后调用，还原。 */
    public void guiPost() {
        restoreIcons();
    }

    public void clientTick() {
        restoreIcons(); // 兜底：HUD 被跳过（F1 等）也别把改动留到逻辑里
        BuffListConfig.pollFile();
        SmokeTest.tick();
    }

    /** 原版右上角效果图标要画之前调用。返回 true 表示原版不要画（关掉了，或者由我们画成列表）。 */
    public boolean renderHud(GuiGraphics g, int sw, int sh) {
        if (!BuffListConfig.hudOn()) return true;
        if (!BuffListConfig.hudList()) return false; // 图标样式：交给原版画（隐藏效果已在 guiPre 里打开）

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen instanceof EffectRenderingInventoryScreen<?>
                || mc.screen instanceof HudPositionScreen) return true; // 调位置界面自己画
        List<MobEffectInstance> effects = hudEffects(mc);
        if (effects.isEmpty()) return true;

        Layout layout = hudLayout(mc, effects, sw, sh);
        int[] p = hudPos(layout, sw, sh);
        draw(g, mc, layout, p[0], p[1], -1, -1, null);
        return true;
    }

    public static List<MobEffectInstance> hudEffects(Minecraft mc) {
        Platform pf = Platform.get();
        return collect(mc, inst -> (inst.showIcon() || BuffListConfig.HUD_SHOW_HIDDEN.get()) && pf.visibleInGui(inst));
    }

    static Layout hudLayout(Minecraft mc, List<MobEffectInstance> effects, int sw, int sh) {
        return layout(mc.font, effects, sw / 2, sh - 60); // 留出底部快捷栏
    }

    /** 常驻列表左上角：没拖过就贴右上角，拖过按比例换算（同背包列表）。 */
    static int[] hudPos(Layout l, int sw, int sh) {
        if (!BuffListConfig.hasHudPos()) return new int[]{sw - 2 - l.width, 2};
        int x = (int) Math.round(BuffListConfig.HUD_X.get() * (sw - l.width));
        int y = (int) Math.round(BuffListConfig.HUD_Y.get() * (sh - l.height));
        return new int[]{Math.max(0, Math.min(x, sw - l.width)), Math.max(0, Math.min(y, sh - l.height))};
    }

    static int layoutWidth(Layout l) { return l.width; }
    static int layoutHeight(Layout l) { return l.height; }

    static void drawPanel(GuiGraphics g, Minecraft mc, Layout l, int x, int y) {
        draw(g, mc, l, x, y, -1, -1, null);
    }

    // ---------------- 公共 ----------------

    static List<MobEffectInstance> collect(Minecraft mc, Predicate<MobEffectInstance> filter) {
        List<MobEffectInstance> list = new ArrayList<>();
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (filter.test(inst)) list.add(inst);
        }
        list.sort(null); // 与原版一致的排序（MobEffectInstance 自带比较规则）
        return list;
    }

    private record Entry(MobEffectInstance inst, Component name, String time, int nameColor, int timeColor) {}

    static final class Layout {
        List<Entry> entries;
        int cols, rows, colW, rowH, icon, width, height;
        boolean showText, showTime;

        /** 没有列表时的占位：只留标签底边那 1 像素。 */
        static Layout empty() {
            Layout l = new Layout();
            l.height = 1;
            return l;
        }
    }

    private static Layout layout(Font font, List<MobEffectInstance> effects, int maxW, int maxH) {
        Layout l = new Layout();
        l.icon = BuffListConfig.ICON_SIZE.get();
        l.rowH = Math.max(l.icon, font.lineHeight) + 2;
        l.entries = new ArrayList<>(effects.size());

        int nameW = 0, timeW = 0;
        for (MobEffectInstance inst : effects) {
            Entry e = entry(inst);
            l.entries.add(e);
            nameW = Math.max(nameW, font.width(e.name));
            timeW = Math.max(timeW, font.width(e.time));
        }

        int n = l.entries.size();
        int maxRows = Math.max(1, (maxH - PAD * 2 + 2) / l.rowH);
        l.cols = (n + maxRows - 1) / maxRows;
        l.rows = (n + l.cols - 1) / l.cols;

        int natural = l.icon + TEXT_GAP + nameW + TIME_GAP + timeW;
        int fit = (maxW - PAD * 2 - (l.cols - 1) * COL_GAP) / l.cols;
        l.colW = Math.max(l.icon, Math.min(natural, fit));
        // 至少要能放下 图标 + 时间 + 两三个字，否则只显示图标
        int minText = l.icon + TEXT_GAP + font.width("一二") + TIME_GAP + timeW;
        int minTimeOnly = l.icon + TEXT_GAP + timeW;
        l.showText = l.colW >= minText;
        l.showTime = l.colW >= minTimeOnly;
        if (!l.showTime) l.colW = l.icon;

        l.width = PAD * 2 + l.cols * l.colW + (l.cols - 1) * COL_GAP;
        l.height = PAD * 2 + l.rows * l.rowH - 2;
        return l;
    }

    private static Entry entry(MobEffectInstance inst) {
        MobEffect effect = inst.getEffect().value();
        MutableComponent name = effect.getDisplayName().copy();
        int amp = inst.getAmplifier();
        if (amp > 0) name.append(" ").append(level(amp + 1));

        int nameColor = effect.getCategory() == MobEffectCategory.BENEFICIAL ? COLOR_GOOD
                : effect.getCategory() == MobEffectCategory.HARMFUL ? COLOR_BAD : COLOR_NEUTRAL;

        String time = MobEffectUtil.formatDuration(inst, 1.0F, tickRate()).getString();
        boolean low = !inst.isInfiniteDuration() && inst.getDuration() < 200;
        return new Entry(inst, name, time, nameColor, low ? COLOR_TIME_LOW : COLOR_TIME);
    }

    static float tickRate() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.tickRateManager().tickrate() : 20f;
    }

    private static Component level(int lvl) {
        String key = "enchantment.level." + lvl;
        if (I18n.exists(key)) return Component.translatable(key);
        return Component.literal(lvl <= 3999 ? roman(lvl) : Integer.toString(lvl));
    }

    private static String roman(int n) {
        int[] v = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] s = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) while (n >= v[i]) { n -= v[i]; sb.append(s[i]); }
        return sb.toString();
    }

    private static void draw(GuiGraphics g, Minecraft mc, Layout l, int x, int y, int mouseX, int mouseY,
                             @Nullable EffectRenderingInventoryScreen<?> screen) {
        Font font = mc.font;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, 200); // 盖在物品图标之上

        int alpha = BuffListConfig.BACKGROUND_ALPHA.get();
        if (alpha > 0) {
            int a = alpha << 24;
            g.fill(x, y, x + l.width, y + l.height, a | BG);
            int b = Math.min(255, alpha + 40) << 24 | BORDER;
            g.fill(x, y, x + l.width, y + 1, b);
            g.fill(x, y + l.height - 1, x + l.width, y + l.height, b);
            g.fill(x, y, x + 1, y + l.height, b);
            g.fill(x + l.width - 1, y, x + l.width, y + l.height, b);
        }

        Entry hovered = null;
        int textY = (l.rowH - 2 - font.lineHeight) / 2 + 1;
        int iconY = (l.rowH - 2 - l.icon) / 2;
        for (int i = 0; i < l.entries.size(); i++) {
            Entry e = l.entries.get(i);
            int col = i / l.rows, row = i % l.rows;
            int cx = x + PAD + col * (l.colW + COL_GAP);
            int cy = y + PAD + row * l.rowH;

            drawIcon(g, mc, e.inst, cx, cy + iconY, l.icon, screen);

            if (l.showTime) {
                int timeW = font.width(e.time);
                int timeX = cx + l.colW - timeW;
                if (l.showText) {
                    int nameX = cx + l.icon + TEXT_GAP;
                    int nameMax = timeX - TIME_GAP - nameX;
                    String name = e.name.getString();
                    if (font.width(name) > nameMax) {
                        name = font.plainSubstrByWidth(name, nameMax - font.width("…")) + "…";
                    }
                    g.drawString(font, name, nameX, cy + textY, e.nameColor, true);
                }
                g.drawString(font, e.time, timeX, cy + textY, e.timeColor, true);
            }

            LAST_ROWS.put(e.inst.getEffect().value(), new int[]{cx, cy, l.colW, l.rowH});
            if (mouseX >= cx && mouseX < cx + l.colW && mouseY >= cy && mouseY < cy + l.rowH) hovered = e;
        }
        pose.popPose();

        if (hovered != null && BuffListConfig.SHOW_TOOLTIP.get()) {
            g.renderTooltip(font, tooltip(font, hovered, screen), mouseX, mouseY);
        }
    }

    private static final int TOOLTIP_WRAP = 260;

    private static List<FormattedCharSequence> tooltip(Font font, Entry e, @Nullable EffectRenderingInventoryScreen<?> screen) {
        List<FormattedCharSequence> out = new ArrayList<>();
        out.add(e.name.copy().withStyle(s -> s.withColor(e.nameColor & 0xFFFFFF)).getVisualOrderText());
        out.add(Component.literal(e.time).withStyle(ChatFormatting.GRAY).getVisualOrderText());

        BuffListConfig.DescMode mode = BuffListConfig.DESC_MODE.get();
        if (mode == BuffListConfig.DescMode.OFF) return out;
        List<Component> desc = EffectDescriptions.lines(e.inst, screen);
        if (desc.isEmpty()) return out;
        if (mode == BuffListConfig.DescMode.SHIFT && !Screen.hasShiftDown()) {
            out.add(Component.translatable("bufflist.tooltip.hold_shift").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            return out;
        }
        for (Component c : desc) out.addAll(font.split(c, TOOLTIP_WRAP));
        return out;
    }

    /**
     * 画 18×18 图标（缩放到 size）。
     * 优先用效果在图集里的标准贴图：各模组自定义绘制时对坐标的约定并不统一
     * （例如拉美西斯二世的诅咒把图标直接画在传入坐标上，而 Forge 约定背包图标在 y+7、HUD 图标在 +3），
     * 交给它们画容易错位。只有标准贴图缺失时才调用模组自己的绘制，并裁剪到图标格内。
     */
    private static void drawIcon(GuiGraphics g, Minecraft mc, MobEffectInstance inst, int x, int y, int size,
                                 @Nullable EffectRenderingInventoryScreen<?> screen) {
        TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
        boolean hasSprite = !MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        float scale = size / 18f;
        pose.scale(scale, scale, 1f);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (hasSprite) {
            g.blit(0, 0, 0, 18, 18, sprite);
        } else {
            boolean custom;
            try {
                custom = Platform.get().renderCustomIcon(g, inst, screen, x, y, size);
            } catch (Throwable ignored) {
                custom = false;
            }
            if (!custom) g.blit(0, 0, 0, 18, 18, sprite);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.popPose();
    }
}
