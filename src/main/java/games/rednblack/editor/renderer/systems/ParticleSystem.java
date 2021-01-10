package games.rednblack.editor.renderer.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;

public class ParticleSystem extends IteratingSystem {

	private final ComponentMapper<ParticleComponent> particleComponentMapper = ComponentMapper.getFor(ParticleComponent.class);
	
	public ParticleSystem() {
		super(Family.all(ParticleComponent.class).get());
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		ParticleComponent particleComponent = particleComponentMapper.get(entity);

		ParticleEffect particleEffect = particleComponent.particleEffect;
		particleEffect.update(deltaTime);
	}

}
