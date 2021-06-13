package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.systems.action.data.ActionData;

/**
 * Created by ZeppLondon on 10/14/2015.
 */
abstract public class ActionLogic<T extends ActionData> {

    abstract public boolean act(float delta, int entity, T actionData);
}
