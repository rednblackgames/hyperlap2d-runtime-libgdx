package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
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

    /** INTERPOLATION is the name of a libGDX interpolation function, offered as a list by the editor. */
    public enum PropertyKind {BOOLEAN, INT, FLOAT, STRING, INTERPOLATION}

    public static class Part {
        public final String role;
        public final boolean required;
        /**
         * Properties of the part the widget sets itself, such as the position of a knob: they are
         * never recorded as state overrides, since the widget would overwrite them anyway.
         */
        public final Array<String> drivenKeys = new Array<>(true, 2, String[]::new);

        public Part(String role, boolean required, String... drivenKeys) {
            this.role = role;
            this.required = required;
            this.drivenKeys.addAll(drivenKeys);
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
    public final Array<String> states = new Array<>(true, 4, String[]::new);
    public final Array<Part> parts = new Array<>(true, 2, Part[]::new);
    public final Array<Property> properties = new Array<>(true, 2, Property[]::new);
    public final Array<Class<? extends Component>> behaviourComponents = new Array<>(true, 1);
    /** child state -> the state it builds on: showing the child applies the parent's look first. */
    public final ObjectMap<String, String> parents = new ObjectMap<>(0);
    /** True for a widget a click checks and unchecks. */
    public boolean checkable = false;
    /**
     * True for a widget that owns the clipping and the size of its composite: it draws only what
     * fits in its own rectangle and never grows to its children, and the editor locks both switches.
     */
    public boolean clipsContent = false;
    /**
     * Role the items the widget is made from are wrapped into when the editor creates it, null when
     * they stay as they are. A scroll pane puts them in its content, which is what slides.
     */
    public String wrapRole = null;

    /**
     * @param states the first one is the default state
     */
    public WidgetType(String name, String... states) {
        if (states.length == 0) throw new IllegalArgumentException("A widget type needs at least one state.");
        this.name = name;
        this.defaultState = states[0];
        this.states.addAll(states);
    }

    public WidgetType part(String role, boolean required, String... drivenKeys) {
        parts.add(new Part(role, required, drivenKeys));
        return this;
    }

    /** Makes a state build on another one: it shows the parent's look plus its own overrides. */
    public WidgetType inherit(String child, String parent) {
        parents.put(child, parent);
        return this;
    }

    public WidgetType checkable() {
        checkable = true;
        return this;
    }

    /** The widget clips what it holds to its own rectangle, and keeps that rectangle. */
    public WidgetType clips() {
        clipsContent = true;
        return this;
    }

    /** What the widget is made from goes into a part of this role, instead of straight inside it. */
    public WidgetType wrapsInto(String role) {
        wrapRole = role;
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
        component.parents.clear();
        component.parents.putAll(parents);
        component.properties.clear();
        for (int i = 0; i < properties.size; i++) {
            Property property = properties.get(i);
            if (property.defaultValue != null) component.properties.put(property.key, property.defaultValue);
        }
    }
}
