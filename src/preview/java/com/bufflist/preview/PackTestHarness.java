package com.bufflist.preview;

import com.bufflist.BuffListConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 整合包实测用：-Dbufflist.packtest=true 时自动建档，把所有非瞬时的有益/中性效果分组加到玩家身上，
 * 每组截两张图（本模组列表 / 原版显示），并把每个效果的可见性写进日志。
 */
@Mod.EventBusSubscriber(modid = "bufflist", value = Dist.CLIENT)
public final class PackTestHarness {
    private static final boolean ON = Boolean.getBoolean("bufflist.packtest");
    private static final Logger LOG = LogUtils.getLogger();
    private static final int GROUP = 8;
    private static final int STEP = 80;

    private static boolean worldStarted;
    private static int idle, ticks = -1;
    private static List<List<ResourceLocation>> groups;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!worldStarted) {
            if (mc.getOverlay() == null && mc.level == null && mc.screen != null && ++idle > 100) {
                if (SURVEY) { survey(mc); return; }
                startWorld(mc);
            }
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (PALADIN) { paladin(mc); return; }
        if (COMPAT) { compat(mc); return; }
        if (DESC) { desc(mc); return; }
        if (ACCEPT) { accept(mc); return; }
        if (HUDCOUNT) { hudcount(mc); return; }
        if (ticks == 200) {
            groups = buildGroups();
            LOG.info("[PackTest] {} effects in {} groups", groups.stream().mapToInt(List::size).sum(), groups.size());
            cmd(mc, "gamerule doDaylightCycle false");
            cmd(mc, "time set noon");
            cmd(mc, "weather clear");
        }
        if (groups == null || ticks < 220) return;
        int g = (ticks - 220) / STEP, t = (ticks - 220) % STEP;
        if (g >= groups.size()) {
            if (t == 0) { LOG.info("[PackTest] DONE"); mc.stop(); }
            return;
        }
        List<ResourceLocation> grp = groups.get(g);
        if (t == 0) {
            mc.setScreen(null);
            cmd(mc, "effect clear @s");
            for (ResourceLocation id : grp) cmd(mc, "effect give @s " + id + " 600 0 true");
        }
        if (t == 30) {
            dump(mc, g, grp);
            BuffListConfig.INVENTORY_ENABLED.set(true);
            mc.setScreen(new InventoryScreen(mc.player));
        }
        if (t == 45) shot(mc, "packtest/g" + g + "_bufflist.png");
        if (t == 50) BuffListConfig.INVENTORY_ENABLED.set(false);
        if (t == 65) shot(mc, "packtest/g" + g + "_vanilla.png");
        if (t == 70) BuffListConfig.INVENTORY_ENABLED.set(true);
    }

    private static final boolean PALADIN = "paladin".equals(System.getProperty("bufflist.scenario"));
    private static final boolean COMPAT = "compat".equals(System.getProperty("bufflist.scenario"));
    private static final boolean SURVEY = "survey".equals(System.getProperty("bufflist.scenario"));
    private static final boolean DESC = "desc".equals(System.getProperty("bufflist.scenario"));
    private static final boolean HUDCOUNT = "hudcount".equals(System.getProperty("bufflist.scenario"));
    private static final boolean ACCEPT = "accept".equals(System.getProperty("bufflist.scenario"));

    /** 验收环境：建好档、挂上一批原版+模组效果、位置恢复默认，之后什么都不做，交给人手动操作。 */
    private static void accept(Minecraft mc) {
        if (ticks != 100) return;
        BuffListConfig.setCustomPos(-1, -1);
        BuffListConfig.INVENTORY_ENABLED.set(true);
        cmd(mc, "gamemode creative");
        cmd(mc, "gamerule doDaylightCycle false");
        cmd(mc, "time set noon");
        cmd(mc, "weather clear");
        cmd(mc, "effect clear @s");
        String[] fx = {
                "minecraft:speed 99999 1", "minecraft:haste 1200 1", "minecraft:strength infinite 2",
                "minecraft:resistance 3600 3", "minecraft:regeneration 600 2", "minecraft:night_vision 3600 0",
                "minecraft:jump_boost 900 0", "minecraft:luck 300 0", "minecraft:health_boost infinite 4",
                "minecraft:weakness 1800 0", "minecraft:slowness 120 0",
                "minecraft:water_breathing 900 0", "minecraft:fire_resistance 60 0", "minecraft:absorption 600 1",
                "minecraft:glowing 240 0", "minecraft:hunger 45 2", "minecraft:poison 30 0",
                // 以下为整合包模组效果，原版环境里不存在时命令会静默失败
                "terramity:lifesteal 1800 1", "goety:gold_touched 1800 0", "irons_spellbooks:evasion 1800 2",
                "alexsmobs:lava_vision 1800 0", "alexsmobs:knockback_resistance 1800 0",
                "traveloptics:vigor_siphon 1800 0", "born_in_chaos_v1:magic_depletion 1800 0",
        };
        for (String f : fx) cmd(mc, "effect give @s " + f); // 不隐藏粒子：隐藏粒子的效果原版不在画面右上角显示图标
        LOG.info("[PackTest] accept environment ready");
    }

    /** 复现「右上角图标比实际少」：一半效果隐藏粒子（装备/饰品常这样挂），记录每个效果的 showIcon 并截 HUD。 */
    private static void hudcount(Minecraft mc) {
        if (ticks == 200) {
            cmd(mc, "gamerule doDaylightCycle false");
            cmd(mc, "time set noon");
            cmd(mc, "effect clear @s");
            String[] fx = {"jump_boost 1000 4 true", "regeneration 1000 5", "health_boost 1000 1", "haste 1000 0 true",
                    "luck 1000 4", "resistance 1000 2 true", "night_vision 1000 0 true", "strength 1000 5 true", "speed 1000 1"};
            for (String f : fx) cmd(mc, "effect give @s minecraft:" + f);
        }
        if (ticks == 240) {
            for (MobEffectInstance inst : mc.player.getActiveEffects()) {
                IClientMobEffectExtensions ext = IClientMobEffectExtensions.of(inst);
                LOG.info("[PackTest] hud {} visible={} showIcon={} ambient={} gui={}", ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect()),
                        inst.isVisible(), inst.showIcon(), inst.isAmbient(), ext.isVisibleInGui(inst));
            }
            LOG.info("[PackTest] hud active={} listHud={}", mc.player.getActiveEffects().size(),
                    com.bufflist.EffectListRenderer.hudEffects(mc).size());
        }
        if (ticks >= 200 && ticks < 330 && mc.screen != null && !(mc.screen instanceof InventoryScreen)) mc.setScreen(null);
        if (ticks == 250) { BuffListConfig.HUD_SHOW_HIDDEN.set(true); BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.VANILLA); }
        if (ticks == 260) shot(mc, "packtest/hudcount_icons_on.png");
        if (ticks == 265) BuffListConfig.HUD_SHOW_HIDDEN.set(false);
        if (ticks == 275) shot(mc, "packtest/hudcount_icons_off.png");
        if (ticks == 280) { BuffListConfig.HUD_SHOW_HIDDEN.set(true); BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.LIST); }
        if (ticks == 290) shot(mc, "packtest/hudcount_list_on.png");
        if (ticks == 295) BuffListConfig.HUD_MODE.set(BuffListConfig.HudMode.VANILLA);
        if (ticks == 300) {
            int n = 0;
            for (MobEffectInstance inst : mc.player.getActiveEffects()) if (!inst.showIcon()) n++;
            LOG.info("[PackTest] hud after-restore hiddenCount={} (应为 5)", n);
        }
        if (ticks == 305) mc.setScreen(new InventoryScreen(mc.player));
        if (ticks == 325) shot(mc, "packtest/hudcount_inv.png");
        if (ticks == 330) { mc.setScreen(null); BuffListConfig.save(); LOG.info("[PackTest] DONE"); mc.stop(); }
    }

    /** 说明验证：-Dbufflist.effects=id[*等级],... 按 6 个一组加效果，逐个悬停截图提示框。 */
    private static void desc(Minecraft mc) {
        String[] all = System.getProperty("bufflist.effects", "").split(",");
        int per = 6, step = 40;
        int groups = (all.length + per - 1) / per;
        int slot = 30 + per * step;
        if (ticks < 200) return;
        int g = (ticks - 200) / slot, t = (ticks - 200) % slot;
        if (g >= groups) {
            if (t == 0) { LOG.info("[PackTest] DONE"); mc.stop(); }
            return;
        }
        if (t == 0) {
            mc.setScreen(null);
            cmd(mc, "effect clear @s");
            cmd(mc, "gamerule doDaylightCycle false");
            for (int i = g * per; i < Math.min(all.length, g * per + per); i++) {
                String[] p = all[i].split("\\*");
                int lvl = p.length > 1 ? Integer.parseInt(p[1]) : 1;
                cmd(mc, "effect give @s " + p[0] + " 1000 " + (lvl - 1) + " true");
            }
            BuffListConfig.DESC_MODE.set(BuffListConfig.DescMode.ALWAYS);
        }
        if (t == 20) mc.setScreen(new InventoryScreen(mc.player));
        int k = (t - 30) / step, kt = (t - 30) % step;
        int idx = g * per + k;
        if (t >= 30 && idx < all.length) {
            ResourceLocation id = new ResourceLocation(all[idx].split("\\*")[0]);
            if (kt == 0) {
                MobEffect ef = ForgeRegistries.MOB_EFFECTS.getValue(id);
                int[] r = com.bufflist.EffectListRenderer.LAST_ROWS.get(ef);
                LOG.info("[PackTest] hover {} row={}", id, r == null ? "none" : java.util.Arrays.toString(r));
                if (r != null) moveMouse(mc, r[0] + r[2] / 2.0, r[1] + r[3] / 2.0);
                else moveMouse(mc, 0, 0);
            }
            if (kt == 12) shot(mc, "packtest/desc_" + id.getNamespace() + "_" + id.getPath() + ".png");
        }
    }

    /** 导出全部已注册效果的清单（中文名、分类、属性修饰符、自带说明、实现类）到 gameDir/bufflist_survey.json。 */
    private static void survey(Minecraft mc) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (MobEffect ef : ForgeRegistries.MOB_EFFECTS) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(ef);
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("id", id.toString());
            o.addProperty("key", ef.getDescriptionId());
            o.addProperty("name", ef.getDisplayName().getString());
            o.addProperty("category", ef.getCategory().name());
            o.addProperty("instant", ef.isInstantenous());
            o.addProperty("color", Integer.toHexString(ef.getColor()));
            o.addProperty("class", ef.getClass().getName());
            o.addProperty("clientExt", IClientMobEffectExtensions.of(ef).getClass().getName());
            com.google.gson.JsonArray attrs = new com.google.gson.JsonArray();
            for (var en : ef.getAttributeModifiers().entrySet()) {
                com.google.gson.JsonObject a = new com.google.gson.JsonObject();
                a.addProperty("attr", String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(en.getKey())));
                a.addProperty("attrName", net.minecraft.client.resources.language.I18n.get(en.getKey().getDescriptionId()));
                a.addProperty("op", en.getValue().getOperation().name());
                a.addProperty("lv1", ef.getAttributeModifierValue(0, en.getValue()));
                a.addProperty("lv2", ef.getAttributeModifierValue(1, en.getValue()));
                attrs.add(a);
            }
            o.add("attrs", attrs);
            for (String s : new String[]{".desc", ".description"}) {
                String k = ef.getDescriptionId() + s;
                if (net.minecraft.client.resources.language.I18n.exists(k)) {
                    o.addProperty("desc", net.minecraft.client.resources.language.I18n.get(k));
                    break;
                }
            }
            arr.add(o);
        }
        try (var w = java.nio.file.Files.newBufferedWriter(mc.gameDirectory.toPath().resolve("bufflist_survey.json"))) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(arr, w);
        } catch (Exception ex) { LOG.error("[PackTest] survey write failed", ex); }
        LOG.info("[PackTest] survey wrote {} effects", arr.size());
        LOG.info("[PackTest] DONE");
        mc.stop();
    }

    /** 兼容修复验证：拉美西斯二世的诅咒的自绘图标 + 神化属性库的悬停提示。 */
    private static void compat(Minecraft mc) {
        if (ticks == 200) {
            cmd(mc, "gamerule doDaylightCycle false");
            cmd(mc, "time set noon");
            cmd(mc, "effect clear @s");
            for (String f : new String[]{"ramesses_curse:hit_slow", "ramesses_curse:drought", "ramesses_curse:flight_fall",
                    "ramesses_curse:inversion", "minecraft:strength", "minecraft:speed", "minecraft:health_boost",
                    "minecraft:resistance", "alexsmobs:orcas_might"})
                cmd(mc, "effect give @s " + f + " 1000 1");
        }
        if (ticks == 240) mc.setScreen(new InventoryScreen(mc.player));
        if (ticks == 280) shot(mc, "packtest/compat_list.png");
        if (ticks == 285) BuffListConfig.INVENTORY_ENABLED.set(false);
        if (ticks == 300) shot(mc, "packtest/compat_vanilla.png");
        if (ticks == 305) BuffListConfig.INVENTORY_ENABLED.set(true);
        String[] hov = {"minecraft:strength", "minecraft:speed", "minecraft:health_boost", "ramesses_curse:drought"};
        for (int k = 0; k < hov.length; k++) {
            int t = 320 + k * 30;
            if (ticks == t) {
                BuffListConfig.DESC_MODE.set(BuffListConfig.DescMode.ALWAYS);
                MobEffect ef = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(hov[k]));
                int[] r = com.bufflist.EffectListRenderer.LAST_ROWS.get(ef);
                LOG.info("[PackTest] hover {} row={}", hov[k], r == null ? "none" : java.util.Arrays.toString(r));
                if (r != null) moveMouse(mc, r[0] + r[2] / 2.0, r[1] + r[3] / 2.0);
            }
            if (ticks == t + 15) shot(mc, "packtest/compat_tip_" + k + ".png");
        }
        if (ticks == 460) {
            BuffListConfig.DESC_MODE.set(BuffListConfig.DescMode.SHIFT);
            LOG.info("[PackTest] DONE");
            mc.stop();
        }
    }

    private static void moveMouse(Minecraft mc, double gx, double gy) {
        double sc = mc.getWindow().getGuiScale();
        try {
            // 生产环境字段名是混淆名；MouseHandler 里声明的前两个 double 字段就是 xpos、ypos
            int n = 0;
            for (var fld : net.minecraft.client.MouseHandler.class.getDeclaredFields()) {
                if (fld.getType() != double.class || java.lang.reflect.Modifier.isStatic(fld.getModifiers())) continue;
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, (n == 0 ? gx : gy) * sc);
                if (++n == 2) break;
            }
        } catch (Exception ex) { LOG.warn("[PackTest] moveMouse failed", ex); }
    }

    /** 复现玩家场景：佩戴圣骑士护盾饰品 + 几个常驻效果，背包开着，每 5 秒记录一次并截图。 */
    private static void paladin(Minecraft mc) {
        if (ticks == 200) {
            cmd(mc, "gamerule doDaylightCycle false");
            cmd(mc, "time set noon");
            cmd(mc, "effect clear @s");
            cmd(mc, "curios replace accessory 0 @s with confluence:paladins_shield");
            cmd(mc, "effect give @s minecraft:speed 1000 1");
            cmd(mc, "effect give @s minecraft:invisibility 1000 0");
            cmd(mc, "effect give @s minecraft:jump_boost 1000 1");
            cmd(mc, "effect give @s minecraft:hero_of_the_village 1000 0");
            cmd(mc, "effect give @s minecraft:night_vision 1000 0");
            cmd(mc, "effect give @s minecraft:strength 1000 1");
        }
        if (ticks == 220) {
            BuffListConfig.INVENTORY_ENABLED.set(true);
            mc.setScreen(new InventoryScreen(mc.player));
        }
        if (ticks > 220 && ticks % 20 == 0) {
            StringBuilder sb = new StringBuilder();
            boolean pal = false, palListed = false;
            for (MobEffectInstance inst : mc.player.getActiveEffects()) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
                sb.append(id.getPath()).append('(').append(inst.getAmplifier()).append(',').append(inst.getDuration()).append(") ");
                if (id.getPath().equals("paladins_shield")) {
                    pal = true;
                    palListed = IClientMobEffectExtensions.of(inst).isVisibleInInventory(inst);
                }
            }
            LOG.info("[PackTest] t={} screen={} paladin={} listed={} | {}", ticks, mc.screen == null ? "-" : mc.screen.getClass().getSimpleName(), pal, palListed, sb);
        }
        if (ticks > 220 && ticks % 100 == 0) shot(mc, "packtest/paladin_t" + ticks + "_bufflist.png");
        if (ticks > 220 && ticks % 100 == 40) BuffListConfig.INVENTORY_ENABLED.set(false);
        if (ticks > 220 && ticks % 100 == 50) shot(mc, "packtest/paladin_t" + ticks + "_vanilla.png");
        if (ticks > 220 && ticks % 100 == 60) BuffListConfig.INVENTORY_ENABLED.set(true);
        if (ticks == 1500) { LOG.info("[PackTest] DONE"); mc.stop(); }
    }

    private static List<List<ResourceLocation>> buildGroups() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (MobEffect ef : ForgeRegistries.MOB_EFFECTS) {
            if (ef.isInstantenous() || ef.getCategory() == MobEffectCategory.HARMFUL) continue;
            ids.add(ForgeRegistries.MOB_EFFECTS.getKey(ef));
        }
        ids.sort(null);
        List<List<ResourceLocation>> out = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += GROUP) out.add(new ArrayList<>(ids.subList(i, Math.min(ids.size(), i + GROUP))));
        return out;
    }

    private static void dump(Minecraft mc, int g, List<ResourceLocation> grp) {
        List<String> got = new ArrayList<>();
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
            IClientMobEffectExtensions ext = IClientMobEffectExtensions.of(inst);
            got.add(id.toString());
            LOG.info("[PackTest] g{} {} '{}' showIcon={} inv={} gui={} ext={}", g, id, inst.getEffect().getDisplayName().getString(),
                    inst.showIcon(), ext.isVisibleInInventory(inst), ext.isVisibleInGui(inst), ext.getClass().getName());
        }
        for (ResourceLocation id : grp) if (!got.contains(id.toString())) LOG.info("[PackTest] g{} {} NOT-ACTIVE-ON-CLIENT", g, id);
    }

    private static void startWorld(Minecraft mc) {
        worldStarted = true;
        mc.options.pauseOnLostFocus = false;
        String name = "packtest" + System.currentTimeMillis();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings ls = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions);
    }

    private static void cmd(Minecraft mc, String c) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withEntity(server.getPlayerList().getPlayers().get(0))
                        .withPermission(4).withSuppressedOutput(), c));
    }

    private static void shot(Minecraft mc, String file) {
        new java.io.File(mc.gameDirectory, "screenshots/packtest").mkdirs();
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> {});
    }
}
