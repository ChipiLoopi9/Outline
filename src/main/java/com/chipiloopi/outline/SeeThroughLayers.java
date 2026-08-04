package com.chipiloopi.outline;

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
	/** Built from the vanilla entity snippet so vertex format and shader match. */
	private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
			.withLocation("pipeline/outline_see_through")
			// ENTITY_SNIPPET is a partial configuration: vanilla's entity pipelines
			// attach the shader programs themselves on top of it. Without these the
			// pipeline has no shader, and core/entity is what actually samples
			// Sampler0 and applies ColorModulator and the lightmap -- omit it and
			// every model draws solid black no matter how it is tinted or lit.
			.withVertexShader("core/entity")
			.withFragmentShader("core/entity")
			// core/entity ends with `color *= lightMapColor`, so if the lightmap
			// sampler is not bound for this pass the whole model multiplies to
			// black — which is what kept happening no matter what tint or light
			// value was submitted. EMISSIVE compiles that multiply out entirely,
			// removing the dependency rather than trying to satisfy it, and full
			// brightness is what a see-through highlight wants anyway.
			.withShaderDefine("EMISSIVE")
			// Likewise skips the overlay mix, which needs its own sampler.
			.withShaderDefine("NO_OVERLAY")
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
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
						.useLightmap()
						.useOverlay()
						.build()));
	}
}
