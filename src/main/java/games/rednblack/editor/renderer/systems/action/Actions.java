package games.rednblack.editor.renderer.systems.action;

import com.artemis.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.DefaultPool;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.PoolManager;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import games.rednblack.editor.renderer.components.ActionComponent;
import games.rednblack.editor.renderer.systems.action.data.*;
import games.rednblack.editor.renderer.systems.action.logic.*;

import java.util.HashMap;

/**
 * Created by Eduard on 10/13/2015.
 */
public class Actions {
    public static final PoolManager ACTION_POOLS = new PoolManager();

    public static HashMap<String, ActionLogic> actionLogicMap = new HashMap<>();
    public static HashMap<String, String> actionDataLogicMap = new HashMap<>();
    private static boolean initialized;

    public static void dispose() {
        initialized = false;
        actionLogicMap.clear();
        actionDataLogicMap.clear();
    }

    private static void initialize() throws ReflectionException {
        registerActionClass(MoveToData.class, MoveToData::new, MoveToAction.class);
        registerActionClass(MoveByData.class, MoveByData::new, MoveByAction.class);
        registerActionClass(SizeToData.class, SizeToData::new, SizeToAction.class);
        registerActionClass(SizeByData.class, SizeByData::new, SizeByAction.class);
        registerActionClass(ScaleToData.class, ScaleToData::new, ScaleToAction.class);
        registerActionClass(ScaleByData.class, ScaleByData::new, ScaleByAction.class);
        registerActionClass(RotateToData.class, RotateToData::new, RotateToAction.class);
        registerActionClass(RotateByData.class, RotateByData::new, RotateByAction.class);
        registerActionClass(ColorData.class, ColorData::new, ColorAction.class);
        registerActionClass(AlphaData.class, AlphaData::new, AlphaAction.class);

        registerActionClass(RunnableData.class, RunnableData::new, RunnableAction.class);
        registerActionClass(DelayData.class, DelayData::new, DelayAction.class);

        registerActionClass(ParallelData.class, ParallelData::new, ParallelAction.class);
        registerActionClass(SequenceData.class, SequenceData::new, SequenceAction.class);
        registerActionClass(RepeatData.class, RepeatData::new, RepeatAction.class);

        PhysicsActions.initialize();

        initialized = true;
    }

    public static <T extends ActionLogic, U extends ActionData> void registerActionClass(Class<U> typeData, DefaultPool.PoolSupplier<U> supplierData, Class<T> type) throws ReflectionException {
        if (!actionLogicMap.containsKey(type.getName())) {
            actionLogicMap.put(type.getName(), ClassReflection.newInstance(type));
            actionDataLogicMap.put(typeData.getName(), type.getName());
            ACTION_POOLS.addPool(supplierData);
        }
    }

    static public <T extends ActionData> T actionData(Class<T> type) {
        return actionData(type, true);
    }

    static public <T extends ActionData> T actionData(Class<T> type, boolean autoPoolable) {
        checkInit();
        Pool<T> pool = ACTION_POOLS.getPool(type);
        T action = pool.obtain();
        if (autoPoolable)
            action.setPool(pool);
        action.logicClassName = actionDataLogicMap.get(type.getName());
        return action;
    }

