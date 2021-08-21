package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

/**
 * Created by azakhary on 8/1/2015.
 */
@All(ButtonComponent.class)
public class ButtonSystem extends IteratingSystem {

    protected ComponentMapper<ButtonComponent> buttonComponentMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsComponentMapper;
    protected ComponentMapper<NodeComponent> nodeComponentMapper;
    protected ComponentMapper<MainItemComponent> mainItemComponentMapper;
    protected ComponentMapper<ViewPortComponent> viewPortComponentMapper;
    protected ComponentMapper<ZIndexComponent> zIndexComponentMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<ParentNodeComponent> parentMapper;

    private final Vector2 tmp = new Vector2();

    @Override
    protected void process(int entity) {
        NodeComponent nodeComponent = nodeComponentMapper.get(entity);

        if (nodeComponent == null) return;

        for (int i = 0; i < nodeComponent.children.size; i++) {
            Integer childEntity = nodeComponent.children.get(i);
            MainItemComponent childMainItemComponent = mainItemComponentMapper.get(childEntity);
            childMainItemComponent.visible = true;
        }

        ViewPortComponent camera = viewPortComponentMapper.get(entity);
        if (camera != null) {
            // if camera is on this entity, then it should not be processed
            return;
        }


        for (int i = 0; i < nodeComponent.children.size; i++) {
            Integer childEntity = nodeComponent.children.get(i);
            MainItemComponent childMainItemComponent = mainItemComponentMapper.get(childEntity);
            ZIndexComponent childZComponent = zIndexComponentMapper.get(childEntity);
            if (isTouched(entity)) {
                if (childZComponent.layerName.equals("normal")) {
                    childMainItemComponent.visible = false;
                }
                if (childZComponent.layerName.equals("pressed")) {
                    childMainItemComponent.visible = true;
                }
            } else {
                if (childZComponent.layerName.equals("normal")) {
                    childMainItemComponent.visible = true;
                }
                if (childZComponent.layerName.equals("pressed")) {
                    childMainItemComponent.visible = false;
                }
            }
        }

    }

    private boolean isTouched(Integer entity) {
        ButtonComponent buttonComponent = buttonComponentMapper.get(entity);
        if (Gdx.input.isTouched()) {
            DimensionsComponent dimensionsComponent = dimensionsComponentMapper.get(entity);
            tmp.set(Gdx.input.getX(), Gdx.input.getY());

            TransformMathUtils.globalToLocalCoordinates(entity, tmp, transformMapper, parentMapper, viewPortComponentMapper);

            if (dimensionsComponent.hit(tmp.x, tmp.y)) {
                buttonComponent.setTouchState(true);
                return true;
            }
        }
        buttonComponent.setTouchState(false);
        return false;
    }
}
