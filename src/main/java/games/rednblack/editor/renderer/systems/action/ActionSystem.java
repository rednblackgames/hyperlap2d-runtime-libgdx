package games.rednblack.editor.renderer.systems.action;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.ActionComponent;
import games.rednblack.editor.renderer.systems.action.data.ActionData;
import games.rednblack.editor.renderer.systems.action.logic.ActionLogic;

/**
 * Created by ZeppLondon on 10/13/2015.
 */
@All(ActionComponent.class)
public class ActionSystem extends IteratingSystem {
    protected ComponentMapper<ActionComponent> actionMapper;

    @Override
    protected void initialize() {
        super.initialize();
    }

    @Override
    protected void process(int entity) {
        ActionComponent actionComponent = actionMapper.get(entity);
        Array<ActionData> dataArray = actionComponent.dataArray;
        for (int i = 0; i < dataArray.size; i++) {
            ActionData data = dataArray.get(i);
            ActionLogic actionLogic = Actions.actionLogicMap.get(data.logicClassName);
            actionLogic.setEngine(getEngine());
            if (actionLogic.act(Math.min(engine.getDelta(), 0.25f), entity, data)) {
                dataArray.removeValue(data, true);
                if (data.getPool() != null)
                    data.getPool().free(data);
            }
        }

        if (dataArray.size == 0) {
            actionMapper.remove(entity);
        }
    }
}
