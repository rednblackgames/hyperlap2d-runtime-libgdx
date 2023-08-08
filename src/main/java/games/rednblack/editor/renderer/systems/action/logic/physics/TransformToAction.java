package games.rednblack.editor.renderer.systems.action.logic.physics;

import com.badlogic.gdx.physics.box2d.Transform;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.systems.action.data.physics.TransformToData;
import games.rednblack.editor.renderer.systems.action.logic.TemporalAction;

public class TransformToAction<T extends TransformToData> extends TemporalAction<T> {

    @Override
    public void update(float percent, int entity, T actionData) {
        PhysicsBodyComponent physicsBodyComponent = physicsBodyMapper.get(entity);
        if (physicsBodyComponent == null || physicsBodyComponent.body == null) return;

        float x = actionData.startX + (actionData.endX - actionData.startX) * percent;
        float y = actionData.startY + (actionData.endY - actionData.startY) * percent;
        float angle = actionData.startAngle + (actionData.endAngle - actionData.startAngle) * percent;

        physicsBodyComponent.body.setTransform(x, y, angle);
    }

    @Override
    public void begin(int entity, T actionData) {
        PhysicsBodyComponent physicsBodyComponent = physicsBodyMapper.get(entity);
        if (physicsBodyComponent == null || physicsBodyComponent.body == null) return;

        Transform transform = physicsBodyComponent.body.getTransform();
        actionData.startX = transform.getPosition().x;
        actionData.startY = transform.getPosition().y;
        actionData.startAngle = transform.getRotation();
    }

    @Override
    public void end(int entity, TransformToData actionData) {

    }
}
