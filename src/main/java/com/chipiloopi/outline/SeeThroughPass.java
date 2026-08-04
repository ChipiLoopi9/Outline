package com.chipiloopi.outline;

/**
 * Marks the window during which an outlined entity is being rendered, so model
 * submissions made inside it can be mirrored onto the silhouette layer.
 *
 * <p>Armor, elytra and capes are drawn by separate feature renderers after the
 * base model, each picking its own render layer, so no argument to a single
 * submit call can reach them. Flagging the whole render and duplicating every
 * model submitted inside it catches all of them without the mod needing to know
 * which features exist.
 *
 * <p>Rendering is single threaded, so a plain static flag is enough — and being
 * plain makes the reentrancy guard easy to reason about, which matters because
 * the duplicate submission re-enters the very method that produced it.
 */
public final class SeeThroughPass {
	private static boolean active;
	private static boolean duplicating;

	private SeeThroughPass() {
	}

	public static void begin() {
		active = true;
	}

	public static void end() {
		active = false;
	}

	/** True while an outlined entity is rendering and we are not already inside a duplicate. */
	public static boolean shouldDuplicate() {
		return active && !duplicating;
	}

	public static void enterDuplicate() {
		duplicating = true;
	}

	public static void exitDuplicate() {
		duplicating = false;
	}
}
