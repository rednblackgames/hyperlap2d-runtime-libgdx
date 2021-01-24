package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.talosvfx.talos.runtime.IEmitter;
import com.talosvfx.talos.runtime.Particle;
import com.talosvfx.talos.runtime.ParticleEffectInstance;
import com.talosvfx.talos.runtime.render.ParticleRenderer;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.additional.TalosComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class TalosDrawableMapper implements Drawable {

    private final ComponentMapper<TalosComponent> particleComponentMapper = ComponentMapper.getFor(TalosComponent.class);
    private final TalosRenderer defaultRenderer = new TalosRenderer();
    private Color tmpColor = new Color();

    public TalosDrawableMapper() {

    }

    @Override
    public void draw(Batch batch, Entity entity, float parentAlpha) {
        tmpColor.set(batch.getColor());

        defaultRenderer.setBatch(batch);

        TalosComponent talosComponent = particleComponentMapper.get(entity);
        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class);

        TransformMathUtils.computeTransform(entity).mulLeft(batch.getTransformMatrix());
        TransformMathUtils.applyTransform(entity, batch);

        talosComponent.effect.render(defaultRenderer);

        TransformMathUtils.resetTransform(entity, batch);

        batch.setColor(tmpColor);
    }

    private static class TalosRenderer implements ParticleRenderer {

        private Batch batch;

        Color color = new Color(Color.WHITE);

        public TalosRenderer () {
        }

        public TalosRenderer (Batch batch) {
            this.batch = batch;
        }

        public void setBatch (Batch batch) {
            this.batch = batch;
        }

        @Override
        public void render (ParticleEffectInstance particleEffectInstance) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

            for (int i = 0; i < particleEffectInstance.getEmitters().size; i++) {
                final IEmitter particleEmitter = particleEffectInstance.getEmitters().get(i);
                if(!particleEmitter.isVisible()) continue;
                if(particleEmitter.isBlendAdd()) {
                    batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
                } else {
                    if (particleEmitter.isAdditive()) {
                        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    } else {
                        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    }
                }

                for (int j = 0; j < particleEmitter.getActiveParticleCount(); j++) {
                    renderParticle(batch, particleEmitter.getActiveParticles().get(j), particleEffectInstance.alpha);
                }
            }

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        private void renderParticle (Batch batch, Particle particle, float parentAlpha) {
            color.set(particle.color);
            color.mul(particle.getEmitter().getTint());
            color.a = particle.transparency * parentAlpha;
            batch.setColor(color);

            if (particle.drawable != null) {
                particle.drawable.setCurrentParticle(particle);
                particle.drawable.draw(batch, particle, color);
            }
        }
    }
}
