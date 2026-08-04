package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import com.chipiloopi.outline.SeeThroughLayers;
import com.chipiloopi.outline.SeeThroughPass;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mirrors every model submitted while an outlined entity renders onto the flat
 * silhouette layer, so armor, elytra and capes show through walls alongside the
 * body without this mod needing to know those feature renderers exist.
 *
 * <p>Held items are intentionally not mirrored. They arrive through submitItem
 * with item and glint layers that use different vertex formats and shaders, and
 * feeding those to an entity-format pipeline is a GPU-level failure rather than
 * a cosmetic one.
 */
@Mixin(BatchingRenderCommandQueue.class)
public abstract class BatchingRenderCommandQueueMixin {
	@Inject(
			method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
			at = @At("HEAD"),
			require = 0
	)
	private void outline$silhouette(Model model, Object state, MatrixStack matrices, RenderLayer layer,
			int light, int overlay, int outlineColor,
			ModelCommandRenderer.CrumblingOverlayCommand crumbling, CallbackInfo ci) {
		outline$duplicate(model, state, matrices, light, overlay);
	}

	@Inject(
			method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
			at = @At("HEAD"),
			require = 0
	)
	private void outline$silhouetteTinted(Model model, Object state, MatrixStack matrices, RenderLayer layer,
			int light, int overlay, int tintedColor, Sprite sprite, int outlineColor,
			ModelCommandRenderer.CrumblingOverlayCommand crumbling, CallbackInfo ci) {
		outline$duplicate(model, state, matrices, light, overlay);
	}

	private void outline$duplicate(Model model, Object state, MatrixStack matrices, int light, int overlay) {
		if (!OutlineMod.useSilhouette() || !SeeThroughPass.shouldDuplicate()) {
			return;
		}

		SeeThroughPass.enterDuplicate();
		try {
			((BatchingRenderCommandQueue) (Object) this).submitModel(
					model,
					state,
					matrices,
					SeeThroughLayers.get(),
					light,
					overlay,
					// The silhouette texture is white, so this tint is the colour.
					// Opaque on purpose: a translucent fill would double-blend
					// wherever two parts of the model overlap, giving an uneven
					// patchwork instead of one clean shape.
					OutlineMod.getOutlineColorArgb(),
					null,
					0,
					null);
		} finally {
			SeeThroughPass.exitDuplicate();
		}
	}
}
