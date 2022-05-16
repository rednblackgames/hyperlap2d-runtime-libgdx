package games.rednblack.editor.renderer.systems.action.data;

import games.rednblack.editor.renderer.systems.action.ActionRunnable;

/**
 * Created by ZeppLondon on 10/15/2015.
 */
public class RunnableData extends ActionData {
    public ActionRunnable runnable;
    public boolean ran;

    public void setRunnable(ActionRunnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void restart() {
        super.restart();

        ran = false;
    }

    @Override
    public void reset() {
        super.reset();

        runnable = null;
        ran = false;
    }
}
