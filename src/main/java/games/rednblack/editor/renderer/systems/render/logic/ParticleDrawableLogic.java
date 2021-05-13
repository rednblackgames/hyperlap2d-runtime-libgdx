package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class ParticleDrawableLogic implements Drawable {

	private final ComponentMapper<ParticleComponent> particleComponentMapper = ComponentMapper.getFor(ParticleComponent.class);
	private final ComponentMapper<TransformComponent> transformComponentMapper = ComponentMapper.getFor(TransformComponent.class);

	public ParticleDrawableLogic() {

	}
	
	@Override
	public void draw(Batch batch, Entity entity, float parentAlpha, boolean normal) {
		ParticleComponent particleComponent = particleComponentMapper.get(entity);

		if (particleComponent.transform) {
			TransformMathUtils.computeTransform(entity).mulLeft(batch.getTransformMatrix());
			TransformMathUtils.applyTransform(entity, batch);
		} else {
			TransformComponent transformComponent = transformComponentMapper.get(entity);
			particleComponent.particleEffect.setPosition(transformComponent.x, transformComponent.y);
		}

		particleComponent.particleEffect.draw(batch);

		if (particleComponent.transform) {
			TransformMathUtils.resetTransform(entity, batch);
		}
	}
}
