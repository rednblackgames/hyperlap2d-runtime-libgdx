package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.widget.WidgetType;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * Turns the input reaching a button into the flags its look follows: pressed, hovered and checked.
 * A button that is not a widget keeps all of it but the look, which is its own business.
 *
 * The system listens to every button itself, so a press belongs to the button it started on and
 * stays with it until the pointer is released, wherever it travels meanwhile: sliding out shows the
 * button released, sliding back in shows it pressed again, and letting go over it is a click.
 */
@All(ButtonComponent.class)
public class ButtonSystem extends BaseEntitySystem implements UIInputListener {

    protected ComponentMapper<ButtonComponent> buttonComponentMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsComponentMapper;
    protected ComponentMapper<NodeComponent> nodeComponentMapper;
    protected ComponentMapper<MainItemComponent> mainItemComponentMapper;
    protected ComponentMapper<ViewPortComponent> viewPortComponentMapper;
    protected ComponentMapper<ParentNodeComponent> parentMapper;
    protected ComponentMapper<WidgetComponent> widgetMapper;
    protected ComponentMapper<InputTargetComponent> inputTargetMapper;

    /** Wired by the engine: a scene with buttons needs the input system to feed them. */
    protected UIInputSystem inputSystem;

    public ButtonSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public ButtonSystem() {
    }

    /** Makes the button an input target, so the pointer can find it, and listens to it. */
    @Override
    protected void inserted(int entityId) {
        inputTargetMapper.create(entityId).addListener(this);
    }

    @Override
    protected void removed(int entityId) {
        InputTargetComponent target = inputTargetMapper.get(entityId);
        if (target != null) target.removeListener(this);
        if (inputSystem != null) inputSystem.cancelTouchFocus(entityId);
    }

