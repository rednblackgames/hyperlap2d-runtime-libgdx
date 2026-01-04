package games.rednblack.editor.renderer.ecs.injection;

import games.rednblack.editor.renderer.ecs.BaseSystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.Engine;

/**
 * Enum used to cache class type according to their usage in Artemis.
 *
 * @author Snorre E. Brekke
 */
public enum ClassType {
	/**
	 * Used for (sub)classes of {@link ComponentMapper}
	 */
	MAPPER,
	/**
	 * Used for (sub)classes of {@link BaseSystem}
	 */
	SYSTEM,
	/**
	 * Used for (sub)classes of {@link Engine}
	 */
	ENGINE,
	/**
	 * Used for everything else.
	 */
	CUSTOM
}
