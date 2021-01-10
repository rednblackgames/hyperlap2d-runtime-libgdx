package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class ParticleDrawableLogic implements Drawable {

	private final ComponentMapper<ParticleComponent> particleComponentMapper = ComponentMapper.getFor(ParticleComponent.class);

	public ParticleDrawableLogic() {

	}
	
	@Override
	public void draw(Batch batch, Entity entity, float parentAlpha) {
		ParticleComponent particleComponent = particleComponentMapper.get(entity);

		TransformMathUtils.computeTransform(entity).mulLeft(batch.getTransformMatrix());
		TransformMathUtils.applyTransform(entity, batch);

		particleComponent.particleEffect.draw(batch);

		TransformMathUtils.resetTransform(entity, batch);
	}
}
