package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

/**
 * Created by azakhary on 8/1/2015.
 */
@All(ButtonComponent.class)
public class ButtonSystem extends IteratingSystem {

    protected ComponentMapper<NodeComponent> componentMapper;

    @Override
    protected void process(int entity) {
        NodeComponent nodeComponent = componentMapper.get(entity);

        if(nodeComponent == null) return;

        for (int i = 0; i < nodeComponent.children.size; i++) {
            Integer childEntity = nodeComponent.children.get(i);
            MainItemComponent childMainItemComponent = ComponentRetriever.get(childEntity, MainItemComponent.class);
            childMainItemComponent.visible = true;
        }

        ViewPortComponent camera = ComponentRetriever.get(entity, ViewPortComponent.class);
        if(camera != null) {
            // if camera is on this entity, then it should not be processed
            return;
        }


        for (int i = 0; i < nodeComponent.children.size; i++) {
            Integer childEntity = nodeComponent.children.get(i);
            MainItemComponent childMainItemComponent = ComponentRetriever.get(childEntity, MainItemComponent.class);
            ZIndexComponent childZComponent = ComponentRetriever.get(childEntity, ZIndexComponent.class);
            if(isTouched(entity)) {
                if(childZComponent.layerName.equals("normal")) {
                    childMainItemComponent.visible = false;
                }
                if(childZComponent.layerName.equals("pressed")) {
                    childMainItemComponent.visible = true;
                }
            } else {
                if(childZComponent.layerName.equals("normal")) {
                    childMainItemComponent.visible = true;
                }
                if(childZComponent.layerName.equals("pressed")) {
                    childMainItemComponent.visible = false;
                }
            }
        }

    }

    private boolean isTouched(Integer entity) {
        ButtonComponent buttonComponent = ComponentRetriever.get(entity, ButtonComponent.class);
        if(Gdx.input.isTouched()) {
            DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class);
            Vector2 localCoordinates  = new Vector2(Gdx.input.getX(), Gdx.input.getY());

            TransformMathUtils.globalToLocalCoordinates(entity, localCoordinates);

            if(dimensionsComponent.hit(localCoordinates.x, localCoordinates.y)) {
                buttonComponent.setTouchState(true);
                return true;
            }
        }
        buttonComponent.setTouchState(false);
        return false;
    }
}
