package games.rednblack.editor.renderer.systems.action.logic;

import com.artemis.ComponentMapper;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.systems.action.data.RotateByData;

/**
 * Created by ZeppLondon on 10/16/2015.
 */
public class RotateByAction<T extends RotateByData> extends RelativeTemporalAction<T> {
    protected ComponentMapper<TransformComponent> transformMapper;

    @Override
    protected void updateRelative(float percentDelta, int entity, T actionData) {
        TransformComponent transformComponent = transformMapper.get(entity);
        transformComponent.rotation += actionData.amount * percentDelta;
    }
}
