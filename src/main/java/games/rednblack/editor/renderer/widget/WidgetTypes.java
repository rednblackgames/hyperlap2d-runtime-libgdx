package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.OrderedMap;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.widget.ProgressBarComponent;
import games.rednblack.editor.renderer.widget.handlers.CoreStateOverrides;

/**
 * Registry of the known {@link WidgetType}s. Built-in types are registered here, games and
 * extensions may add their own before the scene is loaded.
 */
public final class WidgetTypes {
    private WidgetTypes() {
    }

    public static final String STATE_NORMAL = "normal";
    public static final String STATE_HOVER = "hover";
    public static final String STATE_PRESSED = "pressed";
    public static final String STATE_CHECKED = "checked";
    public static final String STATE_DISABLED = "disabled";

    public static final String STATE_CHECKED_HOVER = "checkedHover";
    public static final String STATE_CHECKED_PRESSED = "checkedPressed";
    public static final String STATE_CHECKED_DISABLED = "checkedDisabled";

    public static final String BUTTON = "button";
    public static final String CHECKBOX = "checkBox";
    public static final String PROGRESS_BAR = "progressBar";

    public static final String ROLE_BACKGROUND = "background";
    public static final String ROLE_FILL = "fill";
    public static final String ROLE_KNOB = "knob";

    public static final String PROPERTY_MIN = "min";
    public static final String PROPERTY_MAX = "max";
    public static final String PROPERTY_STEP = "step";
    public static final String PROPERTY_VALUE = "value";
    public static final String PROPERTY_VERTICAL = "vertical";
    public static final String PROPERTY_ANIMATE_DURATION = "animateDuration";
    public static final String PROPERTY_ANIMATE_INTERPOLATION = "animateInterpolation";

    /** Setting of a checkable widget: whether it starts checked. */
    public static final String PROPERTY_CHECKED = "checked";
    /** Setting of a checkable widget: its radio group among its siblings, empty for none. */
    public static final String PROPERTY_GROUP = "group";

    private static final OrderedMap<String, WidgetType> types = new OrderedMap<>();

    static {
        register(new WidgetType(BUTTON, STATE_NORMAL, STATE_HOVER, STATE_PRESSED, STATE_CHECKED, STATE_DISABLED)
                .behaviour(ButtonComponent.class));

        // A click checks and unchecks it. The checked look is authored once in checked, and the
        // checked variants of hover, press and disabled build on it with their own touch only.
        register(new WidgetType(CHECKBOX, STATE_NORMAL, STATE_HOVER, STATE_PRESSED, STATE_DISABLED,
                STATE_CHECKED, STATE_CHECKED_HOVER, STATE_CHECKED_PRESSED, STATE_CHECKED_DISABLED)
                .inherit(STATE_CHECKED_HOVER, STATE_CHECKED)
                .inherit(STATE_CHECKED_PRESSED, STATE_CHECKED)
                .inherit(STATE_CHECKED_DISABLED, STATE_CHECKED)
                .property(PROPERTY_CHECKED, WidgetType.PropertyKind.BOOLEAN, "false")
                .property(PROPERTY_GROUP, WidgetType.PropertyKind.STRING, "")
                .checkable()
                .behaviour(ButtonComponent.class));

        // The background is the track and gives the bar its size; the fill and the knob are placed
        // by the bar itself, so their position is never a state override.
        register(new WidgetType(PROGRESS_BAR, STATE_NORMAL, STATE_DISABLED)
                .part(ROLE_BACKGROUND, true)
                .part(ROLE_FILL, true, CoreStateOverrides.X, CoreStateOverrides.Y)
                .part(ROLE_KNOB, false, CoreStateOverrides.X, CoreStateOverrides.Y)
                .property(PROPERTY_MIN, WidgetType.PropertyKind.FLOAT, "0")
                .property(PROPERTY_MAX, WidgetType.PropertyKind.FLOAT, "100")
                .property(PROPERTY_STEP, WidgetType.PropertyKind.FLOAT, "1")
                .property(PROPERTY_VALUE, WidgetType.PropertyKind.FLOAT, "0")
                .property(PROPERTY_VERTICAL, WidgetType.PropertyKind.BOOLEAN, "false")
                .property(PROPERTY_ANIMATE_DURATION, WidgetType.PropertyKind.FLOAT, "0")
                .property(PROPERTY_ANIMATE_INTERPOLATION, WidgetType.PropertyKind.INTERPOLATION, "linear")
                .behaviour(ProgressBarComponent.class));
    }

    public static void register(WidgetType type) {
        synchronized (types) {
            types.put(type.name, type);
        }
    }

    /** @return the descriptor, or null for an unknown / empty type (a plain state holder) */
    public static WidgetType get(String name) {
        if (name == null || name.isEmpty()) return null;
        synchronized (types) {
            return types.get(name);
        }
    }

    /** @return registered type names, in registration order */
    public static String[] names() {
        synchronized (types) {
            return types.orderedKeys().toArray(String.class);
        }
    }
}
