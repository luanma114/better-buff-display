package com.bufflist.forge;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 可选联动 Apothic Attributes（神化属性库，modid attributeslib）。
 * 它用 coremod 往原版背包的效果悬停提示里加属性与说明，我们取消了原版绘制，
 * 所以在这里自己发它的 GatherEffectScreenTooltipsEvent，拿到同样的内容。
 * 只用反射，不装该模组时完全不受影响。
 */
final class ApothicCompat {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String EVENT = "dev.shadowsoffire.attributeslib.api.client.GatherEffectScreenTooltipsEvent";

    private static boolean init, failed;
    private static Constructor<?> ctor;
    private static Method getTooltip;

    private ApothicCompat() {}

    /** 返回原版提示「名称 + 时长」之外新增的行；未安装或出错时返回 null（调用方回退到自带逻辑）。 */
    @Nullable
    static List<Component> gather(EffectRenderingInventoryScreen<?> screen, MobEffectInstance inst) {
        if (!ready()) return null;
        try {
            List<Component> base = new ArrayList<>();
            base.add(inst.getEffect().getDisplayName().copy());
            base.add(MobEffectUtil.formatDuration(inst, 1.0F));
            Event ev = (Event) ctor.newInstance(screen, inst, base);
            MinecraftForge.EVENT_BUS.post(ev);
            @SuppressWarnings("unchecked")
            List<Component> result = (List<Component>) getTooltip.invoke(ev);
            // 第 0 行是名称（Apothic 把时长并进去了），我们自己的标题已经有名称和时长，丢掉
            List<Component> extra = new ArrayList<>();
            for (int i = 1; i < result.size(); i++) {
                Component c = result.get(i);
                // 没有其他模组改动时，第 1 行可能仍是原始时长，跳过
                if (i == 1 && c == base.get(1)) continue;
                extra.add(c);
            }
            return extra;
        } catch (Throwable t) {
            failed = true;
            LOG.warn("[BuffList] Apothic Attributes tooltip compat disabled", t);
            return null;
        }
    }

    private static boolean ready() {
        if (failed) return false;
        if (init) return ctor != null;
        init = true;
        if (!ModList.get().isLoaded("attributeslib")) return false;
        try {
            Class<?> c = Class.forName(EVENT);
            ctor = c.getConstructor(EffectRenderingInventoryScreen.class, MobEffectInstance.class, List.class);
            getTooltip = c.getMethod("getTooltip");
            return true;
        } catch (Throwable t) {
            LOG.warn("[BuffList] Apothic Attributes found but event API not compatible", t);
            ctor = null;
            return false;
        }
    }
}
