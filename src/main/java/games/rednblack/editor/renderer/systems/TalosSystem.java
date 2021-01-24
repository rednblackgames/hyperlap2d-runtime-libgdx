package games.rednblack.editor.renderer.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import games.rednblack.editor.renderer.components.additional.TalosComponent;

public class TalosSystem extends IteratingSystem {
    private final ComponentMapper<TalosComponent> particleComponentMapper = ComponentMapper.getFor(TalosComponent.class);

    public TalosSystem() {
        super(Family.all(TalosComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TalosComponent talosComponent = particleComponentMapper.get(entity);

        talosComponent.effect.update(deltaTime);
    }
}
