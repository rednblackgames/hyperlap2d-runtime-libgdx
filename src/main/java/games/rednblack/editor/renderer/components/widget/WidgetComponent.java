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
    public Array<String> states = new Array<>(true, 4, String[]::new);

    /** The state whose look is the base (un-overridden) look of the parts. */
    public String defaultState = "normal";

    /** child state -> the state it builds on: showing the child applies the parent's look first. */
    public ObjectMap<String, String> parents = new ObjectMap<>(0);

    /** Widget type specific settings (e.g. min/max of a progress bar), stored as strings. */
    public ObjectMap<String, String> properties = new ObjectMap<>(0);

    /** Live state, not serialized: a loaded widget always starts in {@link #defaultState}. */
    public transient String currentState = null;

    /** What a setting was last read as, beside the very string it was read from. */
    private static class Reading {
        String source;
        float value;
    }

    /** One entry per setting ever read as a number, made the first time it is asked for. */
    private final transient ObjectMap<String, Reading> readings = new ObjectMap<>(0);

    /**
     * A setting read as a number, parsed once and kept until it is written again: the systems ask
     * for these every frame, and {@link Float#parseFloat} builds a converter of its own every call.
     *
     * @param fallback what an absent or unreadable setting means
     */
    public float number(String key, float fallback) {
        String raw = properties.get(key);
        if (raw == null) return fallback;

        Reading reading = readings.get(key);
        if (reading == null) {
            reading = new Reading();
            readings.put(key, reading);
        } else if (reading.source == raw) {
            //the map hands back the very same instance until somebody writes the property again
            return reading.value;
        }

        reading.source = raw;
        reading.value = parse(raw, fallback);
        return reading.value;
    }

    /** @return the number written in the text, the fallback if there is none to be read */
    public static float parse(String value, float fallback) {
        if (value == null) return fallback;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

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

    /** @return the state this one builds on, null if it stands on its own */
    public String parentOf(String state) {
        return state == null ? null : parents.get(state);
    }

    public boolean hasState(String state) {
        return state != null && states.contains(state, false);
    }

    @Override
    public void reset() {
        widgetType = "";
        states.clear();
        defaultState = "normal";
        parents.clear();
        properties.clear();
        currentState = null;
        readings.clear();
    }
}
