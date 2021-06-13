package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.BaseComponentMapper;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class ParticleDrawableLogic implements Drawable {

    protected BaseComponentMapper<ParticleComponent> particleComponentMapper;
    protected BaseComponentMapper<TransformComponent> transformComponentMapper;

    public void init() {
        particleComponentMapper = ComponentRetriever.getMapper(ParticleComponent.class);
        transformComponentMapper = ComponentRetriever.getMapper(TransformComponent.class);
    }

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        if(particleComponentMapper ==null) init(); // TODO: Can we have an injection for this object?

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
