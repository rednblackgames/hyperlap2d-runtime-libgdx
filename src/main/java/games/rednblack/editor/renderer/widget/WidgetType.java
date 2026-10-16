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
     * editor; CHOICE is one of the values the property was declared with, offered the same way;
     * LIST is several values, one per line, such as the items of a select box.
     */
    public enum PropertyKind {BOOLEAN, INT, FLOAT, STRING, INTERPOLATION, CHOICE, LIST}

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
     * A part the editor makes for you when the widget is made, so a new widget works at once: a caret
     * and a selection band are shapes nobody wants to draw by hand, and a select box needs a list and
     * a row before it can show anything. It becomes an ordinary item, so it can be restyled, retinted,
     * replaced with real art or deleted.
     *
     * An image is cut from a region every project has; a composite may hold further default items and
     * be made a widget of its own, with the looks of its states already set on what it holds.
     */
    public static class DefaultPart {
        public enum Kind {IMAGE, COMPOSITE, LABEL}

        /** A width or height that is the widget's own, whatever size it was made at. */
        public static final float WIDGET_SIZE = -1;

        /** Role it plays in the widget, null for an item inside a default part that plays none. */
        public final String role;
        public final Kind kind;
        /** Region in the project's atlas for an image, `white-pixel` being the one every project is given. */
        public String region;
        /** What a label says. */
        public String text = "";
        public float x, y;
        public final float width, height;
        /** Colour to give it, null to leave it as it was drawn. */
        public Color tint;
        /** Where it sits among its siblings: a selection band goes behind the text, a caret over it. */
        public int zIndex;
        /** Widget type a composite is made, null for a plain one. */
        public String widgetType;
        /** What a composite holds. */
        public final Array<DefaultPart> children = new Array<>(true, 0, DefaultPart[]::new);
        /** state -> (property key -> value): how it looks in the states of the widget it is in. */
        public final ObjectMap<String, ObjectMap<String, String>> overrides = new ObjectMap<>(0);
        /**
         * Whether it is constrained to the width, or the height, of what holds it, so it follows
         * when that is resized; the margin is left free on both sides.
         */
        public boolean matchWidth, matchHeight;
        public float marginX, marginY;
        /** Whether it is held to the top, or the right, edge of what holds it, at the given distance. */
        public boolean pinTop, pinRight;
        public float marginTop, marginRight;

        public DefaultPart(String role, String region, float width, float height, Color tint, int zIndex) {
            this(role, Kind.IMAGE, width, height);
            this.region = region;
            this.tint = tint;
            this.zIndex = zIndex;
        }

        public DefaultPart(String role, Kind kind, float width, float height) {
            this.role = role;
            this.kind = kind;
            this.width = width;
            this.height = height;
        }

        public static DefaultPart image(String role, String region, float width, float height) {
            DefaultPart part = new DefaultPart(role, Kind.IMAGE, width, height);
            part.region = region;
            return part;
        }

        public static DefaultPart composite(String role, float width, float height) {
            return new DefaultPart(role, Kind.COMPOSITE, width, height);
        }

        public static DefaultPart label(String role, String text, float width, float height) {
            DefaultPart part = new DefaultPart(role, Kind.LABEL, width, height);
            part.text = text;
            return part;
        }

        public DefaultPart at(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public DefaultPart tint(Color tint) {
            this.tint = tint;
            return this;
        }

        public DefaultPart zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        public DefaultPart widget(String widgetType) {
            this.widgetType = widgetType;
            return this;
        }

        public DefaultPart child(DefaultPart child) {
            children.add(child);
            return this;
        }

        /** Stretches it between the left and right edges of what holds it, the margin in from each. */
        public DefaultPart matchWidth(float margin) {
            matchWidth = true;
            marginX = margin;
            return this;
        }

        /** Stretches it between the bottom and top edges of what holds it, the margin in from each. */
        public DefaultPart matchHeight(float margin) {
            matchHeight = true;
            marginY = margin;
            return this;
        }

        /** Keeps it at the given distance from the top edge of what holds it, whatever that edge does. */
        public DefaultPart pinTop(float margin) {
            pinTop = true;
            marginTop = margin;
            return this;
        }

        /** Keeps it at the given distance from the right edge of what holds it. */
        public DefaultPart pinRight(float margin) {
            pinRight = true;
            marginRight = margin;
            return this;
        }

        public DefaultPart override(String state, String key, String value) {
            ObjectMap<String, String> values = overrides.get(state);
            if (values == null) {
                values = new ObjectMap<>(1);
                overrides.put(state, values);
            }
            values.put(key, value);
            return this;
        }

        /** @return the size asked for, or the widget's own when {@link #WIDGET_SIZE} was */
        public float width(float widgetWidth) {
            return width == WIDGET_SIZE ? widgetWidth : width;
        }

        public float height(float widgetHeight) {
            return height == WIDGET_SIZE ? widgetHeight : height;
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
     * Role the item plays when it alone is made into the widget, null for none: the image a window
     * is made from is its background. It is stretched to the widget's size with constraints.
     */
    public String convertedItemRole = null;

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

    /** A part the editor makes for you when the widget is made. See {@link DefaultPart}. */
    public WidgetType defaultPart(DefaultPart part) {
        defaultParts.add(part);
        return this;
    }

    /** The one item the widget is made from plays this role. See {@link #convertedItemRole}. */
    public WidgetType convertedItemPlays(String role) {
        convertedItemRole = role;
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
