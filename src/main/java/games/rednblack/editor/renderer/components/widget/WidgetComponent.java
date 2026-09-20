package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.ecs.PooledComponent;

/**
 * Marks an entity (normally a composite) as a GUI widget and holds its state machine.
 *
 * The widget only owns <i>which</i> state is current; what a state looks like is described by the
 * {@link WidgetPartComponent}s of its descendants, applied by
 * {@link games.rednblack.editor.renderer.systems.WidgetStateSystem}.
 *
 * {@link #currentState} may be set by a widget behaviour system (e.g. the button one) or directly
 * from game code / scripts through {@link #setState(String)}.
 */
public class WidgetComponent extends PooledComponent {

    /** Key of the {@link games.rednblack.editor.renderer.widget.WidgetType} descriptor, empty for a plain state holder. */
    public String widgetType = "";

    /** Declared states, the first one usually being {@link #defaultState}. */
    public Array<String> states = new Array<>(true, 4, String.class);

    /** The state whose look is the base (un-overridden) look of the parts. */
    public String defaultState = "normal";

    /** Widget type specific settings (e.g. min/max of a progress bar), stored as strings. */
    public ObjectMap<String, String> properties = new ObjectMap<>(0);

    /** Live state, not serialized: a loaded widget always starts in {@link #defaultState}. */
    public transient String currentState = null;

    public String getState() {
        return currentState != null ? currentState : defaultState;
    }

    /**
     * @return true if the state is declared and has been set, false if the widget doesn't declare it
     */
    public boolean setState(String state) {
        if (!hasState(state)) return false;
        currentState = state;
        return true;
    }

    public boolean hasState(String state) {
        return state != null && states.contains(state, false);
    }

    @Override
    public void reset() {
        widgetType = "";
        states.clear();
        defaultState = "normal";
        properties.clear();
        currentState = null;
    }
}
