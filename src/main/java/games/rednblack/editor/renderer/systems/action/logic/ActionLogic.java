package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.data.ActionData;

/**
 * Created by ZeppLondon on 10/14/2015.
 */
abstract public class ActionLogic<T extends ActionData> {
    protected com.artemis.World engine;

    abstract public boolean act(float delta, int entity, T actionData);

    public void setEngine(com.artemis.World engine) {
        if (this.engine == null || this.engine != engine)
            engine.inject(this);
        this.engine = engine;
    }
}
