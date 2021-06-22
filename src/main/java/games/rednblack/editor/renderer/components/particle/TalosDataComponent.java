package games.rednblack.editor.renderer.components.particle;

import com.artemis.PooledComponent;

public class TalosDataComponent extends PooledComponent {

    public String particleName = "";
    public boolean transform = true;

    @Override
    public void reset() {
        particleName = "";
        transform = true;
    }
}
