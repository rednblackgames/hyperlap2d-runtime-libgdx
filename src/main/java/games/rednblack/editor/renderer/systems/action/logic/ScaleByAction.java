package games.rednblack.editor.renderer.systems.action.logic;

import com.artemis.ComponentMapper;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.ScaleByData;

/**
 * Created by ZeppLondon on 10/28/15.
 */
public class ScaleByAction<T extends ScaleByData> extends RelativeTemporalAction<T> {
    protected ComponentMapper<TransformComponent> transformMapper;

    @Override
    protected void updateRelative(float percent, int entity, T actionData) {
        TransformComponent transformComponent = transformMapper.get(entity);
        transformComponent.scaleX += actionData.amountX * percent;
        transformComponent.scaleY += actionData.amountY * percent;
    }
}
