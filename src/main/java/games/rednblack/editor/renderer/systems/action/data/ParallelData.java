package games.rednblack.editor.renderer.systems.action.data;

import com.badlogic.gdx.utils.Array;

/**
 * Created by ZeppLondon on 10/23/15.
 */
public class ParallelData extends ActionData {
    public Array<ActionData> actionsData = new Array<>();
    public boolean complete;

    public void setActionsData(ActionData[] actionsData) {
        this.actionsData.addAll(actionsData);
    }

    @Override
    public void restart() {
        super.restart();

        for (ActionData data : new Array.ArrayIterator<>(actionsData)) {
            data.restart();
        }
        complete = false;
    }

    @Override
    public void reset() {
        super.reset();

        for (ActionData data : new Array.ArrayIterator<>(actionsData)) {
            if (data.getPool() != null)
                data.getPool().free(data);
        }

        actionsData.clear();
        complete = false;
    }
}
