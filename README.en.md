# Quiet Dome · 静穹

> **Language: [中文](README.md) | English**

> This mod is no longer maintained. It only supports Fabric on 26.2.

A Fabric client-side mod that gives Minecraft a colder, sci-fi look:
a video menu backdrop, smooth transitions, a particle-erosion hover effect,
plus an embedded FFmpeg video pipeline.

## Basics

| Field | Value |
|---|---|
| Display name | Quiet Dome |
| Mod ID | `quiet_dome` |
| Chinese name | 静穹 |
| Author | 魔汐-INT |
| Version | `Exptl-Alpha` |
| Artifact | `quiet_dome-Exptl-Alpha.jar` |

## Project layout

```
quiet_dome/
├── build.gradle                 build script (Loom, deps, jar)
├── settings.gradle              plugin repos + project name
├── gradle.properties            version, MC/Fabric/dependency versions
├── gradlew / gradlew.bat        cross-platform build entry
├── gradle/wrapper/              pins Gradle 9.7.0; first build downloads it
├── LICENSE                      project license (GPL-3.0)
├── README.md                    Chinese README
├── README.en.md                 this file
└── src/
    ├── main/java/com/quietdome/         shared (both sides)
    │   ├── Alias.java                   one alias record
    │   ├── AliasRegistry.java           the list of 7 aliases
    │   ├── AliasDeriver.java            hostname -> hash -> index
    │   ├── AliasConfig.java             fallback seed (quiet_dome.json)
    │   ├── AliasManager.java            resolve once, cache it
    │   ├── DomeConfig.java              visual switches (quiet_dome_visual.json)
    │   └── DomeTextures.java            texture ID constants
    │
    ├── client/java/com/quietdome/client/  client only
    │   ├── QuietDomeClient.java         entry: log alias + extract video
    │   ├── VideoManager.java            find/extract video, resolution debounce
    │   ├── VideoPlayer.java             decode thread -> NativeImage -> GPU texture
    │   ├── LinearDynamicTexture.java    linear-filtered DynamicTexture (anti-blocky)
    │   ├── DomeBackground.java          menu backdrop: video + vignette + stars
    │   ├── DomeTransition.java          title intro (dive -> black -> white -> bloom)
    │   ├── DomeRender.java              draw helpers (cover/blit/circle/hash)
    │   ├── DomeVideoAdapter.java        backdrop layer (video first, still fallback)
    │   ├── EntranceAnim.java            black-curtain fade-in per screen
    │   ├── SelectionGlow.java           hover frame + particle erosion
    │   ├── Corruption.java              erosion accumulator (only while hovered)
    │   ├── Starfield.java               drifting background stars
    │   └── mixin/                       bytecode patches on vanilla screens
    │       ├── ScreenMixin.java         backdrop routing + entrance curtain
    │       ├── TitleScreenMixin.java    title intro transition
    │       ├── LoadingOverlayMixin.java loading screen: black hole + logo + bar
    │       ├── VideoSettingsScreenMixin.java  adds a resolution slider
    │       ├── OptionsSubScreenAccessor.java  reads the superclass list field
    │       ├── ViaConfigScreenMixin.java      ViaFabric screen reskin (untested)
    │       └── FabricModMixin.java            Mod Menu entry shows the alias
    │
    ├── main/resources/                  packed into the jar
    │   ├── fabric.mod.json              metadata, entrypoint, mixins, deps
    │   ├── assets/quiet_dome/
    │   │   ├── icon.png                 mod icon
    │   │   ├── font/saira_condensed.ttf font
    │   │   ├── lang/{en_us,zh_cn}.json  language files
    │   │   ├── textures/                stills (birdcage / blackhole / solivault)
    │   │   └── videos/heidong.mp4       black-hole video (the only video)
    │   └── licenses/                    third-party license texts
    │
    └── client/resources/                client-only mixin configs
        ├── quiet_dome.client.mixins.json
        ├── quiet_dome.options.mixins.json
        ├── quiet_dome.viafabric.mixins.json
        └── quiet_dome.modmenu.mixins.json
```

**Key points**

