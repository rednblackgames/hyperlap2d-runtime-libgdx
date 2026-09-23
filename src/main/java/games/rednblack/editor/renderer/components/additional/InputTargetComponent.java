package games.rednblack.editor.renderer.components.additional;

import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.input.Touchable;
import games.rednblack.editor.renderer.input.UIInputListener;

/**
 * Makes an entity take part in input: only entities carrying this component are ever hit, so a
 * scene full of images costs nothing to hit test.
 *
 * Listeners are called when the event reaches the entity on its way back up from the target
 * (bubbling); capture listeners are called on the way down, before anything inside is told, which
 * is how a modal panel swallows everything under it.
 */
@Transient
public class InputTargetComponent extends PooledComponent {

    public Touchable touchable = Touchable.ENABLED;
    /** True for an entity a click gives the keyboard focus to, such as a text field. */
    public boolean focusable = false;

    public final Array<UIInputListener> listeners = new Array<>(true, 1, UIInputListener[]::new);
    public final Array<UIInputListener> captureListeners = new Array<>(true, 0, UIInputListener[]::new);

    public void addListener(UIInputListener listener) {
        if (!listeners.contains(listener, true)) listeners.add(listener);
    }

    public void removeListener(UIInputListener listener) {
        listeners.removeValue(listener, true);
    }

    public void addCaptureListener(UIInputListener listener) {
        if (!captureListeners.contains(listener, true)) captureListeners.add(listener);
    }

    public void removeCaptureListener(UIInputListener listener) {
        captureListeners.removeValue(listener, true);
    }

    @Override
    public void reset() {
        touchable = Touchable.ENABLED;
        focusable = false;
        listeners.clear();
        captureListeners.clear();
    }
}
