package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.systems.action.data.SizeByData;

/**
 * Created by ZeppLondon on 10/28/15.
 */
public class SizeByAction<T extends SizeByData> extends RelativeTemporalAction<T> {

    @Override
    protected void updateRelative(float percentDelta, int entity, T actionData) {
        DimensionsComponent dimensionsComponent = dimensionsMapper.get(entity);
        if (dimensionsComponent == null) return;

        dimensionsComponent.width += actionData.amountWidth * percentDelta;
        dimensionsComponent.height += actionData.amountHeight * percentDelta;
    }
}
