package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;

/**
 * What a slider adds to the progress bar it is built on: the pointer dragging it, the pointer
 * hovering it, and whoever wants to hear the value change.
 *
 * The value itself lives in the {@link ProgressBarComponent} beside this one, since the bar is what
 * keeps it in range and places the parts.
 */
@Transient
public class SliderComponent extends PooledComponent {

    public boolean isTouchEnabled = true;
    public boolean isHovered = false;
    /** Pointer dragging the slider, -1 if none. */
    public int touchPointer = -1;

    /** Last value the listeners were told about, and whether they have been told at all. */
    public float notifiedValue = 0;
    public boolean notified = false;

    public final Array<SliderListener> listeners = new Array<>(true, 1, SliderListener[]::new);

    public interface SliderListener {
        /** The slider holds a new value, whether a drag or game code put it there. */
        void valueChanged(int entity, float value);
    }

    public boolean isDragging() {
        return touchPointer != -1;
    }

    public void addListener(SliderListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SliderListener listener) {
        listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void reset() {
        isTouchEnabled = true;
        isHovered = false;
        touchPointer = -1;
        notifiedValue = 0;
        notified = false;
        listeners.clear();
    }
}
