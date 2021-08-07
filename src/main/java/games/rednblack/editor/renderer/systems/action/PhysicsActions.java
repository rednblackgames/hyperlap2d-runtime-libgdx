package games.rednblack.editor.renderer.systems.action;

import com.artemis.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import games.rednblack.editor.renderer.systems.action.data.ActionData;
import games.rednblack.editor.renderer.systems.action.data.ForceData;
import games.rednblack.editor.renderer.systems.action.logic.ActionLogic;
import games.rednblack.editor.renderer.systems.action.logic.ForceAction;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

/**
 * Created by aurel on 02/04/16.
 */
public class PhysicsActions {

    private static void initialize(Class<? extends ActionData> data, Class<? extends ActionLogic> type) {
        try {
            Actions.registerActionClass(data, type);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Apply a force to an entity with physics component. The force is applied as long as
     * the corresponding entity as a physics component.
     * @param force The world force vector, usually in Newtons (N)
     * @return The games.rednblack.editor.renderer.systems.action.data.ForceData object
     */
    public static ForceData force(Vector2 force) {
        initialize(ForceData.class, ForceAction.class);
        ForceData forceData = Actions.actionData(ForceData.class);
        forceData.setForce(force);

        forceData.logicClassName = ForceAction.class.getName();
        return forceData;
    }

    /**
     * Apply a force to an entity with physics component. The force is applied as long as
     * the corresponding entity as a physics component.
     * @param force The world force vector, usually in Newtons (N)
     * @param relativePoint The point where the force is applied relative to the body origin
     * @return The games.rednblack.editor.renderer.systems.action.data.ForceData object
     */
    public static ForceData force(Vector2 force, Vector2 relativePoint) {
        initialize(ForceData.class, ForceAction.class);
        ForceData forceData = Actions.actionData(ForceData.class);
        forceData.setForce(force, relativePoint);

        forceData.logicClassName = ForceAction.class.getName();
        return forceData;
    }

    /**
     * Apply a force to an entity with physics component.
     * @param force The world force vector, usually in Newtons (N)
     * @param relativePoint The point where the force is applied relative to the body origin
     * @param linkedComponent The force is applied as long as the corresponding entity
     *                        has this component
     * @return The games.rednblack.editor.renderer.systems.action.data.ForceData object
     */
    public static ForceData force(Vector2 force, Vector2 relativePoint, Class<? extends Component> linkedComponent, com.artemis.World engine) {
        ForceData forceData = force(force, relativePoint);

        forceData.linkedComponentMapper = ComponentRetriever.getMapper(linkedComponent, engine);
        return forceData;
    }
}
