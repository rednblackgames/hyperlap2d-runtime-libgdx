package games.rednblack.editor.renderer.input;

/**
 * How an entity takes part in input.
 */
public enum Touchable {
    /** The entity and its children receive events. */
    ENABLED,
    /** Neither the entity nor anything below it receives events: the whole branch is invisible to input. */
    DISABLED,
    /** The entity itself is never hit, but its children still are: a container that lets clicks through. */
    CHILDREN_ONLY
}
