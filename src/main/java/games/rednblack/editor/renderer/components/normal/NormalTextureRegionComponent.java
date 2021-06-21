package games.rednblack.editor.renderer.components.normal;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class NormalTextureRegionComponent extends PooledComponent {
    public TextureRegion textureRegion = null;

    @Override
    public void reset() {
        textureRegion = null;
    }
}
