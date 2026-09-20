package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.*;
import com.badlogic.gdx.Application;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.widget.WidgetTypes;
import games.rednblack.editor.renderer.utils.TransformMathUtils;
import games.rednblack.editor.renderer.utils.ZSortComparator;

import java.util.Arrays;

@All(ButtonComponent.class)
public class ButtonSystem extends BaseEntitySystem {

    private final ZSortComparator zSortComparator = new ZSortComparator();

    protected ComponentMapper<ZIndexComponent> zIndexMapper;
    protected ComponentMapper<ButtonComponent> buttonComponentMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsComponentMapper;
    protected ComponentMapper<NodeComponent> nodeComponentMapper;
    protected ComponentMapper<MainItemComponent> mainItemComponentMapper;
    protected ComponentMapper<ViewPortComponent> viewPortComponentMapper;
    protected ComponentMapper<ZIndexComponent> zIndexComponentMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<ParentNodeComponent> parentMapper;
    protected ComponentMapper<WidgetComponent> widgetMapper;

    private final Vector2 tmp = new Vector2();

    /** Which pointers were down on the previous pass, to tell a fresh press from a held one. */
    private boolean[] pointerWasDown;
    /** Entity each pointer is pressing, -1 if none: a press belongs to the button it started on. */
    private int[] pointerOwner;
    private int pointerCount;

    public ButtonSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public ButtonSystem() {
    }

    @Override
    protected void initialize() {
        //How many pointers the device tracks never changes while running
        pointerCount = Gdx.input.getMaxPointers();
        pointerWasDown = new boolean[pointerCount];
        pointerOwner = new int[pointerCount];
        Arrays.fill(pointerOwner, -1);
    }

    @Override
    protected void removed(int entityId) {
        for (int pointer = 0; pointer < pointerCount; pointer++) {
            if (pointerOwner[pointer] == entityId) pointerOwner[pointer] = -1;
        }
    }

    @Override
    protected final void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        zSortComparator.setzIndexMapper(zIndexMapper);
        zSortComparator.quickSort(ids, actives.size());
        //Top most first: where buttons overlap, the one in front gets the press
        for (int i = actives.size() - 1; i >= 0; i--) {
            process(ids[i]);
        }

