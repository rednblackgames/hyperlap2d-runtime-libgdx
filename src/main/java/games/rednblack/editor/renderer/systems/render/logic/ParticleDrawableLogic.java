package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;

public class ParticleDrawableLogic implements Drawable {

	private final ComponentMapper<ParticleComponent> particleComponentMapper = ComponentMapper.getFor(ParticleComponent.class);
	private final ComponentMapper<TransformComponent> transformComponentMapper = ComponentMapper.getFor(TransformComponent.class);

	public ParticleDrawableLogic() {
	}
	
	@Override
	public void draw(Batch batch, Entity entity, float parentAlpha) {
		ParticleComponent particleComponent = particleComponentMapper.get(entity);

		computeTransform(entity).mulLeft(batch.getTransformMatrix());
		applyTransform(entity, batch);
		particleComponent.particleEffect.draw(batch);
		resetTransform(entity, batch);
	}

	protected Matrix4 computeTransform (Entity rootEntity) {
		TransformComponent curTransform = transformComponentMapper.get(rootEntity);

		Affine2 worldTransform = curTransform.worldTransform;

		float originX = curTransform.originX;
		float originY = curTransform.originY;
		float x = curTransform.x;
		float y = curTransform.y;
		float rotation = curTransform.rotation;
		float scaleX = curTransform.scaleX;
		float scaleY = curTransform.scaleY;

		worldTransform.setToTrnRotScl(x + originX , y + originY, rotation, scaleX, scaleY);
		if (originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

		curTransform.computedTransform.set(worldTransform);

		return curTransform.computedTransform;
	}

	protected void applyTransform (Entity rootEntity, Batch batch) {
		TransformComponent curTransform = transformComponentMapper.get(rootEntity);
		curTransform.oldTransform.set(batch.getTransformMatrix());
		batch.setTransformMatrix(curTransform.computedTransform);
	}

	protected void resetTransform (Entity rootEntity, Batch batch) {
		TransformComponent curTransform = transformComponentMapper.get(rootEntity);
		batch.setTransformMatrix(curTransform.oldTransform);
	}
}
