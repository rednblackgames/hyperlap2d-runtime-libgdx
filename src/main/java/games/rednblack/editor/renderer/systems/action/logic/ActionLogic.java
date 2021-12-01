package games.rednblack.editor.renderer.systems.action.logic;

import com.artemis.ComponentMapper;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.systems.action.data.ActionData;

/**
 * Created by ZeppLondon on 10/14/2015.
 */
abstract public class ActionLogic<T extends ActionData> {
    protected com.artemis.World engine;

    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<TintComponent> tintMapper;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;

    abstract public boolean act(float delta, int entity, T actionData);

    public void setEngine(com.artemis.World engine) {
        if (this.engine == null) {
            this.engine = engine;
            transformMapper = engine.getMapper(TransformComponent.class);
            tintMapper = engine.getMapper(TintComponent.class);
            physicsBodyMapper = engine.getMapper(PhysicsBodyComponent.class);
            dimensionsMapper = engine.getMapper(DimensionsComponent.class);
        }
    }
}
