package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;

import java.util.Objects;

public class WidgetPartVO {
    public String role = "";
    /** state name -> what that state overrides */
    public ObjectMap<String, WidgetStateOverridesVO> overrides = new ObjectMap<>(0);

    public WidgetPartVO() {
    }

    public WidgetPartVO(WidgetPartVO vo) {
        if (vo == null) return;
        role = vo.role;
        for (ObjectMap.Entry<String, WidgetStateOverridesVO> entry : vo.overrides) {
            if (entry.value == null || entry.value.values.size == 0) continue;
            overrides.put(entry.key, new WidgetStateOverridesVO(entry.value));
        }
    }

    public void loadFromComponent(WidgetPartComponent component) {
        role = component.role == null ? "" : component.role;
        overrides.clear();
        for (ObjectMap.Entry<String, ObjectMap<String, String>> entry : component.overrides) {
            if (entry.value == null || entry.value.size == 0) continue;

            WidgetStateOverridesVO stateOverrides = new WidgetStateOverridesVO();
            stateOverrides.values.putAll(entry.value);

            ObjectMap<String, WidgetPartComponent.Transition> transitions = component.transitions.get(entry.key);
            if (transitions != null) {
                for (ObjectMap.Entry<String, WidgetPartComponent.Transition> transition : transitions) {
                    // a transition only makes sense for a property the state overrides
                    if (!entry.value.containsKey(transition.key)) continue;
                    stateOverrides.transitions.put(transition.key,
                            new WidgetOverrideTransitionVO(transition.value.duration, transition.value.interpolation));
                }
            }
            overrides.put(entry.key, stateOverrides);
        }
    }

    public void applyToComponent(WidgetPartComponent component) {
        component.role = role;
        component.overrides.clear();
        component.transitions.clear();
        for (ObjectMap.Entry<String, WidgetStateOverridesVO> entry : overrides) {
            if (entry.value == null || entry.value.values.size == 0) continue;
            component.overrides.put(entry.key, new ObjectMap<>(entry.value.values));

            for (ObjectMap.Entry<String, WidgetOverrideTransitionVO> transition : entry.value.transitions) {
                if (transition.value == null || !entry.value.values.containsKey(transition.key)) continue;
                component.setTransition(entry.key, transition.key, transition.value.duration, transition.value.interpolation);
            }
        }
        component.dirty = true;
    }

    /** @return the value a state overrides a property with, null if it does not */
    public String getOverride(String state, String key) {
        WidgetStateOverridesVO stateOverrides = overrides.get(state);
        return stateOverrides == null ? null : stateOverrides.values.get(key);
    }

    /** @param value null removes the override, and the transition that went with it */
    public void setOverride(String state, String key, String value) {
        WidgetStateOverridesVO stateOverrides = overrides.get(state);
        if (value == null) {
            if (stateOverrides == null) return;
            stateOverrides.values.remove(key);
            stateOverrides.transitions.remove(key);
            if (stateOverrides.values.size == 0) overrides.remove(state);
        } else {
            if (stateOverrides == null) {
                stateOverrides = new WidgetStateOverridesVO();
                overrides.put(state, stateOverrides);
            }
            stateOverrides.values.put(key, value);
        }
    }

    /** @return how the property gets to the value the state overrides it with, null if instantly */
    public WidgetOverrideTransitionVO getTransition(String state, String key) {
        WidgetStateOverridesVO stateOverrides = overrides.get(state);
        return stateOverrides == null ? null : stateOverrides.transitions.get(key);
    }

    /**
     * @param transition null makes the property change instantly again. Ignored for a property the
     *                   state does not override.
     */
    public void setTransition(String state, String key, WidgetOverrideTransitionVO transition) {
        WidgetStateOverridesVO stateOverrides = overrides.get(state);
        if (stateOverrides == null || !stateOverrides.values.containsKey(key)) return;

        if (transition == null) stateOverrides.transitions.remove(key);
        else stateOverrides.transitions.put(key, transition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetPartVO that = (WidgetPartVO) o;
        return Objects.equals(role, that.role) && overrides.equals(that.overrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, overrides);
    }
}
