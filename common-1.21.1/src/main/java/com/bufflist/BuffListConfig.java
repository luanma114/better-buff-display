package com.bufflist;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 客户端配置 config/bufflist-client.toml。不依赖加载器的配置系统，三个平台通用。
 * 文件被手动改动后，下一个客户端 tick 自动重新读取。
 */
public final class BuffListConfig {
    private static final Logger LOG = LogUtils.getLogger();

    public enum Side { AUTO, LEFT, RIGHT }
    public enum HudMode { VANILLA, LIST, HIDDEN }
    public enum DescMode { OFF, SHIFT, ALWAYS }

    /** 按 section 分组，保持写出顺序。 */
    private static final Map<String, List<Value<?>>> SECTIONS = new LinkedHashMap<>();

    public static final BoolValue INVENTORY_ENABLED = bool("inventory", "enabled", true,
            "在背包/创造物品栏用列表替换原版状态效果显示 / Replace the vanilla effect display in the inventory with the list");
    public static final EnumValue<Side> INVENTORY_SIDE = enumValue("inventory", "side", Side.AUTO,
            "列表放在哪一侧 / Which side: AUTO, LEFT, RIGHT. AUTO = left when JEI/REI/EMI is installed, right when the recipe book is open");
    public static final DoubleValue INVENTORY_X = number("inventory", "x", -1.0, -1.0, 1.0,
            "拖动后的横向位置 0~1，-1 = 按 side 自动 / Dragged position 0~1, -1 = automatic");
    public static final DoubleValue INVENTORY_Y = number("inventory", "y", -1.0, -1.0, 1.0,
            "拖动后的纵向位置 0~1，-1 = 自动 / Dragged position 0~1, -1 = automatic");

    public static final BoolValue HUD_ENABLED = bool("hud", "enabled", true,
            "不开背包时屏幕上是否显示状态效果 / Show effects on screen outside the inventory");
    public static final EnumValue<HudMode> HUD_MODE = enumValue("hud", "mode", HudMode.VANILLA,
            "VANILLA = 原版图标 / vanilla icons, LIST = 列表 / list");
    public static final BoolValue HUD_SHOW_HIDDEN = bool("hud", "showHidden", true,
            "也显示隐藏了图标的效果（装备、饰品给的效果常这样）/ Also show effects whose icon is hidden (common for gear and curios)");
    public static final DoubleValue HUD_X = number("hud", "x", -1.0, -1.0, 1.0,
            "常驻列表拖动后的横向位置 0~1，-1 = 右上角 / Dragged position 0~1, -1 = top right");
    public static final DoubleValue HUD_Y = number("hud", "y", -1.0, -1.0, 1.0,
            "常驻列表拖动后的纵向位置 0~1，-1 = 默认 / Dragged position 0~1, -1 = default");

    public static final IntValue ICON_SIZE = integer("style", "iconSize", 9, 8, 18,
            "图标边长（像素，原版 18）/ Icon size in pixels (vanilla is 18)");
    public static final BoolValue SHOW_TOOLTIP = bool("style", "tooltip", true,
            "鼠标悬停时显示提示框 / Show a tooltip on hover");
    public static final IntValue BACKGROUND_ALPHA = integer("style", "backgroundAlpha", 200, 0, 255,
            "背景不透明度 0-255，0 = 不画背景 / Background opacity 0-255, 0 = no background");

    public static final EnumValue<DescMode> DESC_MODE = enumValue("descriptions", "mode", DescMode.SHIFT,
            "效果说明：OFF, SHIFT = 按住 Shift / hold Shift, ALWAYS");

    private static Path file;
    private static long lastModified = -1;

    private BuffListConfig() {}

    public static void init(Path configDir) {
        file = configDir.resolve("bufflist-client.toml");
        if (Files.exists(file)) load();
        save(); // 补齐缺失的项
    }

    /** 文件被外部改动时重新读取。每个客户端 tick 调一次，开销只是一次 stat。 */
    public static void pollFile() {
        if (file == null) return;
        try {
            if (Files.exists(file) && Files.getLastModifiedTime(file).toMillis() != lastModified) load();
        } catch (IOException ignored) {}
    }

