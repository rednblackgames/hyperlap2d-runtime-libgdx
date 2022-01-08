package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.systems.action.data.AlphaData;

/**
 * Created by ZeppLondon on 10/29/15.
 */
public class AlphaAction<T extends AlphaData> extends TemporalAction<T> {
    @Override
    protected void update(float percent, int entity, T actionData) {
        TintComponent tintComponent = tintMapper.get(entity);
        if (tintComponent == null) return;

        tintComponent.color.a = actionData.start + (actionData.end - actionData.start) * percent;
    }

    @Override
    public void begin(int entity, T actionData) {
        TintComponent tintComponent = tintMapper.get(entity);
        if (tintComponent == null) return;

        actionData.start = tintComponent.color.a;
    }
}
