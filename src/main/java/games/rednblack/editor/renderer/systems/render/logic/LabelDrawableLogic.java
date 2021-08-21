package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class LabelDrawableLogic implements Drawable {

	protected ComponentMapper<LabelComponent> labelComponentMapper;
	protected ComponentMapper<TintComponent> tintComponentMapper;
	protected ComponentMapper<DimensionsComponent> dimensionsComponentMapper;
	protected ComponentMapper<TransformComponent> transformMapper;
	protected ComponentMapper<ParentNodeComponent> parentNodeComponentComponentMapper;

	private final Color tmpColor = new Color();

	@Override
	public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
		TransformComponent entityTransformComponent = transformMapper.get(entity);
		LabelComponent labelComponent = labelComponentMapper.get(entity);
		DimensionsComponent dimensionsComponent = dimensionsComponentMapper.get(entity);
		TintComponent tint = tintComponentMapper.get(entity);

		tmpColor.set(tint.color);

		if (labelComponent.style.background != null) {
			batch.setColor(tmpColor);
			labelComponent.style.background.draw(batch, entityTransformComponent.x, entityTransformComponent.y, dimensionsComponent.width, dimensionsComponent.height);
		}

		if(labelComponent.style.fontColor != null) tmpColor.mul(labelComponent.style.fontColor);
		tmpColor.a *= tintComponentMapper.get(parentNodeComponentComponentMapper.get(entity).parentEntity).color.a;

		TransformMathUtils.computeTransform(entityTransformComponent).mulLeft(batch.getTransformMatrix());
		TransformMathUtils.applyTransform(batch, entityTransformComponent);

		labelComponent.cache.tint(tmpColor);
		labelComponent.cache.draw(batch);

		TransformMathUtils.resetTransform(batch, entityTransformComponent);
	}

}
