package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.data.RunnableData;

/**
 * Created by ZeppLondon on 10/15/2015.
 */
public class RunnableAction<T extends RunnableData> extends ActionLogic<T> {
    @Override
    public boolean act(float delta, int entity, T actionData) {
        if (!actionData.ran) {
            actionData.ran = true;
            run(entity, actionData);
        }
        return true;
    }

    public void run(int entity, T actionData) {
        if (actionData.runnable != null)
            actionData.runnable.run(entity);
    }
}
