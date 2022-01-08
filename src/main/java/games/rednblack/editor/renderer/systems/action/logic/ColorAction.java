package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.systems.action.data.ColorData;

/**
 * Created by ZeppLondon on 10/28/15.
 */
public class ColorAction<T extends ColorData> extends TemporalAction<T> {

    @Override
    protected void update(float percent, int entity, T actionData) {
        TintComponent tintComponent = tintMapper.get(entity);
        if (tintComponent == null) return;

        float r = actionData.startR + (actionData.endColor.r - actionData.startR) * percent;
        float g = actionData.startG + (actionData.endColor.g - actionData.startG) * percent;
        float b = actionData.startB + (actionData.endColor.b - actionData.startB) * percent;
        float a = actionData.startA + (actionData.endColor.a - actionData.startA) * percent;
        tintComponent.color.set(r, g, b, a);
    }

    @Override
    public void begin(int entity, T actionData) {
        TintComponent tintComponent = tintMapper.get(entity);
        if (tintComponent == null) return;

        actionData.startR = tintComponent.color.r;
        actionData.startG = tintComponent.color.g;
        actionData.startB = tintComponent.color.b;
        actionData.startA = tintComponent.color.a;
    }
}
