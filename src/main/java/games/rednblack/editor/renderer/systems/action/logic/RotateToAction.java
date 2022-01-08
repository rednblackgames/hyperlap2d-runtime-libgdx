package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.RotateToData;

/**
 * Created by ZeppLondon on 10/16/2015.
 */
public class RotateToAction<T extends RotateToData> extends TemporalAction<T> {

    @Override
    protected void update(float percent, int entity, T actionData) {
        TransformComponent transformComponent = transformMapper.get(entity);
        if (transformComponent == null) return;

        transformComponent.rotation = (actionData.start + (actionData.end - actionData.start) * percent);
    }

    @Override
    public void begin(int entity, T actionData) {
        TransformComponent transformComponent = transformMapper.get(entity);
        if (transformComponent == null) return;

        actionData.start = transformComponent.rotation;
    }
}