    private static void checkInit() {
        if (!initialized) try {
            initialize();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    public static ActionData moveTo(float x, float y) {
        return moveTo(x, y, 0, null);
    }

    public static ActionData moveTo(float x, float y, float duration) {
        return moveTo(x, y, duration, null);
    }

    public static ActionData moveTo(float x, float y, float duration, Interpolation interpolation) {
        MoveToData actionData = actionData(MoveToData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setEndX(x);
        actionData.setEndY(y);
        return (actionData);
    }

    public static ActionData moveBy(float x, float y) {
        return moveBy(x, y, 0, null);
    }

    public static ActionData moveBy(float x, float y, float duration) {
        return moveBy(x, y, duration, null);
    }

    public static ActionData moveBy(float x, float y, float duration, Interpolation interpolation) {
        MoveByData actionData = actionData(MoveByData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setAmountX(x);
        actionData.setAmountY(y);
        return actionData;
    }

    static public ActionData run(ActionRunnable runnable) {
        RunnableData actionData = actionData(RunnableData.class);
        actionData.setRunnable(runnable);
        return actionData;
    }

    static public RotateToData rotateTo(float end) {
        return rotateTo(end, 0, null);
    }

    static public RotateToData rotateTo(float end, float duration) {
        return rotateTo(end, duration, null);
    }

    static public RotateToData rotateTo(float end, float duration, Interpolation interpolation) {
        RotateToData actionData = actionData(RotateToData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setEnd(end);
        return actionData;
    }

    static public RotateByData rotateBy(float amount) {
        return rotateBy(amount, 0, null);
    }

    static public RotateByData rotateBy(float amount, float duration) {
        return rotateBy(amount, duration, null);
    }

    static public RotateByData rotateBy(float amount, float duration, Interpolation interpolation) {
        RotateByData actionData = actionData(RotateByData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setAmount(amount);
        return actionData;
    }

    public static SizeToData sizeTo(float width, float height) {
        return sizeTo(width, height, 0, null);
    }

    public static SizeToData sizeTo(float width, float height, float duration) {
        return sizeTo(width, height, duration, null);
    }

    public static SizeToData sizeTo(float width, float height, float duration, Interpolation interpolation) {
        SizeToData actionData = actionData(SizeToData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setEndWidth(width);
        actionData.setEndHeight(height);
        return actionData;
    }

    public static SizeByData sizeBy(float width, float height) {
        return sizeBy(width, height, 0, null);
    }

    public static SizeByData sizeBy(float width, float height, float duration) {
        return sizeBy(width, height, duration, null);
    }

    public static SizeByData sizeBy(float width, float height, float duration, Interpolation interpolation) {
        SizeByData actionData = actionData(SizeByData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setAmountWidth(width);
        actionData.setAmountHeight(height);
        return actionData;
    }

    public static ScaleToData scaleTo(float width, float height) {
        return scaleTo(width, height, 0, null);
    }

    public static ScaleToData scaleTo(float width, float height, float duration) {
        return scaleTo(width, height, duration, null);
    }

    public static ScaleToData scaleTo(float width, float height, float duration, Interpolation interpolation) {
        ScaleToData actionData = actionData(ScaleToData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setEndX(width);
        actionData.setEndY(height);
        return actionData;
    }

    public static ScaleByData scaleBy(float width, float height) {
        return scaleBy(width, height, 0, null);
    }

    public static ScaleByData scaleBy(float width, float height, float duration) {
        return scaleBy(width, height, duration, null);
    }

    public static ScaleByData scaleBy(float width, float height, float duration, Interpolation interpolation) {
        ScaleByData actionData = actionData(ScaleByData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setAmountX(width);
        actionData.setAmountY(height);
        return actionData;
    }

    public static ColorData color(Color color) {
        return color(color, 0, null);
    }

    public static ColorData color(Color color, float duration) {
        return color(color, duration, null);
    }

    public static ColorData color(Color color, float duration, Interpolation interpolation) {
        ColorData actionData = actionData(ColorData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setEndColor(color);
        return actionData;
    }

    public static AlphaData alpha(float alpha) {
        return alpha(alpha, 0, null);
    }

    public static AlphaData alpha(float alpha, float duration) {
        return alpha(alpha, duration, null);
    }

    public static AlphaData alpha(float alpha, float duration, Interpolation interpolation) {
        AlphaData actionData = actionData(AlphaData.class);
        actionData.setDuration(duration);
        actionData.setInterpolation(interpolation);
        actionData.setEnd(alpha);
        return actionData;
    }

    public static AlphaData fadeIn(float duration) {
        return alpha(1, duration, null);
    }

    public static AlphaData fadeIn(float duration, Interpolation interpolation) {
        return alpha(1, duration, interpolation);
    }


    public static AlphaData fadeOut(float duration) {
        return alpha(0, duration, null);
    }

    public static AlphaData fadeOut(float duration, Interpolation interpolation) {
        return alpha(0, duration, interpolation);
    }

    public static DelayData delay(float duration) {
        DelayData actionData = actionData(DelayData.class);
        actionData.setDuration(duration);
        return actionData;
    }

    public static DelayData delay(float duration, ActionData delayedAction) {
        DelayData actionData = actionData(DelayData.class);
        actionData.setDuration(duration);
        actionData.setDelegatedAction(delayedAction);
        return actionData;
    }

    static public ParallelData parallel(ActionData... actionsData) {
        ParallelData actionData = actionData(ParallelData.class);
        actionData.setActionsData(actionsData);
        return actionData;
    }

    static public SequenceData sequence(ActionData... actionsData) {
        SequenceData actionData = actionData(SequenceData.class);
        actionData.setActionsData(actionsData);
        return actionData;
    }

    static public RepeatData repeat(int count, ActionData action) {
        RepeatData actionData = actionData(RepeatData.class);
        actionData.setRepeatCount(count);
        actionData.setDelegatedAction(action);
        return actionData;
    }

    static public RepeatData forever(ActionData action) {
        RepeatData actionData = actionData(RepeatData.class);
        actionData.setRepeatCount(RepeatData.FOREVER);
        actionData.setDelegatedAction(action);
        return actionData;
    }

    public static void addAction(final int entity, ActionData data, World engine) {
        checkInit();
        ActionComponent actionComponent = engine.getMapper(ActionComponent.class).get(entity);

        if (actionComponent == null) {
            actionComponent = engine.edit(entity).create(ActionComponent.class);
        }

        actionComponent.dataArray.add(data);
    }

    public static void removeActions(int entity, World engine) {
        ActionComponent actionComponent = engine.getMapper(ActionComponent.class).get(entity);
        if (actionComponent != null) {
            actionComponent.reset(); // action component with empty data array will be removed later by ActionSystem
        }
    }

    public static void removeAction(int entity, ActionData data, World engine) {
        ActionComponent actionComponent = engine.getMapper(ActionComponent.class).get(entity);
        if (actionComponent != null) {
            if (actionComponent.dataArray.contains(data, true)) {
                actionComponent.dataArray.removeValue(data, true);
                if (data.getPool() != null)
                    data.getPool().free(data);
            }
        }
    }

    public static boolean hasActions(int entity, World engine) {
        ActionComponent actionComponent = engine.getMapper(ActionComponent.class).get(entity);
        return actionComponent != null && actionComponent.dataArray.size > 0;
    }
}
