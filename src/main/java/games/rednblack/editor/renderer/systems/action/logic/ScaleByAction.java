package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.ScaleByData;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

/**
 * Created by ZeppLondon on 10/28/15.
 */
public class ScaleByAction<T extends ScaleByData> extends RelativeTemporalAction<T> {
    @Override
    protected void updateRelative(float percent, int entity, T actionData) {
        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class);
        transformComponent.scaleX += actionData.amountX * percent;
        transformComponent.scaleY += actionData.amountY * percent;
    }
}
