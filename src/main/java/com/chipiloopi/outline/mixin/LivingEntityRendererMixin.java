package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import com.chipiloopi.outline.SeeThroughLayers;
import com.chipiloopi.outline.SeeThroughPass;
import net.minecraft.client.model.Model;
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
 * Draws outlined entities through walls, in one of two styles.
 *
 * <p>SKIN submits the entity's real texture at the point the renderer is about
 * to pop its matrix stack — the one place the model is both posed for this frame
 * and still under the renderer's own transform. Injecting later would draw it
 * upside down and a block and a half low, because entity models are authored
 * inverted and that correction lives inside the push/pop.
 *
 * <p>SILHOUETTE instead flags the whole render window and lets
 * {@link BatchingRenderCommandQueueMixin} mirror every model submitted inside it
 * onto the flat layer. That catches armor, elytra and capes too, which a single
 * submission here cannot reach — they are drawn by separate feature renderers,
 * each choosing its own layer.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
	/**
	 * Block light 15, sky light 15. Required by submitModel but inert: the
	 * see-through pipelines define EMISSIVE, so the lightmap is never sampled.
	 */
	private static final int OUTLINE_FULL_BRIGHT = 0x00F000F0;

	private static final String RENDER = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;"
			+ "Lnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
			+ "Lnet/minecraft/client/render/state/CameraRenderState;)V";

	@Shadow
	protected M model;

	@Shadow
	protected abstract Identifier getTexture(S state);

	@Inject(method = RENDER, at = @At("HEAD"), require = 0)
	private void outline$beginSilhouette(S state, MatrixStack matrices, OrderedRenderCommandQueue queue,
			CameraRenderState cameraState, CallbackInfo ci) {
		if (OutlineMod.shouldRenderThroughWalls() && OutlineMod.useSilhouette() && state.outlineColor != 0) {
			SeeThroughPass.begin();
		}
	}

	@Inject(method = RENDER, at = @At("RETURN"), require = 0)
	private void outline$endSilhouette(S state, MatrixStack matrices, OrderedRenderCommandQueue queue,
			CameraRenderState cameraState, CallbackInfo ci) {
		SeeThroughPass.end();
	}

	@Inject(
			method = RENDER,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0),
			require = 0
	)
	private void outline$renderSkinThroughWalls(S state, MatrixStack matrices, OrderedRenderCommandQueue queue,
			CameraRenderState cameraState, CallbackInfo ci) {
		if (!OutlineMod.shouldRenderThroughWalls() || OutlineMod.useSilhouette() || state.outlineColor == 0) {
			return;
		}

		queue.getBatchingQueue(0).submitModel(
				(Model) this.model,
				state,
				matrices,
				SeeThroughLayers.skin(this.getTexture(state)),
				OUTLINE_FULL_BRIGHT,
				OverlayTexture.DEFAULT_UV,
				0xFFFFFFFF,
				null,
				0,
				null);
	}
}
