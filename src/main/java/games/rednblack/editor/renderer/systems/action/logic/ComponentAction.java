package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.data.ComponentData;

/**
 * Created by aurel on 19/02/16.
 */
public abstract class ComponentAction<T extends ComponentData> extends DelegateAction<T> {

    @Override
    public boolean act(float delta, int entity, T actionData) {
        if (actionData.linkedComponentMapper == null || actionData.linkedComponentMapper.has(entity)) {
            return delegate(delta, entity, actionData);
        } else {
            return true;
        }
    }
}
