package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.gdx.graphics.g2d.Batch;

public interface Drawable {
    enum RenderingType {TEXTURE, NORMAL_MAP}

    void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType);
}
