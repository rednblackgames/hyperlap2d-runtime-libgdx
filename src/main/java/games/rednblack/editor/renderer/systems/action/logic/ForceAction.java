package games.rednblack.editor.renderer.systems.action.logic;

import com.artemis.ComponentMapper;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.systems.action.data.ForceData;
import games.rednblack.editor.renderer.utils.ForceUtils;

/**
 * Created by aurel on 19/02/16.
 */
public class ForceAction extends ComponentAction<ForceData> {
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyMapper;

    @Override
    protected boolean delegate(float delta, int entity, ForceData actionData) {
        PhysicsBodyComponent physicsBodyComponent = physicsBodyMapper.get(entity);
        if (physicsBodyComponent != null) {
            ForceUtils.applyForce(actionData.force, physicsBodyComponent.body, actionData.relativePoint);
            return false;
        }

        return true;
    }
}
