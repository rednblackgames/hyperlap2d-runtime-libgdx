package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.ecs.Component;

/**
 * Describes a kind of widget: the states it goes through, the named parts it is made of, its own
 * settings and the behaviour components that drive it at runtime.
 *
 * A descriptor is the single source of truth shared by the runtime (which attaches the behaviour
 * components) and the editor (which builds the widget editing UI out of it).
 */
public class WidgetType {

    public enum PropertyKind {BOOLEAN, INT, FLOAT, STRING}

    public static class Part {
        public final String role;
        public final boolean required;

        public Part(String role, boolean required) {
            this.role = role;
            this.required = required;
        }
    }

    public static class Property {
        public final String key;
        public final PropertyKind kind;
        public final String defaultValue;

        public Property(String key, PropertyKind kind, String defaultValue) {
            this.key = key;
            this.kind = kind;
            this.defaultValue = defaultValue;
        }
    }

    public final String name;
    public final String defaultState;
    public final Array<String> states = new Array<>(true, 4, String.class);
    public final Array<Part> parts = new Array<>(true, 2, Part.class);
    public final Array<Property> properties = new Array<>(true, 2, Property.class);
    public final Array<Class<? extends Component>> behaviourComponents = new Array<>(true, 1);

    /**
     * @param states the first one is the default state
     */
    public WidgetType(String name, String... states) {
        if (states.length == 0) throw new IllegalArgumentException("A widget type needs at least one state.");
        this.name = name;
        this.defaultState = states[0];
        this.states.addAll(states);
    }

    public WidgetType part(String role, boolean required) {
        parts.add(new Part(role, required));
        return this;
    }

    public WidgetType property(String key, PropertyKind kind, String defaultValue) {
        properties.add(new Property(key, kind, defaultValue));
        return this;
    }

    public WidgetType behaviour(Class<? extends Component> component) {
        behaviourComponents.add(component);
        return this;
    }

    public Part getPart(String role) {
        for (int i = 0; i < parts.size; i++) {
            if (parts.get(i).role.equals(role)) return parts.get(i);
        }
        return null;
    }

    /** Fills a fresh component with the states and default settings of this type. */
    public void setup(WidgetComponent component) {
        component.widgetType = name;
        component.states.clear();
        component.states.addAll(states);
        component.defaultState = defaultState;
        component.currentState = null;
        component.properties.clear();
        for (int i = 0; i < properties.size; i++) {
            Property property = properties.get(i);
            if (property.defaultValue != null) component.properties.put(property.key, property.defaultValue);
        }
    }
}
