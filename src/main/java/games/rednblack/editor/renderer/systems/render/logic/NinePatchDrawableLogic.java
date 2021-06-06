package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.*;

public class NinePatchDrawableLogic implements Drawable {

	private ComponentMapper<TintComponent> tintComponentComponentMapper;
	private ComponentMapper<TransformComponent> transformMapper;
	private ComponentMapper<DimensionsComponent> dimensionsMapper;
	private ComponentMapper<NinePatchComponent> ninePatchMapper;


	public NinePatchDrawableLogic() {
		tintComponentComponentMapper = ComponentMapper.getFor(TintComponent.class);
		transformMapper = ComponentMapper.getFor(TransformComponent.class);
		dimensionsMapper = ComponentMapper.getFor(DimensionsComponent.class);
		ninePatchMapper = ComponentMapper.getFor(NinePatchComponent.class);
	}

	@Override
	public void draw(Batch batch, Entity entity, float parentAlpha, RenderingType renderingType) {
		TintComponent tintComponent = tintComponentComponentMapper.get(entity);
		TransformComponent entityTransformComponent = transformMapper.get(entity);
		DimensionsComponent entityDimensionsComponent = dimensionsMapper.get(entity);
		NinePatchComponent entityNinePatchComponent = ninePatchMapper.get(entity);
		batch.setColor(tintComponent.color);

		entityNinePatchComponent.ninePatch.draw(batch, entityTransformComponent.x, entityTransformComponent.y,
				entityTransformComponent.originX, entityTransformComponent.originY,
				entityDimensionsComponent.width, entityDimensionsComponent.height,
				entityTransformComponent.scaleX, entityTransformComponent.scaleY, entityTransformComponent.rotation);
	}

}
