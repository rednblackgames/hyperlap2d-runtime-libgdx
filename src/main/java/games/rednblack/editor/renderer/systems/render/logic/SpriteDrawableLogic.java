package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Batch;

public class SpriteDrawableLogic extends TextureRegionDrawLogic {

	@Override
	public void draw(Batch batch, Entity entity, float parentAlpha, RenderingType renderingType) {
		super.draw(batch, entity, parentAlpha, renderingType);
		//TODO in case we need specific things 
	}

}
