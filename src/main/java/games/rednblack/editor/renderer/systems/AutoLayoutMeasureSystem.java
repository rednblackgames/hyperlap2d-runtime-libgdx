package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.BaseSystem;

/**
 * The first step of {@link AutoLayoutSystem}: composites that fit their content take its size
 * before {@link LayoutSystem} resolves the constraints that may depend on it.
 */
public class AutoLayoutMeasureSystem extends BaseSystem {

    @Override
    protected void processSystem() {
        AutoLayoutSystem autoLayoutSystem = engine.getSystem(AutoLayoutSystem.class);
        if (autoLayoutSystem != null) autoLayoutSystem.measure();
    }
}
