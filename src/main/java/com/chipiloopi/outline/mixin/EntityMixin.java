package com.chipiloopi.outline.mixin;

import com.chipiloopi.outline.OutlineMod;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla derives the outline color from the entity's team color. Overriding
 * it at the source keeps the purple consistent everywhere the pipeline reads
 * it (including the vanilla glowing path, e.g. when a target also has the
 * Glowing status effect).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
	private void outline$purpleTeamColor(CallbackInfoReturnable<Integer> cir) {
		if (OutlineMod.shouldOutline((Entity) (Object) this)) {
			cir.setReturnValue(OutlineMod.getOutlineColorRgb());
		}
	}
}
