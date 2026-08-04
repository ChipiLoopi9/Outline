package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import com.chipiloopi.outline.SeeThroughPass;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens the silhouette window around an outlined entity's render.
 *
 * <p>Nothing is submitted here. Flagging the whole call and letting
 * {@link BatchingRenderCommandQueueMixin} mirror each model submission means
 * the base model and every feature renderer — armor, elytra, capes — are all
 * caught, without this mixin needing to reproduce the renderer's transforms or
 * know which features are attached.
 *
 * <p>Targets are identified by {@code outlineColor} being set, which
 * {@link EntityRendererMixin} already fills in for exactly the configured
 * entities.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState> {
	private static final String RENDER = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;"
			+ "Lnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
			+ "Lnet/minecraft/client/render/state/CameraRenderState;)V";

	@Inject(method = RENDER, at = @At("HEAD"), require = 0)
	private void outline$beginSilhouette(S state, MatrixStack matrices, OrderedRenderCommandQueue queue,
			CameraRenderState cameraState, CallbackInfo ci) {
		if (OutlineMod.shouldRenderThroughWalls() && state.outlineColor != 0) {
			SeeThroughPass.begin();
		}
	}

	@Inject(method = RENDER, at = @At("RETURN"), require = 0)
	private void outline$endSilhouette(S state, MatrixStack matrices, OrderedRenderCommandQueue queue,
			CameraRenderState cameraState, CallbackInfo ci) {
		SeeThroughPass.end();
	}
}
