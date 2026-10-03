# 静穹 · Quiet Dome

> **文档语言：中文 | [English](README.en.md)**
> 本模组已停止更新, 只兼容 26.2 fabric !!!

一个 Fabric 客户端模组，给 Minecraft 换一套冷调的界面效果、
视频菜单背景、平滑转场、粒子侵蚀悬停效果，外加内嵌的 FFmpeg 视频管线。

## 基本信息

| 项 | 值 |
|---|---|
| 显示名 | Quiet Dome |
| Mod ID | `quiet_dome` |
| 中文名 | 静穹 |
| 作者 | 魔汐-INT |
| 版本 | `Exptl-Alpha` |
| 产物 | `quiet_dome-Exptl-Alpha.jar` |

## 项目结构

```
quiet_dome/
├── build.gradle                 构建脚本（Loom、依赖、打 jar）
├── settings.gradle              插件仓库 + 项目名
├── gradle.properties            版本号、MC/Fabric/依赖版本都在这里
├── gradlew / gradlew.bat        跨平台构建入口
├── gradle/wrapper/              锁定 Gradle 9.7.0，clone 后首次构建会自动下载该版本
├── LICENSE                      项目本体 GPL-3.0
├── README.md                    本文件
└── src/
    ├── main/java/com/quietdome/         两端通用逻辑
    │   ├── Alias.java                   一个别名的数据载体
    │   ├── AliasRegistry.java           7 个别名的清单
    │   ├── AliasDeriver.java            主机名 → 哈希 → 下标
    │   ├── AliasConfig.java             回退种子持久化（quiet_dome.json）
    │   ├── AliasManager.java            算一次、缓存
    │   ├── DomeConfig.java              视觉开关（quiet_dome_visual.json）
    │   └── DomeTextures.java            贴图 ID 常量
    │
    ├── client/java/com/quietdome/client/ 仅客户端
    │   ├── QuietDomeClient.java         入口：打印别名 + 后台解压视频
    │   ├── VideoManager.java            找/解压视频、分辨率防抖
    │   ├── VideoPlayer.java             解码线程 → NativeImage → GPU 纹理
    │   ├── LinearDynamicTexture.java    线性过滤版 DynamicTexture（防马赛克）
    │   ├── DomeBackground.java          菜单背景：黑洞视频 + 暗角 + 星空
    │   ├── DomeTransition.java          进主菜单转场（坠入→黑→白→扩散）
    │   ├── DomeRender.java              画图工具（cover/blit/圆洞/伪随机）
    │   ├── DomeVideoAdapter.java        背景中间层（视频优先，退静图）
    │   ├── EntranceAnim.java            每个界面显示时的黑幕淡入
    │   ├── SelectionGlow.java           悬停蓝框 + 粒子侵蚀
    │   ├── Corruption.java              侵蚀度累计（悬停才涨、移开暂停）
    │   ├── Starfield.java               背景飘星
    │   └── mixin/                       字节码注入，改动原版界面
    │       ├── ScreenMixin.java         背景路由 + 入场黑幕（挂 final 方法）
    │       ├── TitleScreenMixin.java    主菜单转场
    │       ├── LoadingOverlayMixin.java 加载屏：黑洞 + Mojang logo + 大进度条
    │       ├── VideoSettingsScreenMixin.java  视频设置里加分辨率滑条
    │       ├── OptionsSubScreenAccessor.java  取父类的 list 字段
    │       ├── ViaConfigScreenMixin.java      ViaFabric 设置换皮+重排（未测）
    │       └── FabricModMixin.java            Mod Menu 条目显示别名
    │
    ├── main/resources/                  打进 jar 的资源
    │   ├── fabric.mod.json              模组元数据、入口、mixin 清单、依赖
    │   ├── assets/quiet_dome/
    │   │   ├── icon.png                 模组图标
    │   │   ├── font/saira_condensed.ttf 字体
    │   │   ├── lang/{en_us,zh_cn}.json  语言文件
    │   │   ├── textures/                静图（birdcage / blackhole / solivault）
    │   │   └── videos/heidong.mp4       黑洞视频（唯一视频）
    │   └── licenses/                    第三方许可证全文
    │
    └── client/resources/                仅客户端的 mixin 配置
        ├── quiet_dome.client.mixins.json
        ├── quiet_dome.options.mixins.json
        ├── quiet_dome.viafabric.mixins.json
        └── quiet_dome.modmenu.mixins.json
```

**几个关键点**

- `src/main` 和 `src/client` 是分开的（Loom 的 `splitEnvironmentSourceSets`），
  只有客户端的东西放 `client`，两端通用的放 `main`。
- 视频的**唯一真身**是 `assets/quiet_dome/videos/heidong.mp4`，别的都是副本。
- 所有第三方许可证在 `licenses/`，会随 jar 一起发出去。

## 视觉特性

| 界面 | 效果 |
|---|---|
| 加载屏 | 循环黑洞视频 + 青色进度条 + 原版 Mojang logo |
| 主菜单 | 循环黑洞视频背景 + 暗角 + 星空 |
| 单人 / 多人 | 同上（黑洞视频背景） |
| 进主菜单的转场 | 程序化：坠入 → 全黑 → 泛白 → 从中心扩散露出菜单 |
| 菜单按钮 | 悬停边框 + 蓝色粒子侵蚀（按钮会被慢慢糊住） |
| 所有菜单 | 进入和返回都有黑幕淡入 |
| 视频设置 | 多出一根「静穹视频分辨率」滑条（拖动即时生效） |
| ViaFabric 设置(未测) | 深色面板 + 青边 + 控件重排 |

