package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.Actions;
import games.rednblack.editor.renderer.systems.action.data.SequenceData;

/**
 * Created by ZeppLondon on 10/23/15.
 */
public class SequenceAction<T extends SequenceData> extends ParallelAction<T> {
    @Override
    public boolean act(float delta, int entity, T actionData) {
        if (actionData.index >= actionData.actionsData.size) return true;
        ActionLogic logic = Actions.actionLogicMap.get(actionData.actionsData.get(actionData.index).logicClassName);
        logic.setEngine(engine);
        if (logic.act(delta, entity, actionData.actionsData.get(actionData.index))) {
            actionData.index++;
            if (actionData.index >= actionData.actionsData.size) return true;
        }
        return false;
    }
}
