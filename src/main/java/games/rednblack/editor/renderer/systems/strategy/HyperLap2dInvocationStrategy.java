package games.rednblack.editor.renderer.systems.strategy;

import com.artemis.BaseSystem;
import com.artemis.SystemInvocationStrategy;
import com.artemis.utils.Bag;
import com.artemis.utils.BitVector;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;

public class HyperLap2dInvocationStrategy extends SystemInvocationStrategy {

    private final Bag<BaseSystem> renderSystems = new Bag<>(BaseSystem.class);
    private final Bag<BaseSystem> logicSystems = new Bag<>(BaseSystem.class);
    private final Bag<InterpolationSystem> interpolationSystems = new Bag<>(InterpolationSystem.class);

    private final BitVector disabledRenderSystems = new BitVector();
    private final BitVector disabledLogicSystems = new BitVector();
    private final BitVector disabledInterpolationSystems = new BitVector();

    public static float TIME_STEP = 1f / 120f;
    public static long TIME_STEP_NANO = (long) (TIME_STEP * 1000000000);
    public static float INV_TIME_STEP_NANO = 1f / TIME_STEP_NANO;

    public static void setTimeStep(int targetFps) {
        TIME_STEP = 1f / targetFps;
        TIME_STEP_NANO = (long) (TIME_STEP * 1000000000);
        INV_TIME_STEP_NANO = 1f / TIME_STEP_NANO;
    }

    private long currentTime;
    private long accumulator = 0;

    public static final Object updateEntities = new Object();

    @Override
    protected void initialize() {
        for (int i = 0; i < systems.size(); i++) {
            BaseSystem rawSystem = systems.get(i);
            if (rawSystem instanceof InterpolationSystem)
                interpolationSystems.add((InterpolationSystem) rawSystem);

            Class<?> systemClass = rawSystem.getClass();
            if (systemClass.isAnnotationPresent(FixedTimestep.class)) {
                logicSystems.add(rawSystem);
            } else {
                renderSystems.add(rawSystem);
            }
        }

        currentTime = TimeUtils.nanoTime();
    }

    @Override
    protected void process() {
        long newTime = TimeUtils.nanoTime();
        long frameTime = Math.min(newTime - currentTime, 250000000);
        currentTime = newTime;

        accumulator += frameTime;

        world.setDelta(TIME_STEP);

        while (accumulator >= TIME_STEP_NANO) {
            //Process logic systems
            for (int i = 0, s = logicSystems.size(); s > i; i++) {
                if (disabledLogicSystems.get(i))
                    continue;

                updateEntitySateSync();
                logicSystems.get(i).process();
            }

            accumulator -= TIME_STEP_NANO;
        }

        //interpolate accumulator data
        for (int i = 0, s = interpolationSystems.size(); s > i; i++) {
            if (disabledInterpolationSystems.get(i))
                continue;

            float alpha = accumulator * INV_TIME_STEP_NANO;
            interpolationSystems.get(i).interpolate(alpha);
        }

        world.setDelta(Gdx.graphics.getDeltaTime());

        //process rendering systems
        for (int i = 0, s = renderSystems.size(); s > i; i++) {
            if (disabledRenderSystems.get(i))
                continue;

            updateEntitySateSync();
            renderSystems.get(i).process();
        }

        updateEntitySateSync();
    }

    public void updateEntitySateSync() {
        synchronized (updateEntities) {
            updateEntityStates();
        }
    }

    @Override
    public boolean isEnabled(BaseSystem target) {
        Class<? extends BaseSystem> targetClass = target.getClass();
        Bag<BaseSystem> checkSystems = targetClass.isAnnotationPresent(FixedTimestep.class) ? logicSystems : renderSystems;
        BitVector checkDisabled = targetClass.isAnnotationPresent(FixedTimestep.class) ? disabledLogicSystems : disabledRenderSystems;
        for (int i = 0; i < checkSystems.size(); i++) {
            if (targetClass == checkSystems.get(i).getClass())
                return !checkDisabled.get(i);
        }
        throw new RuntimeException("System not found - " + target);
    }

    @Override
    public void setEnabled(BaseSystem target, boolean value) {
        Class<? extends BaseSystem> targetClass = target.getClass();
        Bag<BaseSystem> checkSystems = targetClass.isAnnotationPresent(FixedTimestep.class) ? logicSystems : renderSystems;
        BitVector checkDisabled = targetClass.isAnnotationPresent(FixedTimestep.class) ? disabledLogicSystems : disabledRenderSystems;
        for (int i = 0; i < checkSystems.size(); i++) {
            if (targetClass == checkSystems.get(i).getClass()) {
                checkDisabled.set(i, !value);
                if (target instanceof InterpolationSystem)
                    disabledInterpolationSystems.set(i, !value);
                return;
            }
        }
        throw new RuntimeException("System not found - " + target);
    }
}
