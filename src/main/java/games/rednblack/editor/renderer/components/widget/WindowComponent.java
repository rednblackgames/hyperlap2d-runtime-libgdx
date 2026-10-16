package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.widget.WidgetBehaviour;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * What a window holds while it runs: whether it is open, the pointer dragging it, and whoever wants
 * to hear it open, close or move.
 *
 * Whether it starts open comes from the widget's settings, and follows them when they change; game
 * code opens and closes it here.
 */
@Transient
public class WindowComponent extends PooledComponent implements WidgetBehaviour {

    public boolean isOpen = true;
    /** The open setting last followed, so a change of setting is followed but a close is not undone. */
    public String openSetting = null;

    /** Pointer dragging the window by its title bar, -1 if none, and where it took hold, in the window. */
    public int dragPointer = -1;
    public float grabX, grabY;

    /** Pointer pressing the close part or the backdrop, -1 if none. */
    public int closePointer = -1;
    public int backdropPointer = -1;

    /** What the listeners were last told, and whether they have been told at all. */
    public boolean notifiedOpen = false;
    public float notifiedX, notifiedY;
    public boolean notified = false;

    public final Array<WindowListener> listeners = new Array<>(true, 1, WindowListener[]::new);

    public interface WindowListener {
        default void opened(int entity) {
        }

        /** The window was closed: by its close part, Escape, a tap on its backdrop or game code. */
        default void closed(int entity) {
        }

        /** The window was dragged, or kept within the viewport, to a new place in its parent. */
        default void moved(int entity, float x, float y) {
        }
    }

    @Override
    public void initialise(WidgetComponent widget) {
        openSetting = widget.properties.get(WidgetTypes.PROPERTY_OPEN);
        isOpen = openSetting == null || Boolean.parseBoolean(openSetting);
        notifiedOpen = isOpen;
    }

    public void open() {
        isOpen = true;
    }

    public void close() {
        isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isDragging() {
        return dragPointer != -1;
    }

    public void addListener(WindowListener listener) {
        listeners.add(listener);
    }

    public void removeListener(WindowListener listener) {
        listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void reset() {
        isOpen = true;
        openSetting = null;
        dragPointer = -1;
        grabX = grabY = 0;
        closePointer = -1;
        backdropPointer = -1;
        notifiedOpen = false;
        notifiedX = notifiedY = 0;
        notified = false;
        listeners.clear();
    }
}
