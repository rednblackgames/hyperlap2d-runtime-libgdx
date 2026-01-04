package games.rednblack.editor.renderer.components.additional;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import com.badlogic.gdx.utils.Array;

@Transient
public class ButtonComponent extends PooledComponent {

    public boolean isTouched = false;
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
        isChecked = false;
        isTouchEnabled = true;
        listeners.clear();
    }
}
