package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.systems.action.data.ActionData;

/**
 * Created by ZeppLondon on 10/14/2015.
 */
abstract public class ActionLogic<T extends ActionData> {
    protected Engine engine;

    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<TintComponent> tintMapper;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<LayoutComponent> layoutMapper;

    abstract public boolean act(float delta, int entity, T actionData);

    /**
     * Binds the mappers to the engine the action runs in. One logic serves every engine there is (a
     * game's scene and its GUI have one each), so they are bound again whenever the engine changes:
     * bound once, they would read the components of whichever engine came first.
     */
    public void setEngine(Engine engine) {
        if (this.engine == engine) return;
        this.engine = engine;
        transformMapper = engine.getMapper(TransformComponent.class);
        tintMapper = engine.getMapper(TintComponent.class);
        physicsBodyMapper = engine.getMapper(PhysicsBodyComponent.class);
        dimensionsMapper = engine.getMapper(DimensionsComponent.class);
        layoutMapper = engine.getMapper(LayoutComponent.class);
    }
}
