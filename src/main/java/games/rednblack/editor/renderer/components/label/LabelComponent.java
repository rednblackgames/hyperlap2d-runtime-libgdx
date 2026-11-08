package games.rednblack.editor.renderer.components.label;

import com.badlogic.gdx.graphics.Color;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.CharArray;

public class LabelComponent extends PooledComponent {
	public transient LabelStyle style;
	public transient final GlyphLayout layout = new GlyphLayout();
	public transient BitmapFontCache cache;
	/**
	 * The outline layer, drawn under the text in {@link #borderColor}: its font, its own layout and
	 * cache, since a layout holds the glyphs of the font it was made with. Null without an outline.
	 */
	public transient BitmapFont outlineFont;
	public transient final GlyphLayout outlineLayout = new GlyphLayout();
	public transient BitmapFontCache outlineCache;

	public CharArray text = new CharArray();
	public String fontName;
	public int fontSize;
	public int labelAlign = Align.center;
	public int lineAlign = Align.center;
	public boolean wrap;
	/** Makes the label as wide as its text, on one line: it never wraps then, whatever {@link #wrap} says. */
	public boolean fitWidth;
	/** Makes the label as tall as its text, laid out in the width it has: wrapped, as many lines as that takes. */
	public boolean fitHeight;
	public boolean mono;
	/** An outline around every glyph, in pixels at {@link #fontSize}, drawn by FreeType: 0 for none. It is a layer
	 * of its own under the text, in {@link #borderColor}, so the tint colours the text and never the outline. */
	public float borderWidth;
	public final Color borderColor = new Color(Color.BLACK);
	public float fontScaleX = 1f;
	public float fontScaleY = 1f;

	public boolean typingEffect = false;

	public String bitmapFont = null;

	public LabelComponent() {

	}

	public void setStyle (LabelStyle style) {
		if (style == null) throw new IllegalArgumentException("style cannot be null.");
		if (style.font == null) throw new IllegalArgumentException("Missing LabelStyle font.");
		this.style = style;
		cache = style.font.newFontCache();
	}

	/** The outline layer's font, made with the same outline width as the text's; null for none. */
	public void setOutlineFont (BitmapFont font) {
		outlineFont = font;
		outlineLayout.reset();
		outlineCache = font == null ? null : font.newFontCache();
	}
	
	public LabelStyle getStyle () {
		return style;
	}

	/** @param newText May be null, "" will be used. */
	public void setText (CharSequence newText) {
		if (newText == null) newText = "";
		if (newText instanceof StringBuilder) {
			if (text.equals(newText)) return;
			text.setLength(0);
			text.append((StringBuilder)newText);
		} else {
			if (textEquals(newText)) return;
			text.setLength(0);
			text.append(newText);
		}
	}
	
	public boolean textEquals (CharSequence other) {
		int length = text.length();
		char[] chars = text.items;
		if (length != other.length()) return false;
		for (int i = 0; i < length; i++)
			if (chars[i] != other.charAt(i)) return false;
		return true;
	}

	public CharArray getText () {
		return text;
	}
	
	public GlyphLayout getGlyphLayout () {
		return layout;
	}

	/** If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds.
	 * If true, the text will word wrap using the width of the label. The preferred width of the label will be 0, it is expected
	 * that the something external will set the width of the label. Wrapping will not occur when ellipsis is true. Default is
	 * false.
	 * <p>
	 * When wrap is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label
	 * will need to layout twice: once to set the width of the label and a second time to adjust to the label's new preferred
	 * height. */
	public void setWrap (boolean wrap) {
		this.wrap = wrap;
	}

	/** @param alignment Aligns each line of text horizontally and all the text vertically.
	 * @see Align */
	public void setAlignment (int alignment) {
		setAlignment(alignment, alignment);
	}

	/** @param labelAlign Aligns all the text with the label widget.
	 * @param lineAlign Aligns each line of text (left, right, or center).
	 * @see Align */
	public void setAlignment (int labelAlign, int lineAlign) {
		this.labelAlign = labelAlign;

		if ((lineAlign & Align.left) != 0)
			this.lineAlign = Align.left;
		else if ((lineAlign & Align.right) != 0)
			this.lineAlign = Align.right;
		else
			this.lineAlign = Align.center;
	}
	
	public void setFontScale (float fontScale) {
		this.fontScaleX = fontScale;
		this.fontScaleY = fontScale;
	}

	public void setFontScale (float fontScaleX, float fontScaleY) {
		this.fontScaleX = fontScaleX;
		this.fontScaleY = fontScaleY;
	}

	public float getFontScaleX () {
		return fontScaleX;
	}

	public void setFontScaleX (float fontScaleX) {
		this.fontScaleX = fontScaleX;
	}

	public float getFontScaleY () {
		return fontScaleY;
	}

	public void setFontScaleY (float fontScaleY) {
		this.fontScaleY = fontScaleY;
	}

	@Override
	public void reset() {
		style = null;
		layout.reset();
		cache = null;

		text.delete(0,text.length());
		fontName = null;
		fontSize = 0;
		labelAlign = Align.center;
		lineAlign = Align.center;
		wrap = false;
		fitWidth = false;
		fitHeight = false;
		mono = false;
		borderWidth = 0;
		borderColor.set(Color.BLACK);
		outlineFont = null;
		outlineLayout.reset();
		outlineCache = null;
		fontScaleX = 1f;
		fontScaleY = 1f;

		typingEffect = false;
		bitmapFont = null;
	}
}
