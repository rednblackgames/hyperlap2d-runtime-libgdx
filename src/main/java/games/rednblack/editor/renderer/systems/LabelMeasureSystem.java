package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Align;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;

/**
 * The first step of {@link LabelSystem}: labels that fit their text take its size before
 * {@link ParentLocalAABBSystem}, {@link LayoutSystem} and {@link AutoLayoutSystem} read it, so what
 * is placed around a new text follows in the same frame. A wrapping label is measured in the width
 * it has when the frame starts; a width the layout changes is seen the frame after.
 */
@All(LabelComponent.class)
public class LabelMeasureSystem extends IteratingSystem {
	protected ComponentMapper<LabelComponent> labelComponentMapper;
	protected ComponentMapper<DimensionsComponent> dimensionComponentMapper;

	@Override
	protected void process(int entity) {
		LabelComponent labelComponent = labelComponentMapper.get(entity);
		if (labelComponent.typingEffect || !(labelComponent.fitWidth || labelComponent.fitHeight))
			return;
		DimensionsComponent dimensionsComponent = dimensionComponentMapper.get(entity);

		BitmapFont font = labelComponent.cache.getFont();

		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		float fontScaleX = labelComponent.fontScaleX;
		float fontScaleY = labelComponent.fontScaleY;
		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(fontScaleX, fontScaleY);

		//a label that fits its width is one line: the text unwrapped is already the one to measure the height of
		if (labelComponent.fitWidth) fitWidth(labelComponent, dimensionsComponent, font);
		else labelComponent.layout.setText(font, labelComponent.text, 0, labelComponent.text.length(), Color.WHITE, dimensionsComponent.width, labelComponent.lineAlign, labelComponent.wrap, null);
		if (labelComponent.fitHeight) fitHeight(dimensionsComponent, font, labelComponent.layout.height);

		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(oldScaleX, oldScaleY);
	}

	/**
	 * The height of the text as laid out, given to the label and to its bounds: an auto layout reads
	 * it from there. As scene2d's label measures it, a descent more at either end, so the vertical
	 * alignment keeps the last line's descenders inside. The label keeps its bottom where it is.
	 */
	private void fitHeight(DimensionsComponent dimensionsComponent, BitmapFont font, float textHeight) {
		float height = textHeight - font.getDescent() * 2;
		if (dimensionsComponent.height == height) return;
		dimensionsComponent.height = height;
		if (dimensionsComponent.boundBox != null) dimensionsComponent.boundBox.height = height;
	}

	/** The width of the text unwrapped, given to the label and to its bounds: an auto layout reads it from there. */
	private void fitWidth(LabelComponent labelComponent, DimensionsComponent dimensionsComponent, BitmapFont font) {
		labelComponent.layout.setText(font, labelComponent.text, 0, labelComponent.text.length(), Color.WHITE, 0, Align.left, false, null);
		float width = labelComponent.layout.width;
		if (dimensionsComponent.width == width) return;
		dimensionsComponent.width = width;
		if (dimensionsComponent.boundBox != null) dimensionsComponent.boundBox.width = width;
	}
}
