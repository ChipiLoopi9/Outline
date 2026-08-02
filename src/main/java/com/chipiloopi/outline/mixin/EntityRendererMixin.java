package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Primary hook. Since the 1.21.9 render-state refactor, whether an entity is
 * drawn into the outline (glow) framebuffer — and with which color — is carried
 * by {@link EntityRenderState#outlineColor}, filled in during state extraction.
 * Writing a color at the end of {@code updateRenderState} both enables the
 * outline and applies the configured tint, walls or no walls.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(
			method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
			at = @At("TAIL")
	)
	private void outline$forceOutlineColor(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
		if (OutlineMod.shouldOutline(entity)) {
			state.outlineColor = OutlineMod.getOutlineColorArgb();
		}
	}
}
