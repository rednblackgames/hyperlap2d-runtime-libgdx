package games.rednblack.editor.renderer.components.particle;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import games.rednblack.editor.renderer.components.BaseComponent;

public class ParticleComponent implements BaseComponent {
    public String particleName = "";
    public float worldMultiplier = 1f;
    private float scaleFactor = 1f;

    public ParticleEffect particleEffect;

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
        worldMultiplier = 1f;
        scaleFactor = 1f;

        particleEffect = null;
    }
}
