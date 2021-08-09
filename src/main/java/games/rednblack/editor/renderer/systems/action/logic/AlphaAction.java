package games.rednblack.editor.renderer.systems.action.logic;

import com.artemis.ComponentMapper;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.systems.action.data.AlphaData;

/**
 * Created by ZeppLondon on 10/29/15.
 */
public class AlphaAction<T extends AlphaData> extends TemporalAction<T> {
    protected ComponentMapper<TintComponent> tintMapper;

    @Override
    protected void update(float percent, int entity, T actionData) {
        TintComponent tintComponent = tintMapper.get(entity);
        tintComponent.color.a = actionData.start + (actionData.end - actionData.start) * percent;
    }

    @Override
    public void begin(int entity, T actionData) {
        TintComponent tintComponent = tintMapper.get(entity);
        actionData.start = tintComponent.color.a;
    }
}
