package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.annotations.Exclude;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Align;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.label.TypingLabelComponent;

@All(LabelComponent.class)
@Exclude(TypingLabelComponent.class)
public class LabelSystem extends IteratingSystem {
	protected ComponentMapper<LabelComponent> labelComponentMapper;
	protected ComponentMapper<DimensionsComponent> dimensionComponentMapper;

	@Override
	protected void process(int entity) {
		LabelComponent labelComponent = labelComponentMapper.get(entity);
		DimensionsComponent dimensionsComponent = dimensionComponentMapper.get(entity);
		
		BitmapFont font = labelComponent.cache.getFont();
		
		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		float fontScaleX = labelComponent.fontScaleX;
		float fontScaleY = labelComponent.fontScaleY;
		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(fontScaleX, fontScaleY);
		
		//horisontal Align
		
		float textWidth = labelComponent.layout.width;
		float textHeight = labelComponent.layout.height;
		float textX = 0;
		
		/*if (labelComponent.wrap || labelComponent.text.indexOf("\n") != -1) {
			// If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
			labelComponent.layout.setText(font, labelComponent.text, 0, labelComponent.text.length, Color.WHITE, dimensionsComponent.width, labelComponent.lineAlign, labelComponent.wrap, null);
			textWidth = labelComponent.layout.width;
			textHeight = labelComponent.layout.height;

			if ((labelComponent.lineAlign  & Align.left) == 0) {
				if ((labelComponent.lineAlign & Align.right) != 0)
					textX += dimensionsComponent.width - textWidth;
				else
					textX += (dimensionsComponent.width - textWidth) / 2;
			}
		}*/
		
		//vertical Align
		float textY = textHeight;
		if ((labelComponent.labelAlign & Align.top) != 0) {
			textY += labelComponent.cache.getFont().isFlipped() ? 0 : dimensionsComponent.height - textHeight;
			textY += labelComponent.style.font.getDescent();
		} else if ((labelComponent.labelAlign & Align.bottom) != 0) {
			textY += labelComponent.cache.getFont().isFlipped() ? dimensionsComponent.height - textHeight : 0;
			textY -= labelComponent.style.font.getDescent();
		} else {
			textY += (dimensionsComponent.height - textHeight) / 2;
		}
		
		labelComponent.layout.setText(font, labelComponent.text, 0, labelComponent.text.length, Color.WHITE, dimensionsComponent.width, labelComponent.lineAlign, labelComponent.wrap, null);
		labelComponent.cache.setText(labelComponent.layout, textX, textY);
		
		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(oldScaleX, oldScaleY);
	}
	
//	private void scaleAndComputePrefSize () {
//		BitmapFont font = labelComponent.cache.getFont();
//		float oldScaleX = font.getScaleX();
//		float oldScaleY = font.getScaleY();
//		float fontScaleX = labelComponent.fontScaleX;
//		float fontScaleY = labelComponent.fontScaleY;
//		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(fontScaleX, fontScaleY);
//
//		computePrefSize();
//
//		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(oldScaleX, oldScaleY);
//	}
//
//	private void computePrefSize () {
//		if (labelComponent.wrap) {
//			float width = dimensionsComponent.width;
//			if (labelComponent.style.background != null) width -= labelComponent.style.background.getLeftWidth() + labelComponent.style.background.getRightWidth();
//			labelComponent.layout.setText(labelComponent.cache.getFont(), labelComponent.text, Color.WHITE, width, Align.left, true);
//		} else
//			labelComponent.layout.setText(labelComponent.cache.getFont(), labelComponent.text);
//		labelComponent.prefSize.set(labelComponent.layout.width, labelComponent.layout.height);
//	}
//	
//	public float getPrefWidth () {
//		if (labelComponent.wrap) return 0;
//		scaleAndComputePrefSize();
//		float width = labelComponent.prefSize.x;
//		Drawable background = labelComponent.style.background;
//		if (background != null) width += background.getLeftWidth() + background.getRightWidth();
//		return width;
//	}
//
//	public float getPrefHeight () {
//		scaleAndComputePrefSize();
//		float height = labelComponent.prefSize.y - labelComponent.style.font.getDescent() * labelComponent.fontScaleY * 2;
//		Drawable background = labelComponent.style.background;
//		if (background != null) height += background.getTopHeight() + background.getBottomHeight();
//		return height;
//	}
//	
//	public void layout () {
//		BitmapFont font = labelComponent.cache.getFont();
//		float oldScaleX = font.getScaleX();
//		float oldScaleY = font.getScaleY();
//		float fontScaleX = labelComponent.fontScaleX;
//		float fontScaleY = labelComponent.fontScaleY;
//		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(fontScaleX, fontScaleY);
//
//		boolean wrap = labelComponent.wrap;
//
//		float width = dimensionsComponent.width, height = dimensionsComponent.height;
//		Drawable background = labelComponent.style.background;
//		float x = 0, y = 0;
//		if (background != null) {
//			x = background.getLeftWidth();
//			y = background.getBottomHeight();
//			width -= background.getLeftWidth() + background.getRightWidth();
//			height -= background.getBottomHeight() + background.getTopHeight();
//		}
//
//		GlyphLayout layout = labelComponent.layout;
//		float textWidth, textHeight;
//		StringBuilder text = labelComponent.text;
//		int labelAlign = labelComponent.labelAlign;
//		int lineAlign = labelComponent.lineAlign;
//		if (wrap || text .indexOf("\n") != -1) {
//			// If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
//			layout.setText(font, text, 0, text.length, Color.WHITE, width, lineAlign , wrap, null);
//			textWidth = layout.width;
//			textHeight = layout.height;
//
//			if ((labelAlign  & Align.left) == 0) {
//				if ((labelAlign & Align.right) != 0)
//					x += width - textWidth;
//				else
//					x += (width - textWidth) / 2;
//			}
//		} else {
//			textWidth = width;
//			textHeight = font.getData().capHeight;
//		}
//
//		if ((labelAlign & Align.top) != 0) {
//			y += labelComponent.cache.getFont().isFlipped() ? 0 : height - textHeight;
//			y += labelComponent.style.font.getDescent();
//		} else if ((labelAlign & Align.bottom) != 0) {
//			y += labelComponent.cache.getFont().isFlipped() ? height - textHeight : 0;
//			y -= labelComponent.style.font.getDescent();
//		} else {
//			y += (height - textHeight) / 2;
//		}
//		if (!labelComponent.cache.getFont().isFlipped()) y += textHeight;
//
//		layout.setText(font, text, 0, text.length, Color.WHITE, textWidth, lineAlign, wrap, null);
//		labelComponent.cache.setText(layout, x, y);
//
//		if (fontScaleX != 1 || fontScaleY != 1) font.getData().setScale(oldScaleX, oldScaleY);
//	}

}
