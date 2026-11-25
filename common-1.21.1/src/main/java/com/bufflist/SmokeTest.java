package com.bufflist;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;

/**
 * 开发自测：启动参数 -Dbufflist.smoketest=true 时自动建档、上效果、截图、打印说明后退出。
 * 只用原版代码，四个平台共用。平时不运行。
 */
final class SmokeTest {
    private static final Logger LOG = LogUtils.getLogger();
    static final boolean ON = Boolean.getBoolean("bufflist.smoketest");
    private static boolean started;
    private static int ticks = -1;

    private SmokeTest() {}

    static void tick() {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started && mc.getOverlay() == null && mc.level == null
                && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            started = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.onboardAccessibility = false;
            String name = "smoketest" + System.currentTimeMillis();
            LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false),
                    WorldPresets::createNormalWorldDimensions, mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        switch (ticks) {
            case 40 -> {
                BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.VANILLA);
                BuffListConfig.INVENTORY_ENABLED.set(true);
                for (String f : new String[]{"speed 600 1", "regeneration 600 2", "strength 1200 0", "poison 300 0",
                        "haste 900 1", "night_vision 1200 0"}) cmd(mc, "effect give @s minecraft:" + f);
                cmd(mc, "effect give @s minecraft:resistance 600 3 true"); // 隐藏粒子 → 原版右上角不显示图标
                cmd(mc, "time set noon");
            }
            case 80 -> shot(mc, "smoke_hud_icons.png");
            case 90 -> mc.setScreen(new InventoryScreen(mc.player));
            case 120 -> shot(mc, "smoke_inventory.png");
            case 125 -> {
                BuffListDebug.dump();
                for (MobEffectInstance inst : mc.player.getActiveEffects()) {
                    StringBuilder sb = new StringBuilder("[SmokeTest] desc ").append(Platform.get().effectId(effect(inst))).append(':');
                    for (Component c : EffectDescriptions.lines(inst, null)) sb.append(" | ").append(c.getString());
                    LOG.info(sb.toString());
                }
            }
            case 130 -> {
                mc.setScreen(null);
                BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.LIST);
            }
            case 160 -> shot(mc, "smoke_hud_list.png");
            case 170 -> mc.setScreen(new BuffListSettingsScreen(null));
            case 190 -> shot(mc, "smoke_settings.png");
            case 200 -> {
                mc.setScreen(null);
                BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.VANILLA);
                BuffListConfig.save();
                LOG.info("[SmokeTest] DONE");
                mc.stop();
            }
            default -> {}
        }
    }

    private static net.minecraft.world.effect.MobEffect effect(MobEffectInstance inst) {
        return inst.getEffect().value();
    }

    private static void cmd(Minecraft mc, String c) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(server.getPlayerList().getPlayers().get(0))
                        .withPermission(4).withSuppressedOutput(), c));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), c -> LOG.info("[SmokeTest] shot {}", name));
    }
}
