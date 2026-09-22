package games.rednblack.editor.renderer.components.additional;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.widget.WidgetBehaviour;
import games.rednblack.editor.renderer.widget.WidgetType;
import games.rednblack.editor.renderer.widget.WidgetTypes;

@Transient
public class ButtonComponent extends PooledComponent implements WidgetBehaviour {

    /** True while the button is held down with the pointer over it: what the pressed look follows. */
    public boolean isTouched = false;
    /** Pointer that pressed the button and is still down, -1 if none. It may have left the button. */
    public int touchPointer = -1;
    public boolean isHovered = false;
    /** Last seen value of the checked setting, so a change of setting is followed but a click is not undone. */
    public String checkedSetting = null;
    public boolean isChecked = false;
    public boolean isTouchEnabled = true;

    public final Array<ButtonListener> listeners = new Array<>();

    public interface ButtonListener {
        void touchUp(int entity);

        void touchDown(int entity);

        void clicked(int entity);

        /** Called when a click, or another button of its radio group, checks or unchecks it. */
        default void checkedChanged(int entity, boolean checked) {
        }
    }

    /** A checkable widget starts checked or not after its setting. */
    @Override
    public void initialise(WidgetComponent widget) {
        WidgetType type = WidgetTypes.get(widget.widgetType);
        if (type == null || !type.checkable) return;

        checkedSetting = widget.properties.get(WidgetTypes.PROPERTY_CHECKED);
        isChecked = Boolean.parseBoolean(checkedSetting);
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
        checkedSetting = null;
        isChecked = false;
        isTouchEnabled = true;
        listeners.clear();
    }
}
