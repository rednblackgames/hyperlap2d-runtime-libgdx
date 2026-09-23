package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;

/**
 * Where a scroll pane is looking, and everything the look of it needs.
 *
 * {@link #scrollX} and {@link #scrollY} are where the pane is heading, counted from the left and
 * from the top of the content, while {@link #visualScrollX} and {@link #visualScrollY} are where it
 * is drawn right now: the two differ while the pane glides, flings or springs back from an
 * overscroll. The system keeps them in range and places the content and the knobs.
 */
@Transient
public class ScrollPaneComponent extends PooledComponent {

    public float scrollX, scrollY;
    public float visualScrollX, visualScrollY;

    /** How far the content can travel, zero when it fits. */
    public float maxX, maxY;
    /** The visible rectangle, and the content measured as it is drawn. */
    public float areaWidth, areaHeight, contentWidth, contentHeight;

    /** Speed of the finger when it let go, driving the fling until the timer runs out. */
    public float velocityX, velocityY;
    public float flingTimer;

    public boolean isTouchEnabled = true;
    public boolean isHovered = false;

    /** Pointer scrolling the pane, -1 if none; it may be following it before the drag begins. */
    public int touchPointer = -1;
    /** True once the pointer has travelled far enough for the press to be a scroll. */
    public boolean dragging = false;
    /** True while a scrollbar knob is being dragged, which is not a flick. */
    public boolean draggingKnobX = false, draggingKnobY = false;
    public float pressX, pressY, lastX, lastY;
    /** How long the pointer has been still while dragging: a finger that stopped does not fling. */
    public float dragIdle;
    /** Where the knob was grabbed, so it does not jump under the pointer. */
    public float knobGrab;

    /** How visible the scrollbars are, and how long before they start fading. */
    public float fadeAlpha = 1;
    public float fadeDelayLeft = 0;

    public boolean started = false;
    public boolean notified = false;
    public float notifiedX, notifiedY;

    public final Array<ScrollPaneListener> listeners = new Array<>(true, 1, ScrollPaneListener[]::new);

    public interface ScrollPaneListener {
        /** The pane is looking somewhere else, whether a pointer or game code sent it there. */
        void scrolled(int entity, float scrollX, float scrollY);
    }

    public boolean isDragging() {
        return dragging || draggingKnobX || draggingKnobY;
    }

    public void setScroll(float x, float y) {
        scrollX = x;
        scrollY = y;
    }

    public float getScrollPercentX() {
        return maxX == 0 ? 0 : MathUtils.clamp(scrollX / maxX, 0, 1);
    }

    public float getScrollPercentY() {
        return maxY == 0 ? 0 : MathUtils.clamp(scrollY / maxY, 0, 1);
    }

    public float getVisualScrollPercentX() {
        return maxX == 0 ? 0 : MathUtils.clamp(visualScrollX / maxX, 0, 1);
    }

    public float getVisualScrollPercentY() {
        return maxY == 0 ? 0 : MathUtils.clamp(visualScrollY / maxY, 0, 1);
    }

    public void setScrollPercentX(float percent) {
        scrollX = maxX * MathUtils.clamp(percent, 0, 1);
    }

    public void setScrollPercentY(float percent) {
        scrollY = maxY * MathUtils.clamp(percent, 0, 1);
    }

    /**
     * Brings a rectangle of the content into view, moving as little as possible.
     *
     * @param x in the content's own coordinates, y counted from its top
     */
    public void scrollTo(float x, float y, float width, float height) {
        float left = scrollX, right = scrollX + areaWidth;
        if (x < left) scrollX = x;
        else if (x + width > right) scrollX = x + width - areaWidth;

        float top = scrollY, bottom = scrollY + areaHeight;
        if (y < top) scrollY = y;
        else if (y + height > bottom) scrollY = y + height - areaHeight;

        scrollX = MathUtils.clamp(scrollX, 0, maxX);
        scrollY = MathUtils.clamp(scrollY, 0, maxY);
    }

    public void addListener(ScrollPaneListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ScrollPaneListener listener) {
        listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void reset() {
        scrollX = scrollY = 0;
        visualScrollX = visualScrollY = 0;
        maxX = maxY = 0;
        areaWidth = areaHeight = contentWidth = contentHeight = 0;
        velocityX = velocityY = 0;
        flingTimer = 0;
        isTouchEnabled = true;
        isHovered = false;
        touchPointer = -1;
        dragging = false;
        draggingKnobX = draggingKnobY = false;
        pressX = pressY = lastX = lastY = 0;
        dragIdle = 0;
        knobGrab = 0;
        fadeAlpha = 1;
        fadeDelayLeft = 0;
        started = false;
        notified = false;
        notifiedX = notifiedY = 0;
        listeners.clear();
    }
}
