package games.rednblack.editor.renderer.components.additional;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import com.badlogic.gdx.utils.Array;

@Transient
public class ButtonComponent extends PooledComponent {

    /** True while the button is held down with the pointer over it: what the pressed look follows. */
    public boolean isTouched = false;
    /** Pointer that pressed the button and is still down, -1 if none. It may have left the button. */
    public int touchPointer = -1;
    public boolean isHovered = false;
    public boolean isChecked = false;
    public boolean isTouchEnabled = true;

    public final Array<ButtonListener> listeners = new Array<>();

    public interface ButtonListener {
        void touchUp(int entity);

        void touchDown(int entity);

        void clicked(int entity);
    }

    public void addListener(ButtonListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ButtonListener listener) {
        listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void reset() {
        isTouched = false;
        touchPointer = -1;
        isHovered = false;
        isChecked = false;
        isTouchEnabled = true;
        listeners.clear();
    }
}
