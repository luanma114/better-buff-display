package com.bufflist;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * 效果说明，三个来源：
 * 1. 属性修饰符（按实际等级自动计算，模组效果同样适用）
 * 2. 本模组为原版效果手写的说明（bufflist.desc.<命名空间>.<路径>，可用资源包覆盖/补充）
 * 3. 其他模组按 JEED / Effect Descriptions 约定提供的 <效果翻译键>.description / .desc
 */
public final class EffectDescriptions {
    private static final DecimalFormat NUM = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private EffectDescriptions() {}

    /**
     * @param screen 当前背包界面（HUD 里为 null）。装了 Apothic Attributes 时，借它的
     *               GatherEffectScreenTooltipsEvent 拿属性与说明行，与原版背包悬停提示保持一致。
     */
    public static List<Component> lines(MobEffectInstance inst, @Nullable EffectRenderingInventoryScreen<?> screen) {
        MobEffect effect = inst.getEffect().value();
        int amp = inst.getAmplifier();
        int lvl = amp + 1;
        List<Component> out = new ArrayList<>();
        ResourceLocation id = Platform.get().effectId(effect);

        String base = effect.getDescriptionId();
        // 1. 本模组 / 资源包提供的说明（有则替代模组自带说明，避免重复）
        boolean own = id != null && addOwnDesc(out, id, amp, lvl);

        List<Component> gathered = screen == null ? null : Platform.get().gatherTooltip(screen, inst);
        if (gathered != null) {
            for (Component c : gathered) {
                if (own && c.getContents() instanceof TranslatableContents tc
                        && (tc.getKey().equals(base + ".desc") || tc.getKey().equals(base + ".description"))) continue;
                out.add(c);
            }
            if (id != null) addNote(out, id, amp, lvl);
            addSource(out, id);
            return out;
        }

        // 2. 其他模组自带的说明
        if (!own) {
            for (String suffix : new String[]{".description", ".desc"}) {
                if (I18n.exists(base + suffix)) {
                    out.add(Component.translatable(base + suffix).withStyle(ChatFormatting.GRAY));
                    break;
                }
            }
        }

        // 3. 属性
        effect.createModifiers(amp, (attr, mod) -> {
            double value = mod.amount();
            if (value == 0) return;
            AttributeModifier.Operation op = mod.operation();
            double shown = op == AttributeModifier.Operation.ADD_VALUE ? value : value * 100.0;
            String key = (value > 0 ? "attribute.modifier.plus." : "attribute.modifier.take.") + op.id();
            out.add(Component.translatable(key, NUM.format(Math.abs(shown)),
                            Component.translatable(attr.value().getDescriptionId()))
                    .withStyle(value > 0 ? ChatFormatting.BLUE : ChatFormatting.RED));
        });

        if (id != null) addNote(out, id, amp, lvl);
        addSource(out, id);
        return out;
    }

    /**
     * 整合包对该效果的改动备注（bufflist.note.<命名空间>.<路径>），不替代原说明，单独成行提示。
     * 可有多条，用换行分隔；每条可用 {mod:模组id[,模组id]} 开头，只有这些模组都已安装时才显示。
     */
    private static void addNote(List<Component> out, ResourceLocation id, int amp, int lvl) {
        String key = "bufflist.note." + id.getNamespace() + "." + id.getPath();
        if (!I18n.exists(key)) return;
        String all = Component.translatable(key, args(id, amp, lvl)).getString();
        for (String line : all.split("\n")) {
            line = line.trim();
            if (line.startsWith("{mod:")) {
                int end = line.indexOf('}');
                if (end < 0) continue;
                boolean ok = true;
                for (String mod : line.substring(5, end).split(",")) {
                    if (!mod.isBlank() && !Platform.get().isModLoaded(mod.trim())) { ok = false; break; }
                }
                if (!ok) continue;
                line = line.substring(end + 1).trim();
            }
            if (line.isEmpty()) continue;
            out.add(Component.translatable("bufflist.tooltip.pack_note", Formula.expand(line, lvl)).withStyle(ChatFormatting.GOLD));
        }
    }

    private static boolean addOwnDesc(List<Component> out, ResourceLocation id, int amp, int lvl) {
        String key = "bufflist.desc." + id.getNamespace() + "." + id.getPath();
        if (!I18n.exists(key)) return false;
        String s = Component.translatable(key, args(id, amp, lvl)).getString();
        out.add(Component.literal(Formula.expand(s, lvl)).withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** 非原版效果：标注来源模组。 */
    private static void addSource(List<Component> out, @Nullable ResourceLocation id) {
        if (id == null || "minecraft".equals(id.getNamespace())) return;
        String ns = id.getNamespace();
        String modName = Platform.get().modName(ns);
        out.add(Component.translatable("bufflist.tooltip.source", modName)
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }

    private static String sec(int ticks) {
        return NUM.format(Math.max(1, ticks) / 20.0);
    }

    /** 原版效果说明里的数值（与 1.20.1 原版逻辑一致）。 */
    private static Object[] args(ResourceLocation id, int amp, int lvl) {
        if (!"minecraft".equals(id.getNamespace())) return new Object[]{lvl};
        switch (id.getPath()) {
            case "regeneration": return new Object[]{sec(50 >> amp)};
            case "poison": return new Object[]{sec(25 >> amp)};
            case "wither": return new Object[]{sec(40 >> amp)};
            case "haste":
            case "conduit_power": return new Object[]{NUM.format(20 * lvl)};
            case "mining_fatigue": {
                double f = switch (amp) { case 0 -> 0.3; case 1 -> 0.09; case 2 -> 0.0027; default -> 8.1E-4; };
                return new Object[]{NUM.format(f * 100) + "%"};
            }
            case "resistance": return new Object[]{NUM.format(Math.min(100, 20 * lvl))};
            case "instant_health": return new Object[]{NUM.format(4 << Math.min(amp, 20)), NUM.format((4 << Math.min(amp, 20)) / 2.0)};
            case "instant_damage": return new Object[]{NUM.format(6 << Math.min(amp, 20))};
            case "absorption": return new Object[]{NUM.format(4 * lvl), NUM.format(2 * lvl)};
            case "saturation": return new Object[]{NUM.format(lvl), NUM.format(2 * lvl)};
            case "levitation": return new Object[]{NUM.format(lvl)};
            case "jump_boost": return new Object[]{NUM.format(lvl)};
            case "hunger": return new Object[]{NUM.format(40.0 / lvl)};
            case "health_boost": return new Object[]{NUM.format(2 * lvl)};
            case "hero_of_the_village": return new Object[]{NUM.format(30 + 6.25 * amp)};
            case "bad_omen": return new Object[]{NUM.format(Math.min(lvl, 5))};
            case "luck":
            case "unluck": return new Object[]{NUM.format(lvl)};
            default: return new Object[]{lvl};
        }
    }
}
