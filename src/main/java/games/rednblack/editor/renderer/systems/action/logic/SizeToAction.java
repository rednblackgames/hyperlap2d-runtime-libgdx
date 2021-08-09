package games.rednblack.editor.renderer.systems.action.logic;

import com.artemis.ComponentMapper;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.systems.action.data.SizeToData;

/**
 * Created by ZeppLondon on 10/28/15.
 */
public class SizeToAction <T extends SizeToData> extends TemporalAction<T>  {
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;

    @Override
    protected void update(float percent, int entity, T actionData) {
        DimensionsComponent dimensionsComponent = dimensionsMapper.get(entity);
        dimensionsComponent.width = actionData.startWidth + (actionData.endHeight - actionData.startWidth) * percent;
        dimensionsComponent.height = actionData.startHeight + (actionData.endHeight - actionData.startHeight) * percent;
    }

    @Override
    public void begin(int entity, T actionData) {
        DimensionsComponent dimensionsComponent = dimensionsMapper.get(entity);
        actionData.startWidth = dimensionsComponent.width;
        actionData.startHeight = dimensionsComponent.height;
    }
}
