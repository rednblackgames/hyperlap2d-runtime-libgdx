package games.rednblack.editor.renderer.components.particle;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;

public class ParticleComponent extends PooledComponent {
    public String particleName = "";
    public boolean transform = true;
    public boolean autoStart = true;

    public float worldMultiplier = 1f;
    private float scaleFactor = 1f;

    public transient ParticleEffect particleEffect;

    public void scaleEffect(float scale) {
        scaleFactor = scale;
        particleEffect.scaleEffect(scaleFactor * worldMultiplier);
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    @Override
    public void reset() {
        particleName = "";
        transform = true;
        autoStart = true;
        worldMultiplier = 1f;
        scaleFactor = 1f;

        if (particleEffect instanceof ParticleEffectPool.PooledEffect) {
            ParticleEffectPool.PooledEffect pooledEffect = (ParticleEffectPool.PooledEffect) particleEffect;
            pooledEffect.free();
            particleEffect = null;
        }
    }
}
