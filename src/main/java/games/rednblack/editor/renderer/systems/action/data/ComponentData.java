package games.rednblack.editor.renderer.systems.action.data;


import games.rednblack.editor.renderer.ecs.BaseComponentMapper;

/**
 * Created by aurel on 19/02/16.
 */
public class ComponentData extends DelegateData {
    public BaseComponentMapper linkedComponentMapper;

    @Override
    public void reset() {
        super.reset();
        linkedComponentMapper = null;
    }
}
