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
 * is drawn right now: the two differ only while the pane glides after the wheel, a scrollbar or game
 * code moved it. Dragging, flinging and springing back move both together, so the content stays
 * under the finger. The system keeps them in range and places the content and the knobs.
 */
@Transient
public class ScrollPaneComponent extends PooledComponent {

    public float scrollX, scrollY;
    public float visualScrollX, visualScrollY;

    /** How far the content can travel, zero when it fits. */
    public float maxX, maxY;
    /** The visible rectangle, and the content measured as it is drawn. */
    public float areaWidth, areaHeight, contentWidth, contentHeight;

    /** How many finger movements are kept to tell how fast it was going when it let go. */
    public static final int SAMPLES = 32;

    /**
     * How fast the scroll is moving, in units per second, while the pane flings or springs back
     * from an overscroll: zero when it rests.
     */
    public float velocityX, velocityY;

    /**
     * Where the finger would have taken the scroll with nothing to hold it back. Past an edge the
     * pane gives way less and less, and this is what that resistance is worked out from.
     */
    public float dragX, dragY;

    /** The finger's movement since the last frame, not yet in the samples. */
    public float pendingX, pendingY;
    /** The finger's last movements, one per frame, with how long each frame was. */
    public final float[] sampleTime = new float[SAMPLES];
    public final float[] sampleX = new float[SAMPLES];
    public final float[] sampleY = new float[SAMPLES];
    public int sampleHead, sampleCount;

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

    /** True while the pane moves on its own, flinging or springing back from an overscroll. */
    public boolean isFlinging() {
        return velocityX != 0 || velocityY != 0;
    }

    public void clearSamples() {
        pendingX = pendingY = 0;
        sampleHead = sampleCount = 0;
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
        dragX = dragY = 0;
        clearSamples();
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
