package games.rednblack.editor.renderer.resources;

import com.badlogic.gdx.graphics.Color;

/**
 * Created by azakhary on 9/9/2014.
 */
public class FontSizePair {

    public String fontName;
    public int fontSize;
    public boolean monoSpace;
    /** The outline FreeType draws around every glyph, in pixels at {@link #fontSize}: 0 for none. */
    public float borderWidth;
    /** Its colour, as {@link Color#rgba8888(Color)}: 0 when there is no outline, so all such fonts are one. */
    public int borderColor;

    public FontSizePair(String name, int size, boolean mono) {
        this(name, size, mono, 0, 0);
    }

    public FontSizePair(String name, int size, boolean mono, float borderWidth, int borderColor) {
        fontName = name;
        fontSize = size;
        monoSpace = mono;
        this.borderWidth = Math.max(0, borderWidth);
        this.borderColor = this.borderWidth > 0 ? borderColor : 0;
    }

    /** A label's outline colour, from its VO's components. */
    public static int rgba8888(float[] color) {
        return color == null ? Color.rgba8888(Color.BLACK) : Color.rgba8888(color[0], color[1], color[2], color[3]);
    }

    @Override
    public boolean equals(Object arg0) {
        FontSizePair arg = (FontSizePair)arg0;
        return arg.fontName.equals(fontName) && arg.fontSize == fontSize && arg.monoSpace == monoSpace
                && arg.borderWidth == borderWidth && arg.borderColor == borderColor;
    }

    @Override
    public String toString() {
        if (borderWidth > 0) return fontName + "_" + fontSize + "_" + monoSpace + "_" + borderWidth + "_" + Integer.toHexString(borderColor);
        return fontName + "_" + fontSize + "_" + monoSpace;
    }

    @Override
    public int hashCode() {
        int hash = fontName.hashCode();
        hash = 31 * hash + fontSize;
        hash = 31 * hash + (monoSpace ? 1 : 0);
        hash = 31 * hash + Float.floatToIntBits(borderWidth);
        return 31 * hash + borderColor;
    }
}
