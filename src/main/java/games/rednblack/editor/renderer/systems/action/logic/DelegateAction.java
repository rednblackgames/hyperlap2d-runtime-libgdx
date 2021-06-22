package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.data.DelegateData;

/**
 * Created by ZeppLondon on 10/15/2015.
 */
public abstract class DelegateAction<T extends DelegateData> extends ActionLogic<T> {
    @Override
    public boolean act(float delta, int entity, T actionData) {
        return delegate(delta, entity, actionData);
    }

    abstract protected boolean delegate (float delta, int entity, T actionData);
}
