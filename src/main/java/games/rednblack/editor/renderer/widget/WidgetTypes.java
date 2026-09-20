package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.OrderedMap;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;

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

    public static final String BUTTON = "button";

    private static final OrderedMap<String, WidgetType> types = new OrderedMap<>();

    static {
        register(new WidgetType(BUTTON, STATE_NORMAL, STATE_HOVER, STATE_PRESSED, STATE_CHECKED, STATE_DISABLED)
                .behaviour(ButtonComponent.class));
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
