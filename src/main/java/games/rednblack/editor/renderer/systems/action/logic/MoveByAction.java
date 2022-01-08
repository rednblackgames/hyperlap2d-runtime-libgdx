package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.MoveByData;

/**
 * Created by ZeppLondon on 10/15/2015.
 */
public class MoveByAction<T extends MoveByData> extends RelativeTemporalAction<T> {

    @Override
    protected void updateRelative(float percentDelta, int entity, T actionData) {
        TransformComponent transformComponent = transformMapper.get(entity);
        if (transformComponent == null) return;

        float amountX = actionData.amountX*percentDelta;
        float amountY = actionData.amountY*percentDelta;

        transformComponent.x += amountX;
        transformComponent.y += amountY;
    }
}
