package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.Actions;
import games.rednblack.editor.renderer.systems.action.data.ActionData;
import games.rednblack.editor.renderer.systems.action.data.ParallelData;

/**
 * Created by ZeppLondon on 10/23/15.
 */
public class ParallelAction<T extends ParallelData> extends ActionLogic<T> {
    @Override
    public boolean act(float delta, int entity, T actionData) {
        actionData.complete = true;
        for (int i = 0; i < actionData.actionsData.size; i++) {
            ActionData data = actionData.actionsData.get(i);
            ActionLogic logic = Actions.actionLogicMap.get(actionData.actionsData.get(i).logicClassName);
            logic.setEngine(engine);
            if (!data.detached) {
                actionData.complete = false;
                if (logic.act(delta, entity, data)) {
                    data.detached = true;
                }
            }
        }
        return actionData.complete;
    }
}
