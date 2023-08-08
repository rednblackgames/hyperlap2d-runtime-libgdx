package games.rednblack.editor.renderer.systems.action.logic.physics;

import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.systems.action.data.physics.ForceData;
import games.rednblack.editor.renderer.systems.action.logic.ComponentAction;
import games.rednblack.editor.renderer.utils.ForceUtils;

/**
 * Created by aurel on 19/02/16.
 */
public class ForceAction extends ComponentAction<ForceData> {

    @Override
    protected boolean delegate(float delta, int entity, ForceData actionData) {
        PhysicsBodyComponent physicsBodyComponent = physicsBodyMapper.get(entity);
        if (physicsBodyComponent == null) return true;

        ForceUtils.applyForce(actionData.force, physicsBodyComponent.body, actionData.relativePoint);
        return false;
    }
}
