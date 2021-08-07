package games.rednblack.editor.renderer.systems.action;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
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
            actionLogic.setEngine(getWorld());
            if (actionLogic.act(world.getDelta(), entity, data)) {
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