所有效果基本走 26.2 的 `GuiGraphicsExtractor` 渲染模型。

---

## ⚠ 视频放哪（重点）

模组只有 **一个** 视频：`heidong.mp4`，黑洞循环，加载屏和菜单背景都用它。
视频**打进 jar**，首次启动时解压到磁盘。

### 改内置视频（开发者，重新构建前）

把你的文件放这——**这是唯一会被打进 jar 的目录**：

```
src/main/resources/assets/quiet_dome/videos/heidong.mp4
```

然后 `gradle build`，`processResources` 会自动收进去。

> 别在项目根目录留副本——那里不参与打包，只会造成混乱、白占空间。
> `resources/assets/.../videos/` 里的那份才算数。

### 运行时换视频（玩家，不用重编）

把你的文件放这（优先于 jar 里的）：

```
<游戏目录>/config/quiet_dome/videos/heidong.mp4
```

首次启动时模组会把自己的副本解到这个目录；如果你已经放了，它不会覆盖。

### 解码参数

| 文件 | 原片比例 | 说明 |
|---|---|---|
| `heidong.mp4` | 16:9 | 加载屏 + 菜单背景，循环 |

解码宽度可在游戏内调（视频设置滑条），高度按原片比例推。
默认宽 1280px，15fps。

进主菜单的转场（坠入 → 黑 → 白 → 扩散）是**程序化**的，不是视频——见 `DomeTransition`。

---

## 配置

`config/quiet_dome_visual.json` —— 视觉开关和解码分辨率：

```json
{
  "loadingScreen": true,
  "menuBackground": true,
  "transitions": true,
  "starfield": true,
  "subScreens": true,
  "videoResolution": 1280,
  "backgroundZoom": 1.08
}
```

`config/quiet_dome.json` —— 每台机器的别名（见下）。

## 别名（内部机制）

模组会按主机名派生出一个别名（7 选 1；主机名拿不到就退回一个存下来的随机种子）。
这个别名只用在 Mod Menu 的条目标题上，界面上不画任何水印。

> 这里别名就不一一列出了...

## 运行环境

- Minecraft 26.2 · Java 25 · Fabric Loader 0.19.x · Fabric API (26.2)
- 可选：Mod Menu、ViaFabric
- 内置 FFmpeg（经 JavaCV，仅 windows-x86_64，约 32MB）

## 构建

```
gradle build
```

产物：`build/libs/quiet_dome-Exptl-Alpha.jar`

## 许可与第三方声明

静穹本体是 **GPL-3.0**。所有打包组件的许可证全文都在 jar 的 `licenses/` 目录里：

| 组件 | 许可证 | 说明 |
|---|---|---|
| 静穹 | GPL-3.0 | 本项目 |
| FFmpeg（经 JavaCPP presets） | LGPL-3.0 | 用的是 `org.bytedeco:ffmpeg:8.1.2-1.5.14`（无 `-gpl` 后缀 = LGPL 构建）；负责视频解码 |
| JavaCPP JNI 胶水 | Apache-2.0 | 与 GPL-3.0 兼容 |
| Saira Condensed 字体 | OFL-1.1 | © 2016 The Saira Project Authors |
| `heidong.mp4` | 见下 | 背景视频 |

LGPL-3.0 和 Apache-2.0 都与 GPL-3.0 兼容（LGPLv3 文本明确声明纳入 GPLv3 条款）。
OFL 字体是作为数据文件随附的，不是链接进代码，所以不影响模组自身的许可。

### 视频来源与使用限制

`heidong.mp4` 来自哲风壁纸：

> https://haowallpaper.com/homeViewLook/18890446631980416

该站《用户协议》原文规定：
> /!\ 禁止商业化的使用：严格禁止用户以「"商业化盈利"、"变相引流盈利性质"」的使用本网站的内容……
> 若进行非商业化非变相盈利的搬运，则搬运后需标注来源为「哲风壁纸」以便互知，
> 若原创用户在简介或帖子等内容中声明禁止搬运，则不可进行搬运……
> —— haowallpaper.com 用户协议（2023-10-20 生效，2025-09-12 修订）

**由此可确定：**
1. **禁止商用** —— 本模组不得以任何形式收费、订阅、引流盈利。
2. **非商业搬运须标注来源** —— 本文已标注「哲风壁纸」，满足这一条。

**协议未明确、存在不确定性的一点：**
协议没有直接规定"把壁纸**打包进软件**再分发"是否算被允许的"搬运"。
它只说了"非商业化搬运需标注来源"，以及"原创作者声明禁止搬运的不可搬运"。

**给使用方的建议：**
- **个人使用**：无风险，直接用。
- **公开分发**（发到平台、分享给他人）：建议先替换为自己的素材，
  或联系哲风壁纸/原作者取得许可，再发布。
- **商业化**（收费、引流）：**协议明确禁止，不要这么做。**

> 经查，该壁纸页上传者未声明禁止搬运，故适用站点默认条款（非商业 + 标注来源）。
> 但"打包进软件再分发"是否属于协议所称的"搬运"，协议未明确，仍建议公开发布前替换为自有素材。
> 如果本项目有所侵权请联系我删除“ tnt111h111@outlook.com ”

## 许可

GPL-3.0，见 [LICENSE](LICENSE)。
