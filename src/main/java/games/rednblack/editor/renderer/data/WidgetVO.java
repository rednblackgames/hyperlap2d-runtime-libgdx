package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;

import java.util.Objects;

public class WidgetVO {
    public String widgetType = "";
    public Array<String> states = new Array<>(true, 4, String.class);
    public String defaultState = "normal";
    public ObjectMap<String, String> properties = new ObjectMap<>(0);
    /** child state -> the state it builds on */
    public ObjectMap<String, String> parents = new ObjectMap<>(0);

    public WidgetVO() {
    }

    public WidgetVO(WidgetVO vo) {
        if (vo == null) return;
        widgetType = vo.widgetType;
        states.addAll(vo.states);
        defaultState = vo.defaultState;
        properties.putAll(vo.properties);
        parents.putAll(vo.parents);
    }

    public void loadFromComponent(WidgetComponent component) {
        widgetType = component.widgetType;
        states.clear();
        states.addAll(component.states);
        defaultState = component.defaultState;
        properties.clear();
        properties.putAll(component.properties);
        parents.clear();
        parents.putAll(component.parents);
    }

    public void applyToComponent(WidgetComponent component) {
        component.widgetType = widgetType;
        component.states.clear();
        component.states.addAll(states);
        component.defaultState = defaultState;
        component.properties.clear();
        component.properties.putAll(properties);
        component.parents.clear();
        component.parents.putAll(parents);
        if (!component.hasState(component.currentState)) component.currentState = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetVO that = (WidgetVO) o;
        return Objects.equals(widgetType, that.widgetType)
                && Objects.equals(defaultState, that.defaultState)
                && states.equals(that.states)
                && properties.equals(that.properties)
                && parents.equals(that.parents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(widgetType, defaultState, states, properties, parents);
    }
}
