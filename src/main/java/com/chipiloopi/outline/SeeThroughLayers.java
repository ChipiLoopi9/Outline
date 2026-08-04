package com.chipiloopi.outline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Render layers that ignore the depth buffer, so the entity's own skin is drawn
 * over whatever is in front of it.
 *
 * <p>The outline post-effect chain cannot do this: everything it receives is a
 * flat-coloured silhouette in the outline framebuffer, with no texture and no
 * lighting. Showing the actual skin through a wall means drawing the model a
 * second time with depth testing switched off, which is a pipeline change
 * rather than a shader one.
 */
public final class SeeThroughLayers {
	/**
	 * Built on the emissive entity snippet, which is core/entity with EMISSIVE
	 * already defined and only Sampler0 declared — the lightmap sampler is
	 * deliberately dropped, because EMISSIVE removes that uniform from the
	 * shader entirely.
	 *
	 * <p>The settings below are not decoration. A player model always draws a
	 * second, inflated skin layer — hat, jacket, sleeves, pants — over the whole
	 * body, and on most skins those texels are fully transparent. Without
	 * ALPHA_CUTOUT they are never discarded, and with no blend function their
	 * alpha is ignored, so RGB 0 is written straight to the colour attachment:
	 * an opaque black shell over a perfectly good body. That, not the tint, the
	 * light value or the shader, is what made these models render black.
	 */
	private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
			.withLocation("pipeline/outline_see_through")
			// Discard the transparent texels of the outer skin layer.
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			// Respect alpha for what survives the cutout.
			.withBlend(BlendFunction.TRANSLUCENT)
			// Matches the layers this replaces: entity models are two-sided, and
			// culling their back faces punches holes in capes and the outer layer.
			.withCull(false)
			// EMISSIVE skips the lightmap; this skips the directional shading that
			// would otherwise darken faces by up to ~60% depending on their normal.
			.withShaderDefine("NO_CARDINAL_LIGHTING")
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			// Ignoring depth is the point; writing it is not. Without this the
			// occluded model stamps its own far depth over the nearer wall, and
			// every later depth-tested pass — particles, weather, clouds,
			// translucents — then draws in front of that wall.
			.withDepthWrite(false)
			.build();

	/** One layer per skin; entity renderers ask for these every frame. */
	private static final Map<Identifier, RenderLayer> CACHE = new ConcurrentHashMap<>();

	private SeeThroughLayers() {
	}

	public static RenderLayer get(Identifier texture) {
		return CACHE.computeIfAbsent(texture, id -> RenderLayer.of(
				"outline_see_through",
				RenderSetup.builder(PIPELINE)
						.texture("Sampler0", id)
						.build()));
	}
}
