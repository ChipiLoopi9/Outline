package com.chipiloopi.outline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config stored at {@code config/outline.json}. */
public class OutlineConfig {
	public enum Targets {
		/** Outline other players only (the reference look). */
		PLAYERS,
		/** Outline players and all mobs/animals. */
		LIVING,
		/** Outline every entity, including items, arrows, armor stands, ... */
		ALL
	}

	public enum SeeThroughStyle {
		/**
		 * The entity's real skin. Parts can still resolve in submission order
		 * against each other, since this pass reads the depth buffer but may not
		 * write to it.
		 */
		SKIN,
		/** A flat colour fill. No self-sorting artifacts, and includes armor. */
		SILHOUETTE
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * Bumped whenever the default color changes. The config file is written on
	 * first launch and never overwritten afterwards, so without this an old
	 * file keeps pinning the color to a previous default and no amount of
	 * changing {@link #DEFAULT_COLOR} would ever reach an existing install.
	 */
	static final int CURRENT_VERSION = 3;

	/**
	 * Deep purple. The look being matched is a violet glow from the reference
	 * screenshots, so hue is a product requirement here rather than a free
	 * choice: a highlight that reads as "not the reference colour" has failed
	 * regardless of how well it separates from the terrain palette.
	 */
	static final String DEFAULT_COLOR = "#6A1B9A";

	/**
	 * Must default to 0, not {@link #CURRENT_VERSION}. Gson instantiates this
	 * class with its no-arg constructor and then overwrites only the fields
	 * actually present in the JSON, so a config written before this field
	 * existed would otherwise come back already stamped as current and skip the
	 * migration below — permanently pinning those installs to the colour they
	 * were first written with.
	 */
	public int configVersion = 0;
	public boolean enabled = true;
	/** Outline color as "#RRGGBB". */
	public String color = DEFAULT_COLOR;
	public boolean outlineSelf = false;
	/** Draw the entity's skin through walls, not just its outline. */
	public boolean renderThroughWalls = true;
	/** Whether occluded entities show their skin or a flat fill. */
	public SeeThroughStyle seeThroughStyle = SeeThroughStyle.SKIN;
	public Targets targets = Targets.PLAYERS;

	private transient int cachedRgb = -1;

	public int getColorRgb() {
		if (cachedRgb < 0) {
			cachedRgb = parseColor(color);
		}
		return cachedRgb;
	}

	private static int parseColor(String hex) {
		try {
			String s = hex.trim();
			if (s.startsWith("#")) {
				s = s.substring(1);
			} else if (s.startsWith("0x") || s.startsWith("0X")) {
				s = s.substring(2);
			}
			// Require exactly RRGGBB. Without this, "#FFF" parses happily as
			// 0x000FFF — a dark blue — instead of being reported as the typo it
			// is, and the user is left wondering why their colour was ignored.
			if (s.length() != 6) {
				throw new NumberFormatException("expected 6 hex digits, got " + s.length());
			}
			return (int) (Long.parseLong(s, 16) & 0xFFFFFFL);
		} catch (Exception e) {
			OutlineMod.LOGGER.warn("Invalid outline color {}, using default {}", hex, DEFAULT_COLOR);
			return OutlineMod.DEFAULT_COLOR_RGB;
		}
	}

	/**
	 * Every colour that has ever shipped as {@link #DEFAULT_COLOR}. A version
	 * bump only rewrites the colour if the file still holds one of these, so a
	 * user who deliberately picked their own colour keeps it while everyone
	 * still on an old default gets moved forward.
	 */
	private static final String[] PREVIOUS_DEFAULTS = {
			"#AA00FF", // v1 — the very first build, and what most installs still hold
			"#9B30FF", // v2
			"#8A2BE2", // v3 — blueviolet
	};

	private static boolean isPreviousDefault(String hex) {
		if (hex == null) {
			return true;
		}
		String s = hex.trim();
		for (String old : PREVIOUS_DEFAULTS) {
			if (s.equalsIgnoreCase(old)) {
				return true;
			}
		}
		return s.equalsIgnoreCase(DEFAULT_COLOR);
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(OutlineMod.MOD_ID + ".json");
	}

	public static OutlineConfig load() {
		Path file = path();
		try {
			if (Files.exists(file)) {
				OutlineConfig cfg = GSON.fromJson(Files.readString(file), OutlineConfig.class);
				if (cfg != null) {
					if (cfg.color == null) {
						cfg.color = DEFAULT_COLOR;
					}
					if (cfg.seeThroughStyle == null) {
						cfg.seeThroughStyle = SeeThroughStyle.SKIN;
					}
					if (cfg.targets == null) {
						cfg.targets = Targets.PLAYERS;
					}
					if (cfg.configVersion < CURRENT_VERSION) {
						if (isPreviousDefault(cfg.color)) {
							OutlineMod.LOGGER.info("Updating outline color {} -> {} (config v{} -> v{})",
									cfg.color, DEFAULT_COLOR, cfg.configVersion, CURRENT_VERSION);
							cfg.color = DEFAULT_COLOR;
						} else {
							OutlineMod.LOGGER.info("Keeping custom outline color {} (config v{} -> v{})",
									cfg.color, cfg.configVersion, CURRENT_VERSION);
						}
						cfg.configVersion = CURRENT_VERSION;
						save(cfg);
					}
					return cfg;
				}
			}
		} catch (Exception e) {
			OutlineMod.LOGGER.warn("Could not read {}, using defaults", file, e);
		}
		OutlineConfig cfg = new OutlineConfig();
		// The field defaults to 0 so that a file *missing* the key is treated as
		// pre-versioning; a file we are writing fresh is by definition current,
		// and must say so or the next launch runs a pointless migration over it.
		cfg.configVersion = CURRENT_VERSION;
		save(cfg);
		return cfg;
	}

	public static void save(OutlineConfig cfg) {
		try {
			Files.createDirectories(path().getParent());
			Files.writeString(path(), GSON.toJson(cfg));
		} catch (Exception e) {
			OutlineMod.LOGGER.warn("Could not save outline config", e);
		}
	}
}
