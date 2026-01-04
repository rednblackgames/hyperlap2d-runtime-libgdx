package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;
import games.rednblack.editor.renderer.utils.ZSortComparator;

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

    private final Vector2 tmp = new Vector2();

    private int inputHoldEntity = -1;

    public ButtonSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public ButtonSystem() {
    }

    @Override
    protected final void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        zSortComparator.setzIndexMapper(zIndexMapper);
        zSortComparator.quickSort(ids, actives.size());
        for (int i = actives.size() - 1; i >= 0; i--) {
            process(ids[i]);
        }
    }

    protected void process(int entity) {
        NodeComponent nodeComponent = nodeComponentMapper.get(entity);
        if (nodeComponent == null) return;

        ViewPortComponent camera = viewPortComponentMapper.get(entity);
        if (camera != null) {
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
            inputHoldEntity = -1;
            return;
        }

        //Check if another input has acquired click focus
        if ((inputHoldEntity != entity && inputHoldEntity != -1)) return;

        boolean isTouched = isTouched(entity, buttonComponent);
        boolean isChecked = buttonComponent.isChecked;
        for (int i = 0; i < nodeComponent.children.size; i++) {
            Integer childEntity = nodeComponent.children.get(i);
            MainItemComponent childMainItemComponent = mainItemComponentMapper.get(childEntity);
            ZIndexComponent childZComponent = zIndexComponentMapper.get(childEntity);
            if (isTouched) {
                inputHoldEntity = entity;
            } else {
                inputHoldEntity = -1;
            }

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

    private boolean isTouched(int entity, ButtonComponent buttonComponent) {
        if (Gdx.input.isTouched()) {
            DimensionsComponent dimensionsComponent = dimensionsComponentMapper.get(entity);
            tmp.set(Gdx.input.getX(), Gdx.input.getY());

            TransformMathUtils.globalToLocalCoordinates(entity, tmp, transformMapper, parentMapper, viewPortComponentMapper);

            if (dimensionsComponent.hit(tmp.x, tmp.y)) {
                setTouchState(buttonComponent, true, entity);
                return true;
            }
        }
        setTouchState(buttonComponent, false, entity);
        return false;
    }

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
