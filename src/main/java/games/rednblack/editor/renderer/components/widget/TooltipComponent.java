package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.widget.WidgetBehaviour;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * What a tooltip holds while it runs: whether it is open, the entity it points at, and whoever wants
 * to hear it open and close.
 *
 * Game code opens it on an entity with {@link #showAt}; the tooltip then places itself by that
 * entity, every frame, and closes on a press anywhere else, or when the entity is gone or hidden.
 */
@Transient
public class TooltipComponent extends PooledComponent implements WidgetBehaviour {

    public boolean isOpen = false;
    /** The open setting last followed, so a change of setting is followed but a close is not undone. */
    public String openSetting = null;
    /** The entity it points at, -1 for none: one opened without a target stays where it was drawn. */
    public int target = -1;

    /** Whether it was last placed below its target, so the pointer is turned the other way. */
    public boolean below = false;
    /** Where the pointer was drawn, in the tooltip, and whether it was drawn under the box; read once. */
    public boolean pointerRead = false;
    public float pointerY, pointerScaleY;
    public boolean pointerDrawnBelow = true;

    /** What the listeners were last told, and whether they have been told at all. */
    public boolean notifiedOpen = false;
    public boolean notifiedShown = false;
    public int notifiedTarget = -1;
    public boolean notified = false;

    public final Array<TooltipListener> listeners = new Array<>(true, 1, TooltipListener[]::new);

    public interface TooltipListener {
        /** It was opened, or moved on to another target while open. */
        default void opened(int entity, int target) {
        }

        /** It was closed: by a press elsewhere, its target going, or game code. */
        default void closed(int entity) {
        }

        /** A closed tooltip has played its closed look and is out of sight. */
        default void hidden(int entity) {
        }
    }

    @Override
    public void initialise(WidgetComponent widget) {
        openSetting = widget.properties.get(WidgetTypes.PROPERTY_OPEN);
        isOpen = openSetting != null && Boolean.parseBoolean(openSetting);
        notifiedOpen = isOpen;
    }

    /**
     * Opens it on the target, or closes it when it is already open on that target: a second tap on
     * what showed it takes it away.
     */
    public void showAt(int target) {
        if (isOpen && this.target == target) {
            isOpen = false;
            return;
        }
        this.target = target;
        isOpen = true;
    }

    public void hide() {
        isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public int getTarget() {
        return target;
    }

    public void addListener(TooltipListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TooltipListener listener) {
        listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void reset() {
        isOpen = false;
        openSetting = null;
        target = -1;
        below = false;
        pointerRead = false;
        pointerY = 0;
        pointerScaleY = 0;
        pointerDrawnBelow = true;
        notifiedOpen = false;
        notifiedShown = false;
        notifiedTarget = -1;
        notified = false;
        listeners.clear();
    }
}
