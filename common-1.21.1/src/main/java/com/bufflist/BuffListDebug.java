package com.bufflist;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** 客户端命令 /bufflist dump：列出玩家身上所有效果及其可见性，用来排查与其它模组的兼容问题。 */
public final class BuffListDebug {
    private static final Logger LOG = LogUtils.getLogger();

    private BuffListDebug() {}

    /** 各平台把它注册成客户端命令 /bufflist dump。 */
    public static int dump() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        List<MobEffectInstance> all = new ArrayList<>(mc.player.getActiveEffects());
        List<String> lines = new ArrayList<>();
        lines.add("[BuffList] active effects: " + all.size()
                + " (map size " + mc.player.getActiveEffectsMap().size() + ")");
        for (MobEffectInstance inst : all) {
            Platform pf = Platform.get();
            String line = String.format("%s | %s | amp=%d dur=%d ambient=%s showIcon=%s visible=%s inv=%s gui=%s%s",
                    pf.effectId(inst.getEffect().value()),
                    inst.getEffect().value().getDisplayName().getString(),
                    inst.getAmplifier(), inst.getDuration(), inst.isAmbient(), inst.showIcon(),
                    inst.isVisible(), safe(() -> pf.visibleInInventory(inst)), safe(() -> pf.visibleInGui(inst)),
                    pf.debugInfo(inst));
            lines.add(line);
        }
        for (String l : lines) {
            LOG.info(l);
            mc.player.sendSystemMessage(Component.literal(l).withStyle(ChatFormatting.GRAY));
        }
        mc.player.sendSystemMessage(Component.translatable("bufflist.debug.written").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static String safe(java.util.function.BooleanSupplier s) {
        try { return Boolean.toString(s.getAsBoolean()); }
        catch (Throwable t) { return "ERR:" + t.getClass().getSimpleName(); }
    }
}
