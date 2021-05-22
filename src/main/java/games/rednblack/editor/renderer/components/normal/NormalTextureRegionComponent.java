package games.rednblack.editor.renderer.components.normal;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import games.rednblack.editor.renderer.components.BaseComponent;

public class NormalTextureRegionComponent implements BaseComponent {
    public TextureRegion textureRegion = null;

    @Override
    public void reset() {
        textureRegion = null;
    }
}
