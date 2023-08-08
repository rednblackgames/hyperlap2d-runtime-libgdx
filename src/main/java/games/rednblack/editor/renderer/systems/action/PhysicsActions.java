package games.rednblack.editor.renderer.systems.action;

import com.artemis.Component;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import games.rednblack.editor.renderer.systems.action.data.ActionData;
import games.rednblack.editor.renderer.systems.action.data.physics.ForceData;
import games.rednblack.editor.renderer.systems.action.data.physics.TransformByData;
import games.rednblack.editor.renderer.systems.action.data.physics.TransformToData;
import games.rednblack.editor.renderer.systems.action.logic.physics.ForceAction;
import games.rednblack.editor.renderer.systems.action.logic.physics.TransformByAction;
import games.rednblack.editor.renderer.systems.action.logic.physics.TransformToAction;

/**
 * Created by aurel on 02/04/16.
 */
public class PhysicsActions {

    static void initialize() throws ReflectionException {
        Actions.registerActionClass(ForceData.class, ForceAction.class);
        Actions.registerActionClass(TransformToData.class, TransformToAction.class);
        Actions.registerActionClass(TransformByData.class, TransformByAction.class);
    }

    public static ActionData transformTo(float x, float y, float angle) {
        return transformTo(x, y, angle, 0, null);
    }

    public static ActionData transformTo(float x, float y, float angle, float duration) {
        return transformTo(x, y, angle, duration, null);
    }

    public static ActionData transformTo(float x, float y, float angle, float duration, Interpolation interpolation) {
        TransformToData actionData = Actions.actionData(TransformToData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setEndX(x);
        actionData.setEndY(y);
        actionData.setEndAngle(angle);

        return (actionData);
    }

    public static ActionData transformBy(float x, float y, float angle) {
        return transformBy(x, y, angle, 0, null);
    }

    public static ActionData transformBy(float x, float y, float angle, float duration) {
        return transformBy(x, y, angle, duration, null);
    }

    public static ActionData transformBy(float x, float y, float angle, float duration, Interpolation interpolation) {
        TransformByData actionData = Actions.actionData(TransformByData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setAmountX(x);
        actionData.setAmountY(y);
        actionData.setAmountAngle(angle);
        return actionData;
    }

    /**
     * Apply a force to an entity with physics component. The force is applied as long as
     * the corresponding entity as a physics component.
     * @param force The world force vector, usually in Newtons (N)
     * @return {@link ForceData} object
     */
    public static ForceData force(Vector2 force) {
        ForceData forceData = Actions.actionData(ForceData.class);
        forceData.setForce(force);
        return forceData;
    }

    /**
     * Apply a force to an entity with physics component. The force is applied as long as
     * the corresponding entity as a physics component.
     * @param force The world force vector, usually in Newtons (N)
     * @param relativePoint The point where the force is applied relative to the body origin
     * @return {@link ForceData} object
     */
    public static ForceData force(Vector2 force, Vector2 relativePoint) {
        ForceData forceData = Actions.actionData(ForceData.class);
        forceData.setForce(force, relativePoint);

        return forceData;
    }

    /**
     * Apply a force to an entity with physics component.
     * @param force The world force vector, usually in Newtons (N)
     * @param relativePoint The point where the force is applied relative to the body origin
     * @param linkedComponent The force is applied as long as the corresponding entity
     *                        has this component
     * @return {@link ForceData} object
     */
    public static ForceData force(Vector2 force, Vector2 relativePoint, Class<? extends Component> linkedComponent, com.artemis.World engine) {
        ForceData forceData = force(force, relativePoint);

        forceData.linkedComponentMapper = engine.getMapper(linkedComponent);
        return forceData;
    }
}
