# Outline

A client-side **Fabric** mod for Minecraft **1.21.11** that draws a bright purple glow
outline around players — **visible through walls** — using the vanilla glowing-outline
renderer (the same pipeline as spectral arrows / the Glowing effect), so it looks exactly
like the vanilla glow, just always on and purple.

- In direct view: a crisp purple outline hugging the player model.
- Behind blocks: the full purple silhouette outline shows through the wall.

## Features

- Purple outline on all players by default (color/targets configurable)
- Works through walls — vanilla outline framebuffer, no custom shaders
- Toggle in-game with **O** (rebindable in Controls → Miscellaneous)
- Tiny, client-side only — nothing to install on the server

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
  "color": "#AA00FF",
  "outlineSelf": false,
  "targets": "PLAYERS"
}
```

- `color` — outline color as `#RRGGBB`
- `outlineSelf` — also outline your own player (visible in third person)
- `targets` — `PLAYERS`, `LIVING` (players + mobs), or `ALL` (every entity)

Edit while the game is closed; the file is re-read on launch. The **O** key flips
`enabled` and saves it.

## How it works

Three small mixins:

1. `EntityRenderer#updateRenderState` — writes the purple color into
   `EntityRenderState#outlineColor`, which since the 1.21.9 render refactor is what flags
   an entity for the outline framebuffer and tints its outline.
2. `Entity#getTeamColorValue` — recolors any other path that derives outline color from
   team color.
3. `MinecraftClient#hasOutline` — forces vanilla's "should glow" check on for targets
   (optional reinforcement injection).

> Note: seeing players through walls can count as an unfair advantage on some servers —
> use it where the rules allow.
