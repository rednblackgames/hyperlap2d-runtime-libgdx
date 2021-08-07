package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class ParticleDrawableLogic implements Drawable {

    protected ComponentMapper<ParticleComponent> particleComponentMapper;
    protected ComponentMapper<TransformComponent> transformComponentMapper;

    protected com.artemis.World engine;

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {

        ParticleComponent particleComponent = particleComponentMapper.get(entity);

        if (particleComponent.transform) {
            TransformMathUtils.computeTransform(entity, engine).mulLeft(batch.getTransformMatrix());
            TransformMathUtils.applyTransform(entity, batch, engine);
        } else {
            TransformComponent transformComponent = transformComponentMapper.get(entity);
            particleComponent.particleEffect.setPosition(transformComponent.x, transformComponent.y);
        }

        particleComponent.particleEffect.draw(batch);

        if (particleComponent.transform) {
            TransformMathUtils.resetTransform(entity, batch, engine);
        }
    }
}
