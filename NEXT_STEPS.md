# Next: armor and elytra through walls

The see-through pass currently submits only the base entity model, so a player
behind a wall shows as a bare skin with no armor or elytra. This is the plan for
adding them.

## Scope, and what is deliberately excluded

Include **armor, elytra and capes**. Exclude **held items**.

That split is not arbitrary. Armor, elytra and capes are entity models: they use
the entity vertex format and the `core/entity` shader, which is exactly what
`SeeThroughLayers.PIPELINE` is built on. Held items go through `submitItem` with
item/block model formats, and enchantment glint uses its own shader and blend
mode. Submitting geometry in one vertex format to a pipeline expecting another
is not a cosmetic bug — it is garbage output or a GPU-level crash. Keeping items
on their normal layers removes that entire class of failure.

## Approach

Features are drawn by `FeatureRenderer`s in a loop after the model is submitted,
each choosing its own `RenderLayer`, so no extra argument to the existing
`submitModel` call can reach them. The entity has to be rendered a second time
with every submission redirected onto see-through layers.

1. **Accessors.** `RenderLayer.renderSetup` and `RenderSetup.textures` are both
   private; expose them with `@Accessor` mixins. `RenderSetup.resolveTextures()`
   may be usable directly and is worth checking first — it avoids one accessor.
2. **`SeeThroughLayers.wrap(RenderLayer)`.** Rebuild an incoming layer with its
   original textures but the no-depth pipeline. Cache by the source layer.
3. **Layer-substituting queue.** Wrap `OrderedRenderCommandQueue` so
   `submitModel` and `submitModelPart` swap in the wrapped layer, while
   `submitItem` passes through untouched.
4. **Second render pass.** Inject at HEAD of `LivingEntityRenderer.render` and
   re-enter with the wrapping queue.

## Hazards, in the order they will bite

- **Re-entrancy.** Step 4 calls `render()` from inside `render()`. The guard must
  be airtight or the client hard-freezes. A `ThreadLocal<Boolean>` checked and
  set before the nested call, cleared in a `finally`.
- **`@Accessor` failure is fatal.** Unlike the current inject, which carries
  `require = 0` and degrades to the feature silently not working, a broken
  accessor crashes the client at load. Verify the field names against the yarn
  1.21.11 mappings before running.
- **Cache growth.** One wrapped layer per distinct incoming layer — armor,
  trims, capes, glint variants — each holding GPU pipeline state. Bound it, or
  at minimum key it so variants collapse.
- **Draw call cost.** This doubles entity submissions.

## Ground truth

Signatures come from the yarn 1.21.11 mappings, not from memory:
`https://raw.githubusercontent.com/FabricMC/yarn/1.21.11/mappings/net/minecraft/...`.
That source is what made the working parts of this feature compile first try;
every part that was guessed instead needed several rounds to correct.

## State this builds on

`SeeThroughLayers` and `LivingEntityRendererMixin` work: skins render through
walls, right side up, in colour, not inside out. Do not disturb the pipeline's
`ALPHA_CUTOUT` / `BlendFunction.TRANSLUCENT` / `withCull(true)` /
`withDepthWrite(false)` combination — each of those fixed a specific, separately
diagnosed bug, and the reasoning is in the commit messages.
