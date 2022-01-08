package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.systems.action.data.SizeToData;

/**
 * Created by ZeppLondon on 10/28/15.
 */
public class SizeToAction <T extends SizeToData> extends TemporalAction<T>  {

    @Override
    protected void update(float percent, int entity, T actionData) {
        DimensionsComponent dimensionsComponent = dimensionsMapper.get(entity);
        if (dimensionsComponent == null) return;

        dimensionsComponent.width = actionData.startWidth + (actionData.endHeight - actionData.startWidth) * percent;
        dimensionsComponent.height = actionData.startHeight + (actionData.endHeight - actionData.startHeight) * percent;
    }

    @Override
    public void begin(int entity, T actionData) {
        DimensionsComponent dimensionsComponent = dimensionsMapper.get(entity);
        if (dimensionsComponent == null) return;

        actionData.startWidth = dimensionsComponent.width;
        actionData.startHeight = dimensionsComponent.height;
    }
}
