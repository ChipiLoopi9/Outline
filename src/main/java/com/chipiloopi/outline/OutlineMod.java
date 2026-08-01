package com.chipiloopi.outline;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central state and decision logic for the outline effect.
 *
 * <p>The effect itself is vanilla's glowing-outline pipeline (the one used by
 * spectral arrows / the Glowing status effect): entities flagged for an outline
 * are drawn into the outline framebuffer and composited with a colored edge
 * that is visible through walls. This mod only forces that flag on for the
 * configured targets and recolors it purple.
 */
public final class OutlineMod {
	public static final String MOD_ID = "outline";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final int DEFAULT_COLOR_RGB = 0xAA00FF;

	private static OutlineConfig config = new OutlineConfig();

	private OutlineMod() {
	}

	public static OutlineConfig getConfig() {
		return config;
	}

	public static void setConfig(OutlineConfig newConfig) {
		config = newConfig;
	}

	public static boolean isEnabled() {
		return config.enabled;
	}

	public static void toggle() {
		config.enabled = !config.enabled;
		OutlineConfig.save(config);
	}

	/** Whether the given entity should get the purple outline. */
	public static boolean shouldOutline(Entity entity) {
		if (entity == null || !config.enabled) {
			return false;
		}
		if (!config.outlineSelf && entity == MinecraftClient.getInstance().player) {
			return false;
		}
		return switch (config.targets) {
			case PLAYERS -> entity instanceof PlayerEntity;
			case LIVING -> entity instanceof LivingEntity;
			case ALL -> true;
		};
	}

	/** Outline color as 0xRRGGBB, the format {@code Entity#getTeamColorValue} returns. */
	public static int getOutlineColorRgb() {
		return config.getColorRgb();
	}

	/** Outline color as opaque 0xAARRGGBB, the format stored in {@code EntityRenderState#outlineColor}. */
	public static int getOutlineColorArgb() {
		return 0xFF000000 | config.getColorRgb();
	}
}
