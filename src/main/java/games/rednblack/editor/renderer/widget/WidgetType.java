package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.graphics.Color;
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

    /**
     * INTERPOLATION is the name of a libGDX interpolation function, offered as a list by the
     * editor; CHOICE is one of the values the property was declared with, offered the same way.
     */
    public enum PropertyKind {BOOLEAN, INT, FLOAT, STRING, INTERPOLATION, CHOICE}

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

    /**
     * A part the editor draws for you when the widget is made, out of an image every project has:
     * a caret and a selection band are shapes nobody wants to draw by hand. It becomes an ordinary
     * item, so it can be restyled, retinted, replaced with real art or deleted.
     */
    public static class DefaultPart {
        public final String role;
        /** Region in the project's atlas, `white-pixel` being the one every project is given. */
        public final String region;
        public final float width, height;
        /** Colour to give it, null to leave it as it was drawn. */
        public final Color tint;
        /** Where it sits among its siblings: a selection band goes behind the text, a caret over it. */
        public final int zIndex;

        public DefaultPart(String role, String region, float width, float height, Color tint, int zIndex) {
            this.role = role;
            this.region = region;
            this.width = width;
            this.height = height;
            this.tint = tint;
            this.zIndex = zIndex;
        }
    }

    public static class Property {
        public final String key;
        public final PropertyKind kind;
        public final String defaultValue;
        /** What a CHOICE property may hold, empty for every other kind. */
        public final Array<String> choices = new Array<>(true, 2, String[]::new);

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
    /** Parts the editor makes along with the widget, for roles that are shapes rather than art. */
    public final Array<DefaultPart> defaultParts = new Array<>(true, 2, DefaultPart[]::new);
    /** True for a widget a click checks and unchecks. */
    public boolean checkable = false;
    /**
     * True for a widget that owns the clipping and the size of its composite: it draws only what
     * fits in its own rectangle and never grows to its children, and the editor locks both switches.
     */
    public boolean clipsContent = false;
    /**
     * True for a widget holding several lines of text: Enter makes a new one instead of meaning
     * "done", the up and down keys walk the lines, and the text is broken to the width it is given.
     */
    public boolean multiline = false;
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

    /** The widget holds several lines of text rather than one. */
    public WidgetType multiline() {
        multiline = true;
        return this;
    }

    /** The widget clips what it holds to its own rectangle, and keeps that rectangle. */
    public WidgetType clips() {
        clipsContent = true;
        return this;
    }

    /** A part the editor draws for you when the widget is made. See {@link DefaultPart}. */
    public WidgetType defaultPart(String role, String region, float width, float height, Color tint, int zIndex) {
        defaultParts.add(new DefaultPart(role, region, width, height, tint, zIndex));
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

    /** A setting the editor offers as a list: the default is the first choice that is worth having. */
    public WidgetType choice(String key, String defaultValue, String... choices) {
        Property property = new Property(key, PropertyKind.CHOICE, defaultValue);
        property.choices.addAll(choices);
        properties.add(property);
        return this;
    }

    /**
     * The same, from the constants themselves, so the list the editor offers and the cases the
     * widget handles cannot drift apart. What is stored is still what each one is called, since a
     * setting is text wherever it is written; a list worked out at runtime uses the other one.
     */
    public WidgetType choice(String key, Enum<?> defaultValue, Enum<?>... choices) {
        Property property = new Property(key, PropertyKind.CHOICE, String.valueOf(defaultValue));
        for (Enum<?> choice : choices) property.choices.add(String.valueOf(choice));
        properties.add(property);
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
