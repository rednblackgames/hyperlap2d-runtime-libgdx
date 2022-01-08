package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.ScaleToData;

/**
 * Created by ZeppLondon on 10/28/15.
 */
public class ScaleToAction<T extends ScaleToData> extends TemporalAction<T> {

    @Override
    protected void update(float percent, int entity, T actionData) {
        TransformComponent transformComponent = transformMapper.get(entity);
        if (transformComponent == null) return;

        transformComponent.scaleX = actionData.startX + (actionData.endX - actionData.startX) * percent;
        transformComponent.scaleY = actionData.startY + (actionData.endY - actionData.startY) * percent;
    }

    @Override
    public void begin(int entity, T actionData) {
        TransformComponent transformComponent = transformMapper.get(entity);
        if (transformComponent == null) return;

        actionData.startX = transformComponent.scaleX;
        actionData.startY = transformComponent.scaleY;
    }
}
