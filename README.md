# Outline

A client-side **Fabric** mod for Minecraft **1.21.11** that puts a glowing purple
outline on players — a crisp opaque line hugging the model plus a tight soft rim of
light around it, **visible through walls**.

## Features

- Crisp outline **with a real rim glow**, not just vanilla's thin line
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
  "color": "#6A1B9A",
  "outlineSelf": false,
  "targets": "PLAYERS"
}
```

- `color` — outline color as `#RRGGBB`
- `outlineSelf` — also outline your own player (visible in third person)
- `targets` — `PLAYERS`, `LIVING` (players + mobs), or `ALL` (every entity)

Edit while the game is closed. The **O** key flips `enabled` and saves it.

### Choosing a color

The default `#6A1B9A` is a deep purple, chosen to match the violet glow in the
reference screenshots this mod is modelled on. Hue is a product requirement
here, not a free choice.

If you prefer a different colour, set `color` in `config/outline.json`. A
version bump only rewrites the colour when the file still holds one of the
previously shipped defaults, so a colour you picked yourself is preserved.


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
| 1 | `outline:post/outline_edge` | edge-detect, preserving the exact colour → `sharp` |
| 2–3 | `outline:post/outline_core_blur` | 2px blur, high gain → `core` (the crisp line) |
| 4–5 | `outline:post/outline_glow_blur` | Gaussian, radius 9, low gain → `glow_v` (the rim) |
| 6 | `outline:post/outline_glow_combine` | core *over* rim → `minecraft:entity_outline` |

The core and the rim are separate shaders on purpose. They share a kernel but want
opposite gains: the core has to come out essentially opaque so it reads as a drawn line,
the rim has to stay translucent so it reads as light. A single shared gain cannot do
both.

### Tuning the look

Edit these and rebuild:

- **Rim width** — `Radius` (currently `9.0`) in the two `outline_glow_blur` passes of
  `assets/minecraft/post_effect/entity_outline.json`. Raise for a bigger bloom, but see
  the warning below.
- **Rim intensity** — `GLOW_STRENGTH` in
  `assets/outline/shaders/post/outline_glow_combine.fsh`.
- **Outer falloff** — `GLOW_GAMMA`; above `1.0` keeps the haze tight to the line, below
  `1.0` flattens it into a thick slab.
- **Line solidity** — `CORE_STRENGTH`, and `GAIN` in `outline_core_blur.fsh`.
- **Core brightness** — `CORE_WHITEN`; `0.0` is the pure outline color, higher is whiter.
  Past ~`0.3` the line stops carrying the configured hue at all.

> **Why the radius is 9 and not 26.** The blur radius is in *screen pixels* and does not
> shrink with distance, so it has to stay small relative to how big a player is on
> screen. Once the radius approaches half the silhouette width, the blurred edges from
> opposite sides of the entity land inside each other's kernel and sum, and the entity
> fills in solid. At radius 26 a 20px-wide distant player reached **0.75 alpha at its own
> centre** — a featureless blob with no readable shape. At radius 9 the same player
> reaches 0.03. Raising the radius past ~12 brings the blobbing back.

### If the glow doesn't appear

Mod resources normally override vanilla assets, but if another resource pack wins, the
same files are shipped as a standalone pack in `resourcepack/`. Zip its contents (so
`pack.mcmeta` is at the zip root), drop it in `resourcepacks/`, and move it to the top of
the enabled list.

> Note: seeing players through walls counts as an unfair advantage on many servers — use
> it where the rules allow.
