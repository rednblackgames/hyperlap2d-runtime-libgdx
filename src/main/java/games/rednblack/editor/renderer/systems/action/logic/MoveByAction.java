package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.MoveByData;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

/**
 * Created by ZeppLondon on 10/15/2015.
 */
public class MoveByAction<T extends MoveByData> extends RelativeTemporalAction<T> {
    @Override
    protected void updateRelative(float percentDelta, int entity, T actionData) {
        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class);

        float amountX = actionData.amountX*percentDelta;
        float amountY = actionData.amountY*percentDelta;

        transformComponent.x += amountX;
        transformComponent.y += amountY;
    }
}
