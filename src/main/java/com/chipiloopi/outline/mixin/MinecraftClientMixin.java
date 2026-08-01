package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reinforcement hook ({@code require = 0}: skipped silently if this method is
 * ever removed). {@code hasOutline} is vanilla's "should this entity glow"
 * check (Glowing effect / spectator outlines); forcing it on covers any code
 * path that consults it directly instead of the extracted render state.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true, require = 0)
	private void outline$forceHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (OutlineMod.shouldOutline(entity)) {
			cir.setReturnValue(true);
		}
	}
}
