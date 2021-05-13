package games.rednblack.editor.renderer.components;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class NormalTextureRegionComponent implements BaseComponent {
    TextureRegion textureRegion = null;

    @Override
    public void reset() {
        textureRegion = null;
    }
}
