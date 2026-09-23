package games.rednblack.editor.renderer.input;

/**
 * Listens to the input reaching an entity, added to its {@link games.rednblack.editor.renderer.components.additional.InputTargetComponent}.
 *
 * Every method is given the event, whose {@code listenerEntity} is the entity this listener sits on
 * and whose {@code localX}/{@code localY} are the pointer in that entity's own coordinates. Acting
 * on an event means calling {@link UIEvent#handle()}: nothing further up the chain sees it, and a
 * handled touch down hands this listener the pointer until it is released, wherever it travels.
 */
public interface UIInputListener {

    default void touchDown(UIEvent event) {
    }

    /** Only reaches the listener that took the pointer on touch down. */
    default void touchDragged(UIEvent event) {
    }

    /** Only reaches the listener that took the pointer on touch down. */
    default void touchUp(UIEvent event) {
    }

    default void mouseMoved(UIEvent event) {
    }

    /** The pointer moved onto the entity, or onto something inside it. */
    default void enter(UIEvent event) {
    }

    /** The pointer left the entity and everything inside it. */
    default void exit(UIEvent event) {
    }

    default void scrolled(UIEvent event) {
    }

    /** Keys only reach the entity holding the keyboard focus, and the entities it sits in. */
    default void keyDown(UIEvent event) {
    }

    default void keyUp(UIEvent event) {
    }

    default void keyTyped(UIEvent event) {
    }
}
