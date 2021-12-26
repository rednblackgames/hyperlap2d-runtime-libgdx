package games.rednblack.editor.renderer.resources;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import games.rednblack.editor.renderer.SceneConfiguration;

public class AsyncResourceManager extends ResourceManager {

    public AsyncResourceManager(SceneConfiguration sceneConfiguration) {
        super(sceneConfiguration);
    }

    public void addAtlasPack(String name, TextureAtlas pack) {
        this.atlasesPack.put(name, pack);
    }
}
