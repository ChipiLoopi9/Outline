package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import com.chipiloopi.outline.SeeThroughLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the model a second time on a depth-test-free layer, so the entity's own
 * skin shows through walls rather than just its outline.
 *
 * <p>Injected immediately before the renderer pops the matrix stack, which is
 * the one point where the model is both posed for this frame and still under
 * the renderer's own transform. Injecting at TAIL instead renders the model
 * upside down and roughly a block and a half low, because entity models are
 * authored inverted and the 180 degree flip plus Y offset that corrects them
 * has already been unwound by the pop.
 *
 * <p>Reusing the renderer's transform rather than reconstructing it also avoids
 * having to guess what vanilla passes to setupTransforms, where a wrong
 * argument would silently misplace the model rather than fail to compile.
 *
 * <p>Targets are identified by {@code outlineColor} being set, which
 * {@link EntityRendererMixin} already fills in for exactly the configured
 * entities — so no separate bookkeeping is needed here.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
	/**
	 * Block light 15, sky light 15. The see-through pass deliberately ignores
	 * the entity's real light level: passing {@code state.light} multiplies the
	 * skin by whatever lighting it actually sits in, which renders a player in
	 * shadow — or behind a wall, where the sampled light is low — as a nearly
	 * black silhouette with only the brightest texels surviving. A highlight
	 * that disappears in the dark is useless, so this pass is always fully lit.
	 */
	private static final int OUTLINE_FULL_BRIGHT = 0x00F000F0;

	@Shadow
	protected M model;

	@Shadow
	protected abstract Identifier getTexture(S state);

	@Inject(
			method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0),
			require = 0
	)
	private void outline$renderSkinThroughWalls(S state, MatrixStack matrices, OrderedRenderCommandQueue queue,
			CameraRenderState cameraState, CallbackInfo ci) {
		if (!OutlineMod.shouldRenderThroughWalls() || state.outlineColor == 0) {
			return;
		}

		queue.getBatchingQueue(0).submitModel(
				this.model,
				state,
				matrices,
				SeeThroughLayers.get(this.getTexture(state)),
				OUTLINE_FULL_BRIGHT,
				OverlayTexture.DEFAULT_UV,
				// Tint, explicitly white. The shorter submitModel overload takes no
				// tint at all, which left it at zero and multiplied every texel to
				// black -- the model was drawing correctly, it was just being
				// coloured out of existence.
				0xFFFFFFFF,
				null,
				0,
				null);
	}
}
