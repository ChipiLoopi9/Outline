package com.chipiloopi.outline;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

/**
 * A flat silhouette layer for the parts of an entity hidden behind terrain.
 *
 * <p>Earlier versions drew the real skin here, which never worked cleanly. With
 * depth testing switched off nothing sorts the model against itself, and the
 * two available settings both fail: culling off lets back faces draw over front
 * ones so the model reads inside out, and culling on removes the back faces of
 * two-sided geometry so arms and capes turn transparent from the side. Correct
 * sorting needs a depth buffer, which is precisely what that approach threw
 * away.
 *
 * <p>A single flat colour sidesteps the whole problem: overlapping parts of one
 * uniform colour are indistinguishable, so there is nothing left to sort. It is
 * also what the reference this mod is modelled on actually shows — the occluded
 * body reads as a filled shape, not a skin.
 */
public final class SeeThroughLayers {
	/** 2x2 opaque white, so the tint alone decides the colour. */
	private static final Identifier WHITE = Identifier.of(OutlineMod.MOD_ID, "textures/silhouette.png");

	/**
	 * GREATER_DEPTH_TEST is the load-bearing choice. It draws only where the
	 * model is <em>behind</em> what is already in the depth buffer, so the
	 * silhouette appears through walls and nowhere else. A visible player is
	 * left completely untouched — no need to order this pass against the real
	 * model, because the two can never cover the same pixel.
	 */
	private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
			.withLocation("pipeline/outline_silhouette")
			.withDepthTestFunction(DepthTestFunction.GREATER_DEPTH_TEST)
			// Never write depth: this pass is reading occlusion, not defining it.
			// Writing would stamp the model's far depth over the nearer wall and
			// let later depth-tested passes draw in front of that wall.
			.withDepthWrite(false)
			// Cull is irrelevant to a flat colour, and leaving it off keeps
			// two-sided geometry like capes intact.
			.withCull(false)
			// Skips the directional shading that would otherwise darken faces by
			// up to ~60% and break the flat fill this depends on.
			.withShaderDefine("NO_CARDINAL_LIGHTING")
			.build();

	private static final RenderLayer LAYER = RenderLayer.of(
			"outline_silhouette",
			RenderSetup.builder(PIPELINE).texture("Sampler0", WHITE).build());

	private SeeThroughLayers() {
	}

	public static RenderLayer get() {
		return LAYER;
	}
}
