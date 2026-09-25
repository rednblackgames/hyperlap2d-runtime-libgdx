package games.rednblack.editor.renderer.systems.strategy;

import games.rednblack.editor.renderer.ecs.BaseSystem;
import games.rednblack.editor.renderer.ecs.SystemInvocationStrategy;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import games.rednblack.editor.renderer.ecs.utils.BitVector;
import games.rednblack.editor.renderer.utils.profiling.SystemProfiler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;

public class HyperLap2dInvocationStrategy extends SystemInvocationStrategy {

    private final Bag<BaseSystem> renderSystems = new Bag<>(BaseSystem.class);
    private final Bag<BaseSystem> logicSystems = new Bag<>(BaseSystem.class);
    private final Bag<InterpolationSystem> interpolationSystems = new Bag<>(InterpolationSystem.class);

    private final BitVector disabledRenderSystems = new BitVector();
    private final BitVector disabledLogicSystems = new BitVector();
    private final BitVector disabledInterpolationSystems = new BitVector();

    public static float TIME_STEP = 1f / 60f;
    public static long TIME_STEP_NANO = (long) (TIME_STEP * 1000000000);
    public static float INV_TIME_STEP_NANO = 1f / TIME_STEP_NANO;

    public static void setFixedTimeStep(int targetFps) {
        TIME_STEP = 1f / targetFps;
        TIME_STEP_NANO = (long) (TIME_STEP * 1000000000);
        INV_TIME_STEP_NANO = 1f / TIME_STEP_NANO;
    }

    public static float TIME_SCALE = 1f;
    public static void setTimeScale(float timeScale) {
        TIME_SCALE = Math.max(0, timeScale);
    }

    private long currentTime;
    private long accumulator = 0;

    /** Off unless someone is watching: null costs one check per system per frame. */
    private SystemProfiler profiler;
    private int logicOffset, interpolationOffset, renderOffset;

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

        if (profiler != null) describeSystems();
    }

    /**
     * Hands the systems over to a profiler, or takes them back with {@code null}. A system that runs both
     * on the fixed step and as an interpolation is reported twice, once for each kind of work.
     */
    public void setProfiler(SystemProfiler profiler) {
        this.profiler = profiler;
        if (profiler != null && systems != null) describeSystems();
    }

    public SystemProfiler getProfiler() {
        return profiler;
    }

    private void describeSystems() {
        profiler.describeBegin();

        logicOffset = 0;
        for (int i = 0; i < logicSystems.size(); i++)
            profiler.describeSystem(logicSystems.get(i).getClass().getSimpleName(), SystemProfiler.BUCKET_LOGIC);

        interpolationOffset = logicSystems.size();
        for (int i = 0; i < interpolationSystems.size(); i++)
            profiler.describeSystem(interpolationSystems.get(i).getClass().getSimpleName() + " (interpolate)", SystemProfiler.BUCKET_INTERPOLATION);

        renderOffset = interpolationOffset + interpolationSystems.size();
        for (int i = 0; i < renderSystems.size(); i++)
            profiler.describeSystem(renderSystems.get(i).getClass().getSimpleName(), SystemProfiler.BUCKET_RENDER);
    }

    @Override
    protected void process() {
        if (profiler != null) profiler.beginFrame();

        long newTime = TimeUtils.nanoTime();
        long frameTime = Math.min(newTime - currentTime, 250000000);
        currentTime = newTime;

        accumulator += (long) (frameTime * TIME_SCALE);

        engine.setDelta(TIME_STEP);

        while (accumulator >= TIME_STEP_NANO) {
            //Process logic systems
            for (int i = 0, s = logicSystems.size(); s > i; i++) {
                if (disabledLogicSystems.get(i))
                    continue;

                updateEntitySateSync();
                if (profiler != null) profiler.begin();
                logicSystems.get(i).process();
                if (profiler != null) profiler.end(logicOffset + i);
            }

            accumulator -= TIME_STEP_NANO;
        }

        //interpolate accumulator data
        for (int i = 0, s = interpolationSystems.size(); s > i; i++) {
            if (disabledInterpolationSystems.get(i))
                continue;

            float alpha = accumulator * INV_TIME_STEP_NANO;
            if (profiler != null) profiler.begin();
            interpolationSystems.get(i).interpolate(alpha);
            if (profiler != null) profiler.end(interpolationOffset + i);
        }

        engine.setDelta(Gdx.graphics.getDeltaTime() * TIME_SCALE);

        //process rendering systems
        for (int i = 0, s = renderSystems.size(); s > i; i++) {
            if (disabledRenderSystems.get(i))
                continue;

            updateEntitySateSync();
            if (profiler != null) profiler.begin();
            renderSystems.get(i).process();
            if (profiler != null) profiler.end(renderOffset + i);
        }

        updateEntitySateSync();

        if (profiler != null) profiler.endFrame();
    }

    public void updateEntitySateSync() {
        if (profiler != null) profiler.beginSync();
        synchronized (updateEntities) {
            updateEntityStates();
        }
        if (profiler != null) profiler.endSync();
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
