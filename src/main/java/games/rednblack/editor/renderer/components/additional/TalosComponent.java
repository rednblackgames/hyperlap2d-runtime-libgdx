package games.rednblack.editor.renderer.components.additional;

import com.talosvfx.talos.runtime.ParticleEffectInstance;
import games.rednblack.editor.renderer.components.BaseComponent;

public class TalosComponent implements BaseComponent {
    public String particleName = "";
    public ParticleEffectInstance effect = null;

    @Override
    public void reset() {
        particleName = "";
        effect = null;
    }
}
