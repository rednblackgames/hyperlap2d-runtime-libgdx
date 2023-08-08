package games.rednblack.editor.renderer.systems.action.logic.physics;

import com.badlogic.gdx.physics.box2d.Transform;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.systems.action.data.physics.TransformByData;
import games.rednblack.editor.renderer.systems.action.logic.RelativeTemporalAction;

public class TransformByAction<T extends TransformByData> extends RelativeTemporalAction<T> {

    @Override
    protected void updateRelative(float percentDelta, int entity, T actionData) {
        PhysicsBodyComponent physicsBodyComponent = physicsBodyMapper.get(entity);
        if (physicsBodyComponent == null || physicsBodyComponent.body == null) return;

        float amountX = actionData.amountX*percentDelta;
        float amountY = actionData.amountY*percentDelta;
        float amountAngle = actionData.amountAngle*percentDelta;

        Transform transform = physicsBodyComponent.body.getTransform();
        float x = transform.getPosition().x + amountX;
        float y = transform.getPosition().y + amountY;
        float angle = transform.getRotation() + amountAngle;

        physicsBodyComponent.body.setTransform(x, y, angle);
    }
}