    @Override
    protected final void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        for (int i = 0, s = actives.size(); i < s; i++) {
            process(ids[i]);
        }
    }

    protected void process(int entity) {
        WidgetComponent widget = widgetMapper.get(entity);
        ButtonComponent buttonComponent = buttonComponentMapper.get(entity);

        //the composite being viewed from the inside in the editor is opened, not pressed
        if (viewPortComponentMapper.has(entity)) {
            releaseSilently(entity, buttonComponent);
            return;
        }

        if (!buttonComponent.isTouchEnabled) {
            releaseSilently(entity, buttonComponent);
            if (widget != null) updateWidgetState(entity, widget, buttonComponent);
            return;
        }

        //A button without a widget is input alone: the listeners and the checked flag, no look
        if (widget == null) return;

        followCheckedSetting(widget, buttonComponent);
        //State driven button: the look of each state belongs to the widget parts
        updateWidgetState(entity, widget, buttonComponent);
    }

    // -------------------------------------------------------------- the pointer

    @Override
    public void touchDown(UIEvent event) {
        int entity = event.listenerEntity;
        ButtonComponent buttonComponent = buttonComponentMapper.get(entity);
        if (buttonComponent == null || !buttonComponent.isTouchEnabled) return;
        //the button being viewed from the inside in the editor is not pressed, it is opened
        if (viewPortComponentMapper.has(entity)) return;
        //a second pointer does not press a button that is already held
        if (buttonComponent.touchPointer != -1) return;

        buttonComponent.touchPointer = event.pointer;
        buttonComponent.isTouched = true;
        for (int i = 0; i < buttonComponent.listeners.size; i++) {
            buttonComponent.listeners.get(i).touchDown(entity);
        }
        event.handle();
    }

    @Override
    public void touchDragged(UIEvent event) {
        int entity = event.listenerEntity;
        ButtonComponent buttonComponent = buttonComponentMapper.get(entity);
        if (buttonComponent == null || buttonComponent.touchPointer != event.pointer) return;

        buttonComponent.isTouched = isOver(entity, event);
    }

    @Override
    public void touchUp(UIEvent event) {
        int entity = event.listenerEntity;
        ButtonComponent buttonComponent = buttonComponentMapper.get(entity);
        if (buttonComponent == null || buttonComponent.touchPointer != event.pointer) return;

        boolean over = !event.cancelled && isOver(entity, event);
        buttonComponent.touchPointer = -1;
        buttonComponent.isTouched = false;

        for (int i = 0; i < buttonComponent.listeners.size; i++) {
            buttonComponent.listeners.get(i).touchUp(entity);
            if (over) buttonComponent.listeners.get(i).clicked(entity);
        }
        if (over) toggle(entity, buttonComponent);
    }

    @Override
    public void enter(UIEvent event) {
        ButtonComponent buttonComponent = buttonComponentMapper.get(event.listenerEntity);
        if (buttonComponent != null) buttonComponent.isHovered = true;
    }

    @Override
    public void exit(UIEvent event) {
        ButtonComponent buttonComponent = buttonComponentMapper.get(event.listenerEntity);
        if (buttonComponent != null) buttonComponent.isHovered = false;
    }

    private boolean isOver(int entity, UIEvent event) {
        DimensionsComponent dimensionsComponent = dimensionsComponentMapper.get(entity);
        return dimensionsComponent != null && dimensionsComponent.hit(event.localX, event.localY);
    }

    // ----------------------------------------------------------------- the look

    /**
     * Picks the most relevant state the widget declares: disabled, pressed, checked, hover, default.
     */
    protected void updateWidgetState(int entity, WidgetComponent widget, ButtonComponent buttonComponent) {
        boolean checked = buttonComponent.isChecked;

        // A checked widget prefers the checked variant of a state, where it declares one: a plain
        // button has none, so a press still beats checked and checked still beats hover for it.
        if (!buttonComponent.isTouchEnabled) {
            if (checked && widget.setState(WidgetTypes.STATE_CHECKED_DISABLED)) return;
            if (widget.setState(WidgetTypes.STATE_DISABLED)) return;
        }
        if (buttonComponent.isTouched) {
            if (checked && widget.setState(WidgetTypes.STATE_CHECKED_PRESSED)) return;
            if (widget.setState(WidgetTypes.STATE_PRESSED)) return;
        }
        if (checked) {
            if (buttonComponent.isHovered && widget.setState(WidgetTypes.STATE_CHECKED_HOVER)) return;
            if (widget.setState(WidgetTypes.STATE_CHECKED)) return;
        }
        if (buttonComponent.isHovered && widget.setState(WidgetTypes.STATE_HOVER)) return;
        widget.currentState = null; //back to the default state
    }

    private boolean isCheckable(WidgetComponent widget) {
        WidgetType type = widget == null ? null : WidgetTypes.get(widget.widgetType);
        return type != null && type.checkable;
    }

    /**
     * The checked setting is where a checkable widget starts, and what the editor shows: a change of
     * it is followed, while a click in between is left alone.
     */
    private void followCheckedSetting(WidgetComponent widget, ButtonComponent buttonComponent) {
        if (!isCheckable(widget)) return;

        String setting = widget.properties.get(WidgetTypes.PROPERTY_CHECKED);
        if (setting == null ? buttonComponent.checkedSetting == null : setting.equals(buttonComponent.checkedSetting)) return;

        buttonComponent.checkedSetting = setting;
        buttonComponent.isChecked = Boolean.parseBoolean(setting);
    }

    /**
     * A click on a checkable widget flips it. In a radio group, the buttons sharing the group name
     * among its siblings, it checks this one and unchecks the others, and a checked one stays checked.
     */
    private void toggle(int entity, ButtonComponent buttonComponent) {
        WidgetComponent widget = widgetMapper.get(entity);
        if (!isCheckable(widget)) return;

        String group = widget.properties.get(WidgetTypes.PROPERTY_GROUP, "");
        if (group.isEmpty()) {
            setChecked(entity, buttonComponent, !buttonComponent.isChecked);
            return;
        }
        if (buttonComponent.isChecked) return;

        ParentNodeComponent parentNode = parentMapper.get(entity);
        NodeComponent siblings = parentNode == null ? null : nodeComponentMapper.get(parentNode.parentEntity);
        if (siblings != null) {
            for (int i = 0; i < siblings.children.size; i++) {
                int sibling = siblings.children.get(i);
                if (sibling == entity) continue;

                WidgetComponent siblingWidget = widgetMapper.get(sibling);
                ButtonComponent siblingButton = buttonComponentMapper.get(sibling);
                if (siblingButton == null || !isCheckable(siblingWidget)) continue;
                if (group.equals(siblingWidget.properties.get(WidgetTypes.PROPERTY_GROUP, ""))) {
                    setChecked(sibling, siblingButton, false);
                }
            }
        }
        setChecked(entity, buttonComponent, true);
    }

    private void setChecked(int entity, ButtonComponent buttonComponent, boolean checked) {
        if (buttonComponent.isChecked == checked) return;

        buttonComponent.isChecked = checked;
        for (int i = 0; i < buttonComponent.listeners.size; i++) {
            buttonComponent.listeners.get(i).checkedChanged(entity, checked);
        }
    }

    /** Drops the press the button may be holding without telling the listeners. */
    private void releaseSilently(int entity, ButtonComponent buttonComponent) {
        if (buttonComponent == null) return;
        if (buttonComponent.touchPointer != -1 && inputSystem != null) inputSystem.cancelTouchFocus(entity);
        buttonComponent.touchPointer = -1;
        buttonComponent.isTouched = false;
        buttonComponent.isHovered = false;
    }

    /**
     * Sets the pressed flag by hand, firing the listeners like a real press or release would.
     * The system itself follows the pointer and does not go through here.
     */
    public void setTouchState(ButtonComponent buttonComponent, boolean isTouched, int entity) {
        if (!buttonComponent.isTouched && isTouched) {
            for (int i = 0; i < buttonComponent.listeners.size; i++) {
                buttonComponent.listeners.get(i).touchDown(entity);
            }
        }
        if (buttonComponent.isTouched && !isTouched) {
            boolean hitEntity = inputSystem != null
                    && inputSystem.isOver(entity, inputSystem.getMouseScreenX(), inputSystem.getMouseScreenY());

            for (int i = 0; i < buttonComponent.listeners.size; i++) {
                buttonComponent.listeners.get(i).touchUp(entity);

                if (hitEntity) {
                    buttonComponent.listeners.get(i).clicked(entity);
                }
            }
        }
        buttonComponent.isTouched = isTouched;
    }
}
