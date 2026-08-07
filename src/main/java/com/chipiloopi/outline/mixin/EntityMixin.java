package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla derives the outline color from the entity's team color. Overriding
 * it at the source keeps the configured colour consistent everywhere the pipeline reads
 * it (including the vanilla glowing path, e.g. when a target also has the
 * Glowing status effect).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
	private void outline$overrideTeamColor(CallbackInfoReturnable<Integer> cir) {
		if (OutlineMod.shouldOutline((Entity) (Object) this)) {
			cir.setReturnValue(OutlineMod.getOutlineColorRgb());
		}
	}

	/**
	 * Keeps outlined targets rendering far past the point vanilla drops them.
	 *
	 * <p>Vanilla's cutoff is {@code 64 * getRenderDistanceMultiplier()} blocks,
	 * scaled by the bounding box, so a player at default settings stops being
	 * drawn at roughly 64 blocks — and an entity that is not drawn is not
	 * submitted to the outline framebuffer either, so the outline and its glow
	 * both vanish at once. No amount of work in the shader chain can bring back
	 * something the renderer never submitted.
	 *
	 * <p>This only ever turns a "no" into a "yes", never the reverse, so nothing
	 * vanilla would have drawn gets culled by it. It also only applies to
	 * configured targets, which keeps the extra draws bounded by the number of
	 * players in range rather than by every entity loaded.
	 *
	 * <p>It cannot see through the server, though. Entities outside the server's
	 * tracking range — commonly 48 to 64 blocks, and not something a client can
	 * change — are never sent to the client at all, so there is nothing here to
	 * render at any distance. Raising this helps wherever the server tracks
	 * further than vanilla draws, which is the usual case on servers that raise
	 * their view distance.
	 */
	@Inject(method = "shouldRender(D)Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void outline$extendRenderDistance(double distanceSq, CallbackInfoReturnable<Boolean> cir) {
		double limit = OutlineMod.getRenderDistanceBlocks();
		if (limit > 0.0 && distanceSq < limit * limit && OutlineMod.shouldOutline((Entity) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
