package games.rednblack.editor.renderer.systems.strategy;

import com.artemis.BaseSystem;
import com.artemis.SystemInvocationStrategy;
import com.artemis.utils.Bag;
import com.artemis.utils.BitVector;

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

    public static void setTimeStep(int targetFps) {
        TIME_STEP = 1f / targetFps;
        TIME_STEP_NANO = (long) (TIME_STEP * 1000000000);
        INV_TIME_STEP_NANO = 1f / TIME_STEP_NANO;
    }

    private long currentTime = System.nanoTime();
    private long accumulator = 0;

    @Override
    protected void initialize() {
        BaseSystem[] rawSystems = systems.getData();
        for (int i = 0; i < systems.size(); i++) {
            BaseSystem rawSystem = rawSystems[i];
            if (rawSystem instanceof RendererSystem)
                renderSystems.add(rawSystem);
            else
                logicSystems.add(rawSystem);
            if (rawSystem instanceof InterpolationSystem)
                interpolationSystems.add((InterpolationSystem) rawSystem);
        }
    }

    @Override
    protected void process() {
        long newTime = System.nanoTime();
        long frameTime = newTime - currentTime;
        currentTime = newTime;

        accumulator += frameTime;

        world.setDelta(TIME_STEP);

        while (accumulator >= TIME_STEP_NANO) {
            //Process logic systems
            BaseSystem[] systemsData = logicSystems.getData();
            for (int i = 0, s = logicSystems.size(); s > i; i++) {
                if (disabledLogicSystems.get(i))
                    continue;

                updateEntityStates();
                systemsData[i].process();
            }

            accumulator -= TIME_STEP_NANO;
        }

        //interpolate accumulator data
        InterpolationSystem[] systems = interpolationSystems.getData();
        for (int i = 0, s = interpolationSystems.size(); s > i; i++) {
            if (disabledInterpolationSystems.get(i))
                continue;

            updateEntityStates();
            float alpha = accumulator * INV_TIME_STEP_NANO;
            systems[i].interpolate(alpha);
        }

        //process rendering systems
        BaseSystem[] systemsData = renderSystems.getData();
        for (int i = 0, s = renderSystems.size(); s > i; i++) {
            if (disabledRenderSystems.get(i))
                continue;

            updateEntityStates();
            systemsData[i].process();
        }

        updateEntityStates();
    }

    @Override
    public boolean isEnabled(BaseSystem target) {
        Bag<BaseSystem> checkSystems = (target instanceof RendererSystem) ? renderSystems : logicSystems;
        BitVector checkDisabled = (target instanceof RendererSystem) ? disabledRenderSystems : disabledLogicSystems;
        Class targetClass = target.getClass();
        for (int i = 0; i < checkSystems.size(); i++) {
            if (targetClass == checkSystems.get(i).getClass())
                return !checkDisabled.get(i);
        }
        throw new RuntimeException("System not found - " + target);
    }

    @Override
    public void setEnabled(BaseSystem target, boolean value) {
        Bag<BaseSystem> checkSystems = (target instanceof RendererSystem) ? renderSystems : logicSystems;
        BitVector checkDisabled = (target instanceof RendererSystem) ? disabledRenderSystems : disabledLogicSystems;
        Class targetClass = target.getClass();
        for (int i = 0; i < checkSystems.size(); i++) {
            if (targetClass == checkSystems.get(i).getClass()) {
                checkDisabled.set(i, value);
                if (target instanceof InterpolationSystem)
                    disabledInterpolationSystems.set(i, value);
                return;
            }
        }
        throw new RuntimeException("System not found - " + target);
    }
}
