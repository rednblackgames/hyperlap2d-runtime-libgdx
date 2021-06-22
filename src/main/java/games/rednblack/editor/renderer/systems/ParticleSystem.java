package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;

@All(ParticleComponent.class)
public class ParticleSystem extends IteratingSystem {

	protected ComponentMapper<ParticleComponent> particleComponentMapper;

    @Override
    protected void process(int entityId) {
        ParticleComponent particleComponent = particleComponentMapper.get(entityId);

        ParticleEffect particleEffect = particleComponent.particleEffect;
        particleEffect.update(world.delta);
    }

}
