package com.chipiloopi.outline;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class OutlineClient implements ClientModInitializer {
	private static KeyBinding toggleKey;

	@Override
	public void onInitializeClient() {
		OutlineMod.setConfig(OutlineConfig.load());

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.outline.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				OutlineMod.toggle();
				boolean on = OutlineMod.isEnabled();
				client.inGameHud.setOverlayMessage(
						Text.literal("Outline: " + (on ? "ON" : "OFF"))
								.formatted(on ? Formatting.AQUA : Formatting.GRAY),
						false);
			}
		});

		OutlineMod.LOGGER.info("Outline loaded — outlines {} (toggle with O)",
				OutlineMod.isEnabled() ? "enabled" : "disabled");
	}
}
