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
 * <p>Injected at TAIL on purpose: by then the renderer has already posed the
 * model for this frame, so the second submission reuses that pose instead of
 * whatever was left over from the previous entity.
 *
 * <p>Targets are identified by {@code outlineColor} being set, which
 * {@link EntityRendererMixin} already fills in for exactly the configured
 * entities — so no separate bookkeeping is needed here.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
	@Shadow
	protected M model;

	@Shadow
	protected abstract Identifier getTexture(S state);

	@Inject(
			method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
			at = @At("TAIL"),
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
				state.light,
				OverlayTexture.DEFAULT_UV,
				0,
				null);
	}
}
