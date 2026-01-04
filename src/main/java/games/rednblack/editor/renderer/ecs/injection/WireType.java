package games.rednblack.editor.renderer.ecs.injection;

import games.rednblack.editor.renderer.ecs.annotations.SkipWire;
import games.rednblack.editor.renderer.ecs.annotations.Wire;

/**
 * <p>Enum used by {@link ClassType} to indicate if a class or field is annotated
 * with {@link Wire}.</p>
 *
 * @author Snorre E. Brekke
 */
public enum WireType {
	/**
	 * Indicates that a class is (implicitly or explicitly) annotated
	 * with {@link Wire}.
	 */
	WIRE,
	/**
	 * Indicates that a class is not annotated with anything relevant to the artemis-engine.
	 */
	IGNORED,
	/**
	 * Indicates that a class is annotated with {@link SkipWire}
	 */
	SKIPWIRE
}