    private static void load() {
        try {
            Map<String, String> raw = new HashMap<>();
            String section = "";
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                if (s.startsWith("[") && s.endsWith("]")) {
                    section = s.substring(1, s.length() - 1).trim();
                    continue;
                }
                int eq = s.indexOf('=');
                if (eq < 0) continue;
                String key = s.substring(0, eq).trim();
                String val = s.substring(eq + 1).trim();
                int hash = val.indexOf(" #");
                if (hash >= 0 && !val.startsWith("\"")) val = val.substring(0, hash).trim();
                if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length() - 1);
                raw.put(section + "." + key, val);
            }
            for (List<Value<?>> values : SECTIONS.values()) {
                for (Value<?> v : values) {
                    String s = raw.get(v.section + "." + v.key);
                    if (s != null) v.parse(s);
                }
            }
            lastModified = Files.getLastModifiedTime(file).toMillis();
        } catch (Exception e) {
            LOG.warn("[BuffList] could not read {}", file, e);
        }
    }

    public static void save() {
        if (file == null) return;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Value<?>>> sec : SECTIONS.entrySet()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append('[').append(sec.getKey()).append("]\n");
            for (Value<?> v : sec.getValue()) {
                sb.append("\t#").append(v.comment).append('\n');
                sb.append('\t').append(v.key).append(" = ").append(v.serialize()).append('\n');
            }
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
            lastModified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            LOG.warn("[BuffList] could not write {}", file, e);
        }
    }

    public static boolean hasCustomPos() {
        return INVENTORY_X.get() >= 0 && INVENTORY_Y.get() >= 0;
    }

    public static void setCustomPos(double x, double y) {
        INVENTORY_X.set(x);
        INVENTORY_Y.set(y);
        save();
    }

    /** 常驻显示是否开启（兼容旧配置里的 mode = HIDDEN）。 */
    public static boolean hudOn() {
        return HUD_ENABLED.get() && HUD_MODE.get() != HudMode.HIDDEN;
    }

    public static boolean hudList() {
        return HUD_MODE.get() == HudMode.LIST;
    }

    public static boolean hasHudPos() {
        return HUD_X.get() >= 0 && HUD_Y.get() >= 0;
    }

    public static void setHudPos(double x, double y) {
        HUD_X.set(x);
        HUD_Y.set(y);
        save();
    }

    // ---------------- 值类型 ----------------

    public abstract static class Value<T> {
        final String section, key, comment;
        T value;

        Value(String section, String key, T def, String comment) {
            this.section = section;
            this.key = key;
            this.value = def;
            this.comment = comment;
            SECTIONS.computeIfAbsent(section, s -> new ArrayList<>()).add(this);
        }

        public T get() {
            return value;
        }

        public void set(T v) {
            value = v;
        }

        abstract void parse(String s);

        String serialize() {
            return String.valueOf(value);
        }
    }

    public static final class BoolValue extends Value<Boolean> {
        BoolValue(String section, String key, boolean def, String comment) { super(section, key, def, comment); }

        @Override
        void parse(String s) { value = Boolean.parseBoolean(s); }
    }

    public static final class IntValue extends Value<Integer> {
        private final int min, max;

        IntValue(String section, String key, int def, int min, int max, String comment) {
            super(section, key, def, comment);
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Integer v) { value = Math.max(min, Math.min(max, v)); }

        @Override
        void parse(String s) {
            try { set(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private final double min, max;

        DoubleValue(String section, String key, double def, double min, double max, String comment) {
            super(section, key, def, comment);
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Double v) { value = Math.max(min, Math.min(max, v)); }

        @Override
        void parse(String s) {
            try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends Value<E> {
        private final Class<E> type;

        EnumValue(String section, String key, E def, String comment) {
            super(section, key, def, comment);
            this.type = def.getDeclaringClass();
        }

        @Override
        void parse(String s) {
            try { value = Enum.valueOf(type, s.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) {}
        }

        @Override
        String serialize() { return "\"" + value.name() + "\""; }
    }

    private static BoolValue bool(String sec, String key, boolean def, String comment) {
        return new BoolValue(sec, key, def, comment);
    }

    private static IntValue integer(String sec, String key, int def, int min, int max, String comment) {
        return new IntValue(sec, key, def, min, max, comment);
    }

    private static DoubleValue number(String sec, String key, double def, double min, double max, String comment) {
        return new DoubleValue(sec, key, def, min, max, comment);
    }

    private static <E extends Enum<E>> EnumValue<E> enumValue(String sec, String key, E def, String comment) {
        return new EnumValue<>(sec, key, def, comment);
    }
}