        for (int pointer = 0; pointer < pointerCount; pointer++) {
            pointerWasDown[pointer] = Gdx.input.isTouched(pointer);
        }
    }

    protected void process(int entity) {
        NodeComponent nodeComponent = nodeComponentMapper.get(entity);
        if (nodeComponent == null) return;

        WidgetComponent widget = widgetMapper.get(entity);

        ViewPortComponent camera = viewPortComponentMapper.get(entity);
        if (camera != null) {
            releaseSilently(entity, buttonComponentMapper.get(entity));

            //A widget being edited keeps the state chosen in the editor
            if (widget != null) return;

            //Override visibility when editing the button
            for (int i = 0; i < nodeComponent.children.size; i++) {
                Integer childEntity = nodeComponent.children.get(i);
                MainItemComponent childMainItemComponent = mainItemComponentMapper.get(childEntity);
                childMainItemComponent.visible = true;
            }
            return;
        }

        ButtonComponent buttonComponent = buttonComponentMapper.get(entity);

        if (!buttonComponent.isTouchEnabled) {
            releaseSilently(entity, buttonComponent);
            if (widget != null) updateWidgetState(entity, widget, buttonComponent);
            return;
        }

        boolean isTouched = updateTouch(entity, buttonComponent);
        boolean isChecked = buttonComponent.isChecked;

        if (widget != null) {
            //State driven button: the look of each state belongs to the widget parts
            buttonComponent.isHovered = buttonComponent.touchPointer == -1 && isHovered(entity);
            updateWidgetState(entity, widget, buttonComponent);
            return;
        }

        //Legacy button: swap the "normal" and "pressed" layers
        for (int i = 0; i < nodeComponent.children.size; i++) {
            Integer childEntity = nodeComponent.children.get(i);
            MainItemComponent childMainItemComponent = mainItemComponentMapper.get(childEntity);
            ZIndexComponent childZComponent = zIndexComponentMapper.get(childEntity);
            if (isTouched || isChecked) {
                if (childZComponent.getLayerName().equals("normal")) {
                    childMainItemComponent.visible = false;
                }
                if (childZComponent.getLayerName().equals("pressed")) {
                    childMainItemComponent.visible = true;
                }
            } else {
                if (childZComponent.getLayerName().equals("normal")) {
                    childMainItemComponent.visible = true;
                }
                if (childZComponent.getLayerName().equals("pressed")) {
                    childMainItemComponent.visible = false;
                }
            }
        }
    }

    /**
     * Picks the most relevant state the widget declares: disabled, pressed, checked, hover, default.
     */
    protected void updateWidgetState(int entity, WidgetComponent widget, ButtonComponent buttonComponent) {
        if (!buttonComponent.isTouchEnabled && widget.setState(WidgetTypes.STATE_DISABLED)) return;
        if (buttonComponent.isTouched && widget.setState(WidgetTypes.STATE_PRESSED)) return;
        if (buttonComponent.isChecked && widget.setState(WidgetTypes.STATE_CHECKED)) return;
        if (buttonComponent.isHovered && widget.setState(WidgetTypes.STATE_HOVER)) return;
        widget.currentState = null; //back to the default state
    }

    /**
     * Hovering only exists where there is a pointer that moves without touching.
     */
    private boolean isHovered(int entity) {
        Application.ApplicationType type = Gdx.app.getType();
        if (type != Application.ApplicationType.Desktop && type != Application.ApplicationType.WebGL) return false;
        //a pointer held down is dragging something, whatever it passes over is not hovered
        if (Gdx.input.isTouched()) return false;

        return isOver(entity, 0);
    }

    private boolean isOver(int entity, int pointer) {
        DimensionsComponent dimensionsComponent = dimensionsComponentMapper.get(entity);
        tmp.set(Gdx.input.getX(pointer), Gdx.input.getY(pointer));
        TransformMathUtils.globalToLocalCoordinates(entity, tmp, transformMapper, parentMapper, viewPortComponentMapper);
        return dimensionsComponent.hit(tmp.x, tmp.y);
    }

    /**
     * Follows the pointer pressing the button. A press is taken only when it starts over the button,
     * and then belongs to it until released: sliding out shows the button released, sliding back in
     * shows it pressed again, and letting go over it is a click. A press started elsewhere never
     * presses the button by sliding over it.
     *
     * @return true while the button is held down with the pointer over it
     */
    private boolean updateTouch(int entity, ButtonComponent buttonComponent) {
        int pointer = buttonComponent.touchPointer;

        if (pointer != -1 && pointerOwner[pointer] != entity) {
            //lost track of the pointer (component reused by another entity): start over
            releaseSilently(entity, buttonComponent);
            pointer = -1;
        }

        if (pointer == -1) {
            for (int p = 0; p < pointerCount; p++) {
                boolean justPressed = Gdx.input.isTouched(p) && !pointerWasDown[p];
                if (!justPressed || pointerOwner[p] != -1 || !isOver(entity, p)) continue;

                pointerOwner[p] = entity;
                buttonComponent.touchPointer = p;
                buttonComponent.isTouched = true;
                for (int i = 0; i < buttonComponent.listeners.size; i++) {
                    buttonComponent.listeners.get(i).touchDown(entity);
                }
                break;
            }
            return buttonComponent.isTouched;
        }

        boolean over = isOver(entity, pointer);
        if (Gdx.input.isTouched(pointer)) {
            buttonComponent.isTouched = over;
            return over;
        }

        //released
        pointerOwner[pointer] = -1;
        buttonComponent.touchPointer = -1;
        buttonComponent.isTouched = false;
        for (int i = 0; i < buttonComponent.listeners.size; i++) {
            buttonComponent.listeners.get(i).touchUp(entity);
            if (over) buttonComponent.listeners.get(i).clicked(entity);
        }
        return false;
    }

    /** Drops the press the button may be holding without telling the listeners. */
    private void releaseSilently(int entity, ButtonComponent buttonComponent) {
        if (buttonComponent == null) return;
        int pointer = buttonComponent.touchPointer;
        //only a press this entity really owns: a reused component may name somebody else's pointer
        if (pointer != -1 && pointerOwner[pointer] == entity) pointerOwner[pointer] = -1;
        buttonComponent.touchPointer = -1;
        buttonComponent.isTouched = false;
        buttonComponent.isHovered = false;
    }

    /**
     * Sets the pressed flag by hand, firing the listeners like a real press or release would.
     * The system itself tracks pointers and does not go through here.
     */
    public void setTouchState(ButtonComponent buttonComponent, boolean isTouched, int entity) {
        if (!buttonComponent.isTouched && isTouched) {
            for (int i = 0; i < buttonComponent.listeners.size; i++) {
                buttonComponent.listeners.get(i).touchDown(entity);
            }
        }
        if (buttonComponent.isTouched && !isTouched) {
            DimensionsComponent dimensionsComponent = dimensionsComponentMapper.get(entity);
            tmp.set(Gdx.input.getX(), Gdx.input.getY());
            TransformMathUtils.globalToLocalCoordinates(entity, tmp, transformMapper, parentMapper, viewPortComponentMapper);
            boolean hitEntity = dimensionsComponent.hit(tmp.x, tmp.y);

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