- `src/main` and `src/client` are split (Loom's `splitEnvironmentSourceSets`).
  Client-only code goes in `client`; shared code in `main`.
- The **only real copy** of the video is `assets/quiet_dome/videos/heidong.mp4`.
- All third-party licenses live in `licenses/` and ship inside the jar.

## Visual features

| Screen | Effect |
|---|---|
| Loading | Looping black-hole video + cyan progress bar + vanilla Mojang logo |
| Title | Looping black-hole video backdrop + vignette + starfield |
| Singleplayer / Multiplayer | same video backdrop |
| Title intro | procedural: dive -> black -> white -> bloom reveal |
| Menu buttons | hover frame + blue particle erosion (buttons slowly get smothered) |
| All menus | black-curtain fade-in on entry AND on return |
| Video Settings | extra "Quiet Dome Video Resolution" slider (applies live) |
| ViaFabric screen (untested) | dark panel + cyan frame + reflowed controls |

Effects use 26.2's `GuiGraphicsExtractor` render model.

---

## ⚠ Where to put the video (READ THIS)

The mod has **one** video: `heidong.mp4`, a black-hole loop used by the loading
screen and the menu backdrop. It ships **inside the jar** and is extracted to
disk on first launch.

### Replace the bundled video (developers, before building)

Put your file here — **this is the ONLY folder that gets packed into the jar**:

```
src/main/resources/assets/quiet_dome/videos/heidong.mp4
```

Then run `gradle build`; `processResources` picks it up automatically.

> Do NOT keep copies in the project root — they are not packed and only cause
> confusion / wasted space. The copy under `resources/assets/.../videos/` is the truth.

### Replace the video at runtime (players, no rebuild)

Drop your file here (it takes priority over the bundled one):

```
<gameDir>/config/quiet_dome/videos/heidong.mp4
```

On first launch the mod extracts its bundled copy into that folder; if a file is
already there, it is left untouched.

### Decoder parameters

| File | Source aspect | Notes |
|---|---|---|
| `heidong.mp4` | 16:9 | loading screen + menu backdrop, looped |

Decode width is adjustable in-game (Video Settings slider); height follows the
source aspect. Default 1280px wide, 15 fps.

The title intro (dive -> black -> white -> bloom) is **procedural**, not a video —
see `DomeTransition`.

---

## Configuration

`config/quiet_dome_visual.json` — visual switches and decode resolution:

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

`config/quiet_dome.json` — per-machine alias (see below).

## Alias (internal)

The mod derives an alias from the hostname (1 of 7; if the hostname is
unavailable it falls back to a persisted random seed). It is only used for the
Mod Menu entry title — no watermark is drawn on screen.

> The individual aliases are not listed here.

## Environment

- Minecraft 26.2 · Java 25 · Fabric Loader 0.19.x · Fabric API (26.2)
- Optional: Mod Menu, ViaFabric
- Bundled FFmpeg (via JavaCV, windows-x86_64 only, ~32 MB)

## Build

```
gradle build
```

Output: `build/libs/quiet_dome-Exptl-Alpha.jar`

## Licenses & Third-Party Notices

Quiet Dome itself is **GPL-3.0**. Full license texts for every bundled component
are in the jar's `licenses/` folder:

| Component | License | Notes |
|---|---|---|
| Quiet Dome | GPL-3.0 | this project |
| FFmpeg (via JavaCPP presets) | LGPL-3.0 | `org.bytedeco:ffmpeg:8.1.2-1.5.14` (no `-gpl` suffix = LGPL build); video decoding |
| JavaCPP JNI glue | Apache-2.0 | GPL-3.0 compatible |
| Saira Condensed font | OFL-1.1 | © 2016 The Saira Project Authors |
| `heidong.mp4` | see below | background video |

LGPL-3.0 and Apache-2.0 are both compatible with GPL-3.0 (the LGPLv3 text
explicitly incorporates the GPLv3 terms). The OFL font is carried alongside as a
data file, not linked into code, so it does not affect the mod's own licensing.

### Video provenance and usage limits

`heidong.mp4` comes from the Haowallpaper site (哲风壁纸):

> https://haowallpaper.com/homeViewLook/18890446631980416

The site's User Agreement states:

> ⚠ Commercial use prohibited: users are strictly forbidden from using site
> content for "commercial profit" or "disguised traffic/profit" purposes...
> For non-commercial, non-profit redistribution, the source must be credited as
> "哲风壁纸" (Haowallpaper)...
> If the original author has stated in their profile or post that redistribution
> is prohibited, it must not be redistributed...
> — haowallpaper.com User Agreement (effective 2023-10-20, revised 2025-09-12)

**What is certain:**

1. **No commercial use** — this mod must not be sold, subscription-gated, or used to drive traffic for profit in any form.
2. **Non-commercial redistribution must credit the source** — this document credits "哲风壁纸", satisfying that condition.

**What is uncertain:**

The agreement does not explicitly say whether **bundling the wallpaper into software** counts as the "redistribution" it permits. It only says non-commercial redistribution must credit the source, and that authors who forbid redistribution must be respected.

**Advice for users:**

- **Personal use**: no risk, just use it.
- **Public distribution** (uploading to a platform, sharing with others): replace it with your own asset, or obtain permission from Haowallpaper / the original author first.
- **Commercial use** (charging, driving traffic): **explicitly prohibited — do not do it.**

> The uploader of this wallpaper has not declared redistribution prohibited, so
> the site's default terms apply (non-commercial + credit). Whether "bundling
> into software" counts as the "redistribution" the agreement refers to is still
> unclear, so replacing it with your own asset before public release is advised.
> If this project infringes on anything, contact me for removal: tnt111h111@outlook.com

## License

GPL-3.0. See [LICENSE](LICENSE).
