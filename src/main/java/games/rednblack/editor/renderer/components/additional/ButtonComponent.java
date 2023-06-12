package games.rednblack.editor.renderer.components.additional;

import com.artemis.PooledComponent;
import com.artemis.annotations.Transient;
import com.badlogic.gdx.utils.Array;

/**
 * Created by azakhary on 8/1/2015.
 */
@Transient
public class ButtonComponent extends PooledComponent {

    public boolean isTouched = false;
    public boolean isChecked = false;

    private final Array<ButtonListener> listeners = new Array<>();

    public interface ButtonListener {
        void touchUp();

        void touchDown();

        void clicked();
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

    public void setTouchState(boolean isTouched) {
        if (!this.isTouched && isTouched) {
            for (int i = 0; i < listeners.size; i++) {
                listeners.get(i).touchDown();
            }
        }
        if (this.isTouched && !isTouched) {
            for (int i = 0; i < listeners.size; i++) {
                listeners.get(i).touchUp();
                listeners.get(i).clicked();
            }
        }
        this.isTouched = isTouched;
    }

    @Override
    public void reset() {
        isTouched = false;
        listeners.clear();
    }
}
