# Outline

A client-side **Fabric** mod for Minecraft **1.21.11** that puts a bright purple glow
outline on players — a crisp lavender line hugging the model plus a wide soft purple
bloom around it, **visible through walls**.

## Features

- Purple outline **with a real glow**, not just vanilla's thin line
- Works through walls — built on the vanilla entity-outline framebuffer, no shader pack needed
- Toggle in-game with **O** (rebindable in Controls → Miscellaneous)
- Client-side only — nothing to install on the server

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.18.0 |
| Fabric API | 0.141.4+1.21.11 (any 1.21.11 build) |
| Java | 21 |

## Building

```bash
./gradlew build
```

The jar ends up in `build/libs/outline-1.0.0.jar`. Drop it (plus Fabric API) into your
`mods` folder.

## Configuration

`config/outline.json` is created on first launch:

```json
{
  "enabled": true,
  "color": "#9B30FF",
  "outlineSelf": false,
  "targets": "PLAYERS"
}
```

- `color` — outline color as `#RRGGBB`
- `outlineSelf` — also outline your own player (visible in third person)
- `targets` — `PLAYERS`, `LIVING` (players + mobs), or `ALL` (every entity)

Edit while the game is closed. The **O** key flips `enabled` and saves it.

## How it works

### Forcing the outline on (Java)

Three mixins put the targets into vanilla's outline framebuffer and set their color:

1. `EntityRenderer#updateRenderState` — writes the color into
   `EntityRenderState#outlineColor`, which since the 1.21.9 render refactor is what flags
   an entity for the outline pass and tints it.
2. `Entity#getTeamColorValue` — recolors any path deriving outline color from team color.
3. `MinecraftClient#hasOutline` — forces vanilla's "should glow" check on for targets.

### Producing the glow (resources)

Vanilla can't render a wide glow, and not because of a setting. Its
`entity_outline_box_blur.fsh` declares a `Radius` uniform and then shadows it:

```glsl
layout(std140) uniform BlurConfig { vec2 BlurDir; float Radius; };
...
    float radius = 2.0;   // hardcoded — the uniform is ignored
```

So the vanilla halo is permanently clamped to two pixels, which is the thin line you get
from a plain glowing effect. This mod overrides
`assets/minecraft/post_effect/entity_outline.json` with a longer chain:

| Pass | Shader | Purpose |
|---|---|---|
| 1 | `minecraft:post/entity_sobel` | edge-detect the silhouette → `sharp` |
| 2–3 | `minecraft:post/entity_outline_box_blur` | vanilla 2px blur → `core` (the crisp line) |
| 4–5 | `outline:post/outline_glow_blur` | wide Gaussian, radius 20 → `glow_v` (the halo) |
| 6 | `outline:post/outline_glow_combine` | core over halo → `minecraft:entity_outline` |

The core path is byte-identical to vanilla, so the crisp line is unchanged; the glow is
added around it.

### Tuning the look

Edit these and rebuild:

- **Glow width** — `Radius` (currently `20.0`) in the two `outline_glow_blur` passes of
  `assets/minecraft/post_effect/entity_outline.json`. Raise for a bigger bloom.
- **Glow intensity** — `GLOW_STRENGTH` in
  `assets/outline/shaders/post/outline_glow_combine.fsh`.
- **Outer falloff** — `GLOW_GAMMA`; below `1.0` lifts the faint outer edge.
- **Core brightness** — `CORE_WHITEN`; `0.0` is the pure outline color, higher is whiter.

### If the glow doesn't appear

Mod resources normally override vanilla assets, but if another resource pack wins, the
same files are shipped as a standalone pack in `resourcepack/`. Zip its contents (so
`pack.mcmeta` is at the zip root), drop it in `resourcepacks/`, and move it to the top of
the enabled list.

> Note: seeing players through walls counts as an unfair advantage on many servers — use
> it where the rules allow.
