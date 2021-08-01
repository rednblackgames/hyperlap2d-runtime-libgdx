package games.rednblack.editor.renderer.systems.action.logic;

import com.artemis.BaseComponentMapper;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.systems.action.data.ForceData;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.ForceUtils;

/**
 * Created by aurel on 19/02/16.
 */
public class ForceAction extends ComponentAction<ForceData> {

    private BaseComponentMapper<PhysicsBodyComponent> physicsBodyComponentMapper;

    public ForceAction() {
        this.physicsBodyComponentMapper = ComponentRetriever.getMapper(PhysicsBodyComponent.class);
    }

    @Override
    protected boolean delegate(float delta, int entity, ForceData actionData) {
        if (physicsBodyComponentMapper.has(entity)) {
            PhysicsBodyComponent physicsBodyComponent = physicsBodyComponentMapper.get(entity);

            ForceUtils.applyForce(actionData.force, physicsBodyComponent.body, actionData.relativePoint);
            return false;
        }

        return true;
    }
}
