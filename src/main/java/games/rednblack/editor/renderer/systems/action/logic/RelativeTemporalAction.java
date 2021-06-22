package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.data.RelativeTemporalData;

/**
 * Created by ZeppLondon on 10/15/2015.
 */
public abstract class RelativeTemporalAction<T extends RelativeTemporalData> extends TemporalAction<T> {
    @Override
    protected void update(float percent, int entity, T actionData) {
        updateRelative(percent - actionData.lastPercent, entity, actionData);
        actionData.lastPercent = percent;
    }

    @Override
    public void begin(int entity, T actionData) {
        RelativeTemporalData data = actionData;
        data.lastPercent = 0;
    }

    abstract protected void updateRelative (float percentDelta, int entity, T actionData);
}
