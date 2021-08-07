package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.Actions;
import games.rednblack.editor.renderer.systems.action.data.RepeatData;

public class RepeatAction<T extends RepeatData> extends DelegateAction<T> {
    @Override
    protected boolean delegate(float delta, int entity, T actionData) {
        if (actionData.repeatCount != RepeatData.FOREVER
                && actionData.executedCount >= actionData.repeatCount)
            return true;

        ActionLogic logic = Actions.actionLogicMap.get(actionData.delegatedData.logicClassName);
        logic.setEngine(engine);
        boolean actionEnd = logic.act(delta, entity, actionData.delegatedData);
        if (actionEnd) {
            actionData.executedCount++;
            actionData.delegatedData.restart();
        }

        return false;
    }
}
