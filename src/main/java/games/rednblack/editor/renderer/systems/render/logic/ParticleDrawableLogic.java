package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class ParticleDrawableLogic implements DrawableLogic {

    protected ComponentMapper<ParticleComponent> particleComponentMapper;
    protected ComponentMapper<TransformComponent> transformComponentMapper;

    protected com.artemis.World engine;

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE_MINUS_DST_ALPHA, GL20.GL_ONE);

        TransformComponent transformComponent = transformComponentMapper.get(entity);
        ParticleComponent particleComponent = particleComponentMapper.get(entity);

        if (particleComponent.transform) {
            TransformMathUtils.computeTransform(transformComponent).mulLeft(batch.getTransformMatrix());
            TransformMathUtils.applyTransform(batch, transformComponent);
        } else {
            particleComponent.particleEffect.setPosition(transformComponent.x, transformComponent.y);
        }

        particleComponent.particleEffect.draw(batch);

        if (particleComponent.transform) {
            TransformMathUtils.resetTransform(batch, transformComponent);
        }
    }

    @Override
    public void beginPipeline() {

    }

    @Override
    public void endPipeline() {

    }
}
