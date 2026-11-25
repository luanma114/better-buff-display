# Better Buff Display · 更好的buff显示

Client-side Minecraft mod that turns the pile of effect icons in your inventory into a list you can read: icon, name, level and time left on each row. Hold Shift on a row to see what the effect does, with the numbers for your level.

- Forge 1.20.1 · Fabric 1.20.1 · NeoForge 1.21.1 · Fabric 1.21.1
- Descriptions for vanilla and about 370 modded effects, English and Chinese; resource packs can add more (see the last section)
- Moves out of the way of JEI / REI / EMI and the recipe book; drag it anywhere
- HUD can stay vanilla icons or become a list; effects with hidden icons (from gear and curios) still show
- `/bufflist dump` prints every active effect and its visibility flags for bug reports

Building: `gradle build` (Gradle 8.8+, JDK 17) in the repo root builds Forge 1.20.1; the 1.21.1 ports need JDK 21. The other loaders live in `ports/` and share code from `common/` (1.20.1) and `common-1.21.1/`. `gradle runClient -Psmoke` (or `runSmoke` for Forge) starts a self-test that creates a world, applies effects, takes screenshots and quits.

License: MIT

---

Minecraft 纯客户端模组。把背包里挤成一团的状态效果图标换成整齐的列表：图标 + 名称 + 等级 + 剩余时间；悬停可查看带具体数值的效果说明。服务器不用装。

## 安装

到 [Releases](../../releases)、CurseForge 或 Modrinth 下载对应版本，放进 `mods` 文件夹：

| 文件 | 游戏版本 | 加载器 |
|---|---|---|
| `better-buff-display-1.20.1-forge-*.jar` | 1.20.1 | Forge 47+ |
| `better-buff-display-1.20.1-fabric-*.jar` | 1.20.1 | Fabric（需要 Fabric API） |
| `better-buff-display-1.21.1-neoforge-*.jar` | 1.21.1 | NeoForge 21.1+ |
| `better-buff-display-1.21.1-fabric-*.jar` | 1.21.1 | Fabric（需要 Fabric API） |

- 纯客户端，服务器不用装
- 效果说明已经打包在模组里：原版效果，加上约 370 个模组效果（中英文），不需要另装资源包
- Fabric 版装了 Mod Menu 时可以从模组列表打开设置
- 想修改或补充说明，可以用资源包覆盖（见文末「编写效果说明」）

## 使用

### 背包列表
打开背包，原版的效果图标会换成列表。列表位置默认自动选择：装了 JEI / REI / EMI 时放左边，打开配方书时放右边，否则放右边；一边放不下会自动换到另一边。

列表右上角有两个小按钮：
- **齿轮**：打开设置界面，所有选项都能直接点着改（也可以从模组列表的「配置」按钮进入）
- **十字箭头**：按住拖动，把列表放到屏幕任意位置，松开后位置会记住；想恢复自动摆放，在设置里把「列表位置」改回自动/左侧/右侧

### 查看效果说明
鼠标悬停在某一行上会显示名称和剩余时间，**按住 Shift** 显示完整说明，从上到下依次是：

| 内容 | 说明 |
|---|---|
| 效果说明 | 效果具体做什么，带数值；跟等级挂钩的数值会按当前等级算好 |
| 属性加成 | 例如「+20% 攻击伤害」，按实际等级自动计算（装了 Apothic Attributes 时与其显示一致） |
| 整合包改动（金色） | 整合包对该效果的魔改，只有做出改动的模组已安装时才显示 |
| 来自：某模组 | 非原版效果标注来源模组 |

### 常驻显示
默认开启，样式是「图标」，即原版那一排方块图标。在设置界面可以用「常驻显示」开关它，用「常驻样式」切换图标/列表；改成列表后可以点「调整常驻显示位置」，按住列表拖到屏幕任意位置。也可以编辑 `config\bufflist-client.toml`，把 `[hud]` 下的
```toml
mode = "VANILLA"
```
改成
```toml
mode = "LIST"
```
保存后自动生效（没生效就重进一次游戏）。打开背包时右上角列表不显示，因为背包里已经有一份。

## 全部配置项

配置文件：`config\bufflist-client.toml`

### `[inventory]` 背包

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 在背包/创造物品栏用列表替换原版状态效果显示，设为 `false` 恢复原版 |
| `side` | `"AUTO"` | 列表放哪一侧：`AUTO` / `LEFT` / `RIGHT` |
| `x` / `y` | `-1.0` | 拖动后的位置（0~1，0 贴左/顶，1 贴右/底）；`-1` 表示没拖过，按 `side` 摆放 |

### `[hud]` 常驻显示（不开背包时的画面）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 常驻显示开关 |
| `showHidden` | `true` | 常驻显示也显示隐藏了图标的效果（装备、饰品给的效果常这样），和背包里一致；对原版图标和物品栏HUD+等接管图标的模组都生效 |
| `mode` | `"VANILLA"` | 常驻样式：`VANILLA` = 图标（原版那一排方块图标）；`LIST` = 列表 |
| `x` / `y` | `-1.0` | 常驻列表拖动后的位置（0~1）；`-1` 表示默认右上角 |

### `[style]` 样式

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `iconSize` | `9` | 图标边长（像素），范围 8–18，原版为 18 |
| `tooltip` | `true` | 鼠标悬停条目时显示提示框 |
| `backgroundAlpha` | `200` | 背景面板不透明度，范围 0–255，0 为不画背景 |

### `[descriptions]` 效果说明

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `mode` | `"SHIFT"` | `OFF` = 不显示；`SHIFT` = 按住 Shift 显示；`ALWAYS` = 一直显示 |

## 排查问题

聊天框输入 `/bufflist dump`，会列出身上每个效果的 ID、等级、时长和各项可见性，同时写入 `logs/latest.log`。某个效果显示不对时，把这些内容发给作者即可定位。

## 兼容性

- **JEI / REI / EMI、配方书**：列表自动避让
- **Apothic Attributes（神化属性库）**：悬停提示里的属性与说明行与原版背包一致
- **自定义图标的效果**：优先使用效果的标准贴图；没有标准贴图时才交给模组自己绘制，并限制在图标格子内，避免错位

---

## 编写效果说明（资源包作者）

效果说明完全由语言文件提供，任何资源包都可以添加或覆盖，键名规则：

| 键 | 作用 |
|---|---|
| `bufflist.desc.<命名空间>.<效果路径>` | 效果说明；存在时替代模组自带的 `.desc` / `.description` 说明 |
| `bufflist.note.<命名空间>.<效果路径>` | 整合包改动备注，以金色「整合包改动：…」单独显示，不替代原说明 |

例如效果 `terramity:lifesteal` 的说明键是 `bufflist.desc.terramity.lifesteal`。

### 等级公式
文字里可以写 `{=表达式}`，显示时按当前等级算出数值：
- 变量：`lvl`（等级，I 级 = 1）、`amp`（= lvl − 1）
- 运算：`+ - * / ^`、括号、`min(a,b)`、`max(a,b)`
- 例：`有 {=lvl*10}% 概率不消耗弹药` → III 级显示「有 30% 概率不消耗弹药」

### 按模组条件显示备注
`bufflist.note.*` 的每一条可以用 `{mod:模组id}` 开头，只有这些模组都已安装时才显示；多条用换行 `\n` 分隔，各自带标记：
```json
"bufflist.note.minecraft.resistance": "{mod:celestial_overhaul}星月改革改为乘法叠加，伤害 ×0.8^等级，当前减伤 {=100-100*0.8^lvl}%（原版为每级 20%）"
```
