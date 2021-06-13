package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.BaseComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.label.TypingLabelComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class LabelDrawableLogic implements Drawable {

	protected BaseComponentMapper<LabelComponent> labelComponentMapper;
	protected BaseComponentMapper<TypingLabelComponent> typingLabelComponentMapper;
	protected BaseComponentMapper<TintComponent> tintComponentMapper;
	protected BaseComponentMapper<DimensionsComponent> dimensionsComponentMapper;
	protected BaseComponentMapper<TransformComponent> transformMapper;
	protected BaseComponentMapper<ParentNodeComponent> parentNodeComponentComponentMapper;

	private final Color tmpColor = new Color();

	public void init() {
		labelComponentMapper = ComponentRetriever.getMapper(LabelComponent.class);
		tintComponentMapper = ComponentRetriever.getMapper(TintComponent.class);
		dimensionsComponentMapper = ComponentRetriever.getMapper(DimensionsComponent.class);
		transformMapper = ComponentRetriever.getMapper(TransformComponent.class);
		parentNodeComponentComponentMapper = ComponentRetriever.getMapper(ParentNodeComponent.class);
		typingLabelComponentMapper = ComponentRetriever.getMapper(TypingLabelComponent.class);
	}
	
	@Override
	public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
		if(labelComponentMapper==null) init(); // TODO: Can we have an injection for this object?

		TransformComponent entityTransformComponent = transformMapper.get(entity);
		LabelComponent labelComponent = labelComponentMapper.get(entity);
		DimensionsComponent dimensionsComponent = dimensionsComponentMapper.get(entity);
		TintComponent tint = tintComponentMapper.get(entity);
		TypingLabelComponent typingLabelComponent = typingLabelComponentMapper.get(entity);

		tmpColor.set(tint.color);

		if (labelComponent.style.background != null) {
			batch.setColor(tmpColor);
			labelComponent.style.background.draw(batch, entityTransformComponent.x, entityTransformComponent.y, dimensionsComponent.width, dimensionsComponent.height);
		}

		if(labelComponent.style.fontColor != null) tmpColor.mul(labelComponent.style.fontColor);
		tmpColor.a *= tintComponentMapper.get(parentNodeComponentComponentMapper.get(entity).parentEntity).color.a;

		TransformMathUtils.computeTransform(entity).mulLeft(batch.getTransformMatrix());
		TransformMathUtils.applyTransform(entity, batch);

		if (typingLabelComponent == null) {
			labelComponent.cache.tint(tmpColor);
			labelComponent.cache.draw(batch);
		} else {
			typingLabelComponent.typingLabel.setColor(tmpColor);
			typingLabelComponent.typingLabel.draw(batch, 1);
		}

		TransformMathUtils.resetTransform(entity, batch);
	}

}
