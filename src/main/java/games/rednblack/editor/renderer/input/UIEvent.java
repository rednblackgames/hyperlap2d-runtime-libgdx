package games.rednblack.editor.renderer.input;

import com.badlogic.gdx.utils.Pool;

/**
 * One piece of input on its way to an entity. The same event travels the whole chain from the root
 * down to the target and back up, so {@link #listenerEntity} says who is being called right now,
 * while {@link #target} stays the entity the event is really aimed at.
 *
 * A listener that acts on an event calls {@link #handle()}: the chain stops there, the pointer of a
 * touch down belongs to that listener until it is released, and the game below the UI is told the
 * event was taken.
 */
public class UIEvent implements Pool.Poolable {

    public enum Type {
        touchDown, touchUp, touchDragged, mouseMoved, enter, exit, scrolled, keyDown, keyUp, keyTyped
    }

    public Type type;
    /** Entity the event is aimed at: the one under the pointer, or the one holding the keyboard focus. */
    public int target = -1;
    /** Entity whose listener is being called, somewhere between the target and the root. */
    public int listenerEntity = -1;
    /** enter and exit only: the entity the pointer came from, or is going to. */
    public int relatedEntity = -1;

    public int pointer = -1;
    /** Mouse button of a touch down or up, -1 when it has none. */
    public int button = -1;
    public int keyCode = -1;
    public char character;

    public float screenX, screenY;
    /** Where the pointer is in the coordinates of the scene the target belongs to. */
    public float sceneX, sceneY;
    /** Where the pointer is in the coordinates of {@link #listenerEntity}. */
    public float localX, localY;
    public float amountX, amountY;

    /** True while the event is going down the chain, before it reaches the target. */
    public boolean capture;
    /** True for a release the system itself made up, because the pointer was taken away. */
    public boolean cancelled;

    private boolean handled;

    /** Takes the event: nobody further along the chain sees it. */
    public void handle() {
        handled = true;
    }

    public boolean isHandled() {
        return handled;
    }

    @Override
    public void reset() {
        type = null;
        target = -1;
        listenerEntity = -1;
        relatedEntity = -1;
        pointer = -1;
        button = -1;
        keyCode = -1;
        character = 0;
        screenX = screenY = 0;
        sceneX = sceneY = 0;
        localX = localY = 0;
        amountX = amountY = 0;
        capture = false;
        cancelled = false;
        handled = false;
    }
}
