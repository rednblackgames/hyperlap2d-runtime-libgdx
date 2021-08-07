package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.RotateByData;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

/**
 * Created by ZeppLondon on 10/16/2015.
 */
public class RotateByAction<T extends RotateByData> extends RelativeTemporalAction<T> {
    @Override
    protected void updateRelative(float percentDelta, int entity, T actionData) {
        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class, engine);
        transformComponent.rotation += actionData.amount * percentDelta;
    }
}
