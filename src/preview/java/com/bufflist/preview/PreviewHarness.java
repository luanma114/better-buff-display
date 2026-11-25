package com.bufflist.preview;

import com.bufflist.BuffListConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 仅开发预览用：自动建档、上 buff、截图后退出。不进正式 jar。 */
@Mod.EventBusSubscriber(modid = "bufflist", value = Dist.CLIENT)
public final class PreviewHarness {
    private static final boolean ON = Boolean.getBoolean("bufflist.preview");
    private static boolean worldStarted;
    private static int ticks = -1;

    private static void startWorld(Minecraft mc) {
        worldStarted = true;
        mc.options.pauseOnLostFocus = false;
        mc.options.onboardAccessibility = false;
        String name = "preview" + System.currentTimeMillis();
        LevelSettings ls = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                WorldPresets::createNormalWorldDimensions);
    }

    private static void cmd(Minecraft mc, String c) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(server.getPlayerList().getPlayers().get(0))
                        .withPermission(4).withSuppressedOutput(), c));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!worldStarted && mc.getOverlay() == null && mc.level == null
                && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            startWorld(mc);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (ticks == 60) {
            String[] fx = {
                    "speed 99999 1", "jump_boost 600 0", "regeneration 40 2", "strength infinite 9",
                    "resistance 3600 3", "fire_resistance 1 0", "night_vision 179 0", "water_breathing 900 0",
                    "haste 1200 1", "health_boost infinite 4", "luck 300 0", "slowness 5 1",
                    "poison 30 0", "absorption 600 1", "mining_fatigue 60 1", "weakness 30 0", "hunger 20 2", "glowing 120 0",
            };
            for (String f : fx) cmd(mc, "effect give @s minecraft:" + f);
            cmd(mc, "give @s minecraft:diamond_sword");
            cmd(mc, "give @s minecraft:golden_apple 16");
            cmd(mc, "time set noon");
        }
        if (ticks == 100) mc.setScreen(new InventoryScreen(mc.player));
        if (ticks == 140) shot(mc, "preview_inventory.png");
        String[] hov = {"regeneration", "regeneration", "haste", "poison", "health_boost", "absorption", "resistance"};
        for (int k = 0; k < hov.length; k++) {
            int t = 150 + k * 12;
            if (ticks == t) {
                BuffListConfig.DESC_MODE.set(k == 0 ? BuffListConfig.DescMode.SHIFT : BuffListConfig.DescMode.ALWAYS);
                hover(mc, hov[k]);
            }
            if (ticks == t + 8) shot(mc, "tip_" + k + "_" + hov[k] + ".png");
        }
        int d = 150 + hov.length * 12;
        int[] btn = com.bufflist.EffectListRenderer.LAST_BUTTONS;
        if (ticks == d) {
            BuffListConfig.DESC_MODE.set(BuffListConfig.DescMode.SHIFT);
            BuffListConfig.setCustomPos(-1, -1);
            if (!(mc.screen instanceof InventoryScreen)) mc.setScreen(new InventoryScreen(mc.player));
            moveMouse(mc, btn[0] + 4, btn[1] + 4);
        }
        if (ticks == d + 8) shot(mc, "btn_hover_gear.png");
        if (ticks == d + 10) {
            moveMouse(mc, btn[2] + 4, btn[3] + 4);
            if (mc.screen != null) net.minecraftforge.client.ForgeHooksClient.onScreenMouseClickedPre(mc.screen, btn[2] + 4, btn[3] + 4, 0);
        }
        if (ticks == d + 14) if (mc.screen != null) net.minecraftforge.client.ForgeHooksClient.onScreenMouseDragPre(mc.screen, 120, 60, 0, 0, 0);
        if (ticks == d + 18) if (mc.screen != null) net.minecraftforge.client.ForgeHooksClient.onScreenMouseDragPre(mc.screen, 60, 30, 0, 0, 0);
        if (ticks == d + 22) shot(mc, "drag_during.png");
        if (ticks == d + 24) if (mc.screen != null) net.minecraftforge.client.ForgeHooksClient.onScreenMouseReleasedPre(mc.screen, 60, 30, 0);
        if (ticks == d + 26) moveMouse(mc, 5, 175);
        if (ticks == d + 32) mc.setScreen(null);
        if (ticks == d + 36) mc.setScreen(new InventoryScreen(mc.player));
        if (ticks == d + 46) shot(mc, "drag_reopen.png");
        if (ticks == d + 48) if (mc.screen != null) net.minecraftforge.client.ForgeHooksClient.onScreenMouseClickedPre(mc.screen, btn[0] + 4, btn[1] + 4, 0);
        if (ticks == d + 58) shot(mc, "settings_screen.png");
        int b = d + 64;
        if (ticks == b) {
            BuffListConfig.setCustomPos(-1, -1);
            mc.setScreen(null);
            BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.LIST);
        }
        if (ticks == b + 40) shot(mc, "preview_hud_list.png");
        // 列表关掉后：按钮标签仍在（原版图标之上），点齿轮还能进设置
        if (ticks == b + 50) {
            BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.VANILLA);
            BuffListConfig.INVENTORY_ENABLED.set(false);
            mc.setScreen(new InventoryScreen(mc.player));
        }
        if (ticks == b + 60) shot(mc, "tab_list_off.png");
        if (ticks == b + 62) if (mc.screen != null) net.minecraftforge.client.ForgeHooksClient.onScreenMouseClickedPre(mc.screen, btn[0] + 4, btn[1] + 4, 0);
        if (ticks == b + 70) {
            shot(mc, "settings_from_list_off.png");
            BuffListConfig.INVENTORY_ENABLED.set(true);
            mc.setScreen(null);
            cmd(mc, "effect clear @s");
        }
        // 身上没有效果：同样只显示按钮标签
        if (ticks == b + 76) mc.setScreen(new InventoryScreen(mc.player));
        if (ticks == b + 86) shot(mc, "tab_no_effects.png");
        // 常驻显示：设置界面新布局 → 调位置界面拖动 → 回到游戏看新位置
        if (ticks == b + 88) {
            for (String f : new String[]{"speed 600 1", "strength 600 0", "regeneration 600 2", "weakness 300 0", "haste 900 1"})
                cmd(mc, "effect give @s minecraft:" + f);
            BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.LIST);
            BuffListConfig.setHudPos(-1, -1);
            mc.setScreen(new com.bufflist.BuffListSettingsScreen(null));
        }
        if (ticks == b + 98) shot(mc, "settings_v2.png");
        if (ticks == b + 100) mc.setScreen(new com.bufflist.HudPositionScreen(null));
        if (ticks == b + 106) {
            int sw = mc.getWindow().getGuiScaledWidth();
            if (mc.screen == null) return;
            mc.screen.mouseClicked(sw - 12, 6, 0);
            mc.screen.mouseDragged(sw - 60, 40, 0, 0, 0);
            mc.screen.mouseDragged(40, 70, 0, 0, 0);
            moveMouse(mc, 40, 70);
        }
        if (ticks == b + 112) shot(mc, "hudpos_drag.png");
        if (ticks == b + 114 && mc.screen != null) { mc.screen.mouseReleased(40, 70, 0); mc.screen.onClose(); }
        if (ticks == b + 124) shot(mc, "hud_moved.png");
        if (ticks == b + 130) {
            BuffListConfig.setHudPos(-1, -1);
            BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.VANILLA);
            BuffListConfig.save();
            mc.stop();
        }
    }

    /** 把鼠标移到列表里某个效果那一行（复现渲染器的布局：右侧、行高 11、内边距 3）。 */
    private static void hover(Minecraft mc, String path) {
        if (!(mc.screen instanceof InventoryScreen inv)) return;
        java.util.List<net.minecraft.world.effect.MobEffectInstance> list = new java.util.ArrayList<>();
        for (var i : mc.player.getActiveEffects())
            if (net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(i).isVisibleInInventory(i)) list.add(i);
        list.sort(null);
        int idx = -1;
        for (int i = 0; i < list.size(); i++)
            if (net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(list.get(i).getEffect()).getPath().equals(path)) idx = i;
        if (idx < 0) return;
        int x = inv.getGuiLeft() + inv.getXSize() + 2 + 3 + 30;
        int y = inv.getGuiTop() + 3 + idx * 11 + 5;
        double sc = mc.getWindow().getGuiScale();
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                var fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, (f.equals("xpos") ? x : y) * sc);
            }
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private static void moveMouse(Minecraft mc, double x, double y) {
        double sc = mc.getWindow().getGuiScale();
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                var fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, (f.equals("xpos") ? x : y) * sc);
            }
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private static void shot(Minecraft mc, String file) {
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> {});
    }
}
