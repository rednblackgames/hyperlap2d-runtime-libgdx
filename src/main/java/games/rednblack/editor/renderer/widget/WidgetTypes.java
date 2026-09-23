package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.OrderedMap;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.widget.ProgressBarComponent;
import games.rednblack.editor.renderer.components.widget.ScrollPaneComponent;
import games.rednblack.editor.renderer.components.widget.SliderComponent;
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
    public static final String STATE_DRAGGED = "dragged";

    public static final String STATE_CHECKED_HOVER = "checkedHover";
    public static final String STATE_CHECKED_PRESSED = "checkedPressed";
    public static final String STATE_CHECKED_DISABLED = "checkedDisabled";

    public static final String BUTTON = "button";
    public static final String CHECKBOX = "checkBox";
    public static final String PROGRESS_BAR = "progressBar";
    public static final String SLIDER = "slider";
    public static final String SCROLL_PANE = "scrollPane";

    public static final String ROLE_BACKGROUND = "background";
    public static final String ROLE_FILL = "fill";
    public static final String ROLE_KNOB = "knob";
    public static final String ROLE_CONTENT = "content";
    public static final String ROLE_SCROLL_BAR_X = "scrollBarX";
    public static final String ROLE_KNOB_X = "knobX";
    public static final String ROLE_SCROLL_BAR_Y = "scrollBarY";
    public static final String ROLE_KNOB_Y = "knobY";
    public static final String ROLE_CORNER = "corner";

    public static final String PROPERTY_MIN = "min";
    public static final String PROPERTY_MAX = "max";
    public static final String PROPERTY_STEP = "step";
    public static final String PROPERTY_VALUE = "value";
    public static final String PROPERTY_VERTICAL = "vertical";
    public static final String PROPERTY_ANIMATE_DURATION = "animateDuration";
    public static final String PROPERTY_ANIMATE_INTERPOLATION = "animateInterpolation";

    public static final String PROPERTY_SCROLL_DISABLED_X = "scrollDisabledX";
    public static final String PROPERTY_SCROLL_DISABLED_Y = "scrollDisabledY";
    /** Shows a scrollbar even where there is nothing to scroll. */
    public static final String PROPERTY_FORCE_SCROLL_X = "forceScrollX";
    public static final String PROPERTY_FORCE_SCROLL_Y = "forceScrollY";
    public static final String PROPERTY_CLAMP = "clamp";
    public static final String PROPERTY_FLICK_SCROLL = "flickScroll";
    /** How far the pointer may travel before a press becomes a scroll. */
    public static final String PROPERTY_FLICK_TAP_SQUARE = "flickTapSquare";
    public static final String PROPERTY_FLING_TIME = "flingTime";
    public static final String PROPERTY_OVERSCROLL = "overscroll";
    public static final String PROPERTY_OVERSCROLL_DISTANCE = "overscrollDistance";
    public static final String PROPERTY_OVERSCROLL_SPEED_MIN = "overscrollSpeedMin";
    public static final String PROPERTY_OVERSCROLL_SPEED_MAX = "overscrollSpeedMax";
    public static final String PROPERTY_SMOOTH_SCROLLING = "smoothScrolling";
    public static final String PROPERTY_FADE_SCROLL_BARS = "fadeScrollBars";
    public static final String PROPERTY_FADE_DELAY = "fadeDelay";
    public static final String PROPERTY_FADE_DURATION = "fadeDuration";
    /** Knobs as long as the share of the content in view, instead of the size they were drawn. */
    public static final String PROPERTY_VARIABLE_SIZE_KNOBS = "variableSizeKnobs";
    public static final String PROPERTY_SCROLL_BAR_TOUCH = "scrollBarTouch";
    /** Which edges the pane lays its scrollbars along. */
    public static final String PROPERTY_BARS_ON_RIGHT = "barsOnRight";
    public static final String PROPERTY_BARS_ON_BOTTOM = "barsOnBottom";
    public static final String PROPERTY_MOUSE_WHEEL_X = "mouseWheelX";
    public static final String PROPERTY_MOUSE_WHEEL_Y = "mouseWheelY";

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

        // A progress bar the pointer drives: pressing the track puts the knob there and dragging
        // carries it along. The value itself still belongs to the bar underneath.
        register(new WidgetType(SLIDER, STATE_NORMAL, STATE_HOVER, STATE_DRAGGED, STATE_DISABLED)
                .part(ROLE_BACKGROUND, true)
                .part(ROLE_KNOB, true, CoreStateOverrides.X, CoreStateOverrides.Y)
                .part(ROLE_FILL, false, CoreStateOverrides.X, CoreStateOverrides.Y)
                .property(PROPERTY_MIN, WidgetType.PropertyKind.FLOAT, "0")
                .property(PROPERTY_MAX, WidgetType.PropertyKind.FLOAT, "100")
                .property(PROPERTY_STEP, WidgetType.PropertyKind.FLOAT, "1")
                .property(PROPERTY_VALUE, WidgetType.PropertyKind.FLOAT, "0")
                .property(PROPERTY_VERTICAL, WidgetType.PropertyKind.BOOLEAN, "false")
                .property(PROPERTY_ANIMATE_DURATION, WidgetType.PropertyKind.FLOAT, "0")
                .property(PROPERTY_ANIMATE_INTERPOLATION, WidgetType.PropertyKind.INTERPOLATION, "linear")
                .behaviour(ProgressBarComponent.class)
                .behaviour(SliderComponent.class));

        // The pane keeps its own rectangle and shows only what fits in it: the content slides
        // behind it, the scrollbars say where it is. Everything it is made from becomes its content.
        register(new WidgetType(SCROLL_PANE, STATE_NORMAL, STATE_HOVER, STATE_DRAGGED, STATE_DISABLED)
                .clips()
                .wrapsInto(ROLE_CONTENT)
                .part(ROLE_CONTENT, true, CoreStateOverrides.X, CoreStateOverrides.Y)
                .part(ROLE_BACKGROUND, false, CoreStateOverrides.X, CoreStateOverrides.Y)
                .part(ROLE_SCROLL_BAR_X, false, CoreStateOverrides.X, CoreStateOverrides.Y,
                        CoreStateOverrides.VISIBLE, CoreStateOverrides.TINT)
                .part(ROLE_KNOB_X, false, CoreStateOverrides.X, CoreStateOverrides.Y,
                        CoreStateOverrides.VISIBLE, CoreStateOverrides.TINT)
                .part(ROLE_SCROLL_BAR_Y, false, CoreStateOverrides.X, CoreStateOverrides.Y,
                        CoreStateOverrides.VISIBLE, CoreStateOverrides.TINT)
                .part(ROLE_KNOB_Y, false, CoreStateOverrides.X, CoreStateOverrides.Y,
                        CoreStateOverrides.VISIBLE, CoreStateOverrides.TINT)
                .part(ROLE_CORNER, false, CoreStateOverrides.VISIBLE)
                .property(PROPERTY_SCROLL_DISABLED_X, WidgetType.PropertyKind.BOOLEAN, "false")
                .property(PROPERTY_SCROLL_DISABLED_Y, WidgetType.PropertyKind.BOOLEAN, "false")
                .property(PROPERTY_FORCE_SCROLL_X, WidgetType.PropertyKind.BOOLEAN, "false")
                .property(PROPERTY_FORCE_SCROLL_Y, WidgetType.PropertyKind.BOOLEAN, "false")
                .property(PROPERTY_CLAMP, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_FLICK_SCROLL, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_FLICK_TAP_SQUARE, WidgetType.PropertyKind.FLOAT, "8")
                .property(PROPERTY_FLING_TIME, WidgetType.PropertyKind.FLOAT, "1")
                .property(PROPERTY_OVERSCROLL, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_OVERSCROLL_DISTANCE, WidgetType.PropertyKind.FLOAT, "50")
                .property(PROPERTY_OVERSCROLL_SPEED_MIN, WidgetType.PropertyKind.FLOAT, "30")
                .property(PROPERTY_OVERSCROLL_SPEED_MAX, WidgetType.PropertyKind.FLOAT, "200")
                .property(PROPERTY_SMOOTH_SCROLLING, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_FADE_SCROLL_BARS, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_FADE_DELAY, WidgetType.PropertyKind.FLOAT, "1")
                .property(PROPERTY_FADE_DURATION, WidgetType.PropertyKind.FLOAT, "1")
                .property(PROPERTY_VARIABLE_SIZE_KNOBS, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_SCROLL_BAR_TOUCH, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_BARS_ON_RIGHT, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_BARS_ON_BOTTOM, WidgetType.PropertyKind.BOOLEAN, "true")
                .property(PROPERTY_MOUSE_WHEEL_X, WidgetType.PropertyKind.FLOAT, "40")
                .property(PROPERTY_MOUSE_WHEEL_Y, WidgetType.PropertyKind.FLOAT, "40")
                .behaviour(ScrollPaneComponent.class));
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
            return types.orderedKeys().toArray(String[]::new);
        }
    }
}
