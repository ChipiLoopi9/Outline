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

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public boolean enabled = true;
	/** Outline color as "#RRGGBB". */
	public String color = "#AA00FF";
	public boolean outlineSelf = false;
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
			return (int) (Long.parseLong(s, 16) & 0xFFFFFFL);
		} catch (Exception e) {
			OutlineMod.LOGGER.warn("Invalid outline color '{}', using default #AA00FF", hex);
			return OutlineMod.DEFAULT_COLOR_RGB;
		}
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
						cfg.color = "#AA00FF";
					}
					if (cfg.targets == null) {
						cfg.targets = Targets.PLAYERS;
					}
					return cfg;
				}
			}
		} catch (Exception e) {
			OutlineMod.LOGGER.warn("Could not read {}, using defaults", file, e);
		}
		OutlineConfig cfg = new OutlineConfig();
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
