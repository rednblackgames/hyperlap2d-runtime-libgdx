package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;

@All(ParticleComponent.class)
public class ParticleSystem extends IteratingSystem {

	protected ComponentMapper<ParticleComponent> particleComponentMapper;

    @Override
    protected void process(int entityId) {
        ParticleComponent particleComponent = particleComponentMapper.get(entityId);

        ParticleEffect particleEffect = particleComponent.particleEffect;
        particleEffect.update(engine.getDelta());
    }

}
