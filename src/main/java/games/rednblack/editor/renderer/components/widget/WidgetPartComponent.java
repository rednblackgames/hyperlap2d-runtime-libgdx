package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.utils.InterpolationMap;
import games.rednblack.editor.renderer.widget.StateTween;
import games.rednblack.editor.renderer.ecs.PooledComponent;

/**
 * Attached to any descendant of a widget that takes part in it: either because it fills a named
 * role of the widget type (e.g. the "knob" of a slider) or because it looks different in some of
 * the widget states, or both.
 *
 * Overrides are sparse: per state, only the properties that differ from the base look are listed,
 * as {@code property key -> value}. Keys are resolved by the handlers registered in
 * {@link games.rednblack.editor.renderer.systems.WidgetStateSystem}.
 */
public class WidgetPartComponent extends PooledComponent {

    /** Role declared by the widget type, empty when the part only carries state overrides. */
    public String role = "";

    /** state name -> (property key -> value) */
    public ObjectMap<String, ObjectMap<String, String>> overrides = new ObjectMap<>(0);

    /**
     * state name -> (property key -> how to get to the overridden value). A property with no entry
     * changes instantly. On the way back to the base look, the transition of the state being left
     * is used.
     */
    public ObjectMap<String, ObjectMap<String, Transition>> transitions = new ObjectMap<>(0);

    public static class Transition {
        public float duration;
        /** Name of a libGDX interpolation function, see {@link InterpolationMap}. */
        public String interpolation;

        private transient Interpolation resolved;

        public Interpolation getInterpolation() {
            if (resolved == null) {
                resolved = InterpolationMap.map.get(interpolation);
                if (resolved == null) resolved = Interpolation.linear;
            }
            return resolved;
        }
    }

    /** State currently applied to this entity, null while showing the base look. */
    public transient String appliedState = null;

    /** Base values of the properties the applied state has overridden, used to restore them. */
    public transient final ObjectMap<String, String> baseSnapshot = new ObjectMap<>(0);

    /** Set to force the system to re-apply the current state (e.g. after overrides were edited). */
    public transient boolean dirty = false;

    /** Transitions in progress, owned and recycled by the state system. */
    public transient final Array<StateTween> tweens = new Array<>(false, 2);

    /** The next time the state is applied it is not animated (the look was only taken off to be read). */
    public transient boolean snapNext = false;

    public ObjectMap<String, String> getOverrides(String state) {
        return state == null ? null : overrides.get(state);
    }

    public void setOverride(String state, String key, String value) {
        ObjectMap<String, String> patch = overrides.get(state);
        if (patch == null) {
            patch = new ObjectMap<>(2);
            overrides.put(state, patch);
        }
        patch.put(key, value);
        dirty = true;
    }

    public void removeOverride(String state, String key) {
        ObjectMap<String, String> patch = overrides.get(state);
        if (patch == null) return;
        patch.remove(key);
        if (patch.size == 0) overrides.remove(state);
        removeTransition(state, key);
        dirty = true;
    }

    public Transition getTransition(String state, String key) {
        if (state == null) return null;
        ObjectMap<String, Transition> stateTransitions = transitions.get(state);
        return stateTransitions == null ? null : stateTransitions.get(key);
    }

    public void setTransition(String state, String key, float duration, String interpolation) {
        ObjectMap<String, Transition> stateTransitions = transitions.get(state);
        if (stateTransitions == null) {
            stateTransitions = new ObjectMap<>(2);
            transitions.put(state, stateTransitions);
        }
        Transition transition = new Transition();
        transition.duration = duration;
        transition.interpolation = interpolation;
        stateTransitions.put(key, transition);
    }

    public void removeTransition(String state, String key) {
        ObjectMap<String, Transition> stateTransitions = transitions.get(state);
        if (stateTransitions == null) return;
        stateTransitions.remove(key);
        if (stateTransitions.size == 0) transitions.remove(state);
    }

    @Override
    public void reset() {
        role = "";
        overrides.clear();
        transitions.clear();
        tweens.clear();
        snapNext = false;
        appliedState = null;
        baseSnapshot.clear();
        dirty = false;
    }
}
