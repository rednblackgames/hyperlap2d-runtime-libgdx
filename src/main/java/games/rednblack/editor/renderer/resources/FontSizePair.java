package games.rednblack.editor.renderer.resources;

/**
 * Created by azakhary on 9/9/2014.
 * <p>
 * A label with an outline is drawn in two layers, so its tint can be the text's colour and its
 * outline keep its own: the <b>outline</b> layer, the glyphs swollen by the outline, all white, drawn
 * in the outline's colour; and over it the <b>text</b> layer, the glyphs alone, white, drawn in the
 * tint. FreeType makes both with the same outline width, which is what gives them the same metrics
 * to the pixel — an outline widens every advance, so a plain font would not line up. No colour is in
 * either: every label with the same font, size and outline width shares the pair.
 */
public class FontSizePair {

    public String fontName;
    public int fontSize;
    public boolean monoSpace;
    /** The outline FreeType draws around every glyph, in pixels at {@link #fontSize}: 0 for none. */
    public float borderWidth;
    /** With an outline, whether this is the outline layer rather than the text layer. */
    public boolean outline;

    public FontSizePair(String name, int size, boolean mono) {
        this(name, size, mono, 0, false);
    }

    public FontSizePair(String name, int size, boolean mono, float borderWidth, boolean outline) {
        fontName = name;
        fontSize = size;
        monoSpace = mono;
        this.borderWidth = Math.max(0, borderWidth);
        this.outline = this.borderWidth > 0 && outline;
    }

    @Override
    public boolean equals(Object arg0) {
        FontSizePair arg = (FontSizePair)arg0;
        return arg.fontName.equals(fontName) && arg.fontSize == fontSize && arg.monoSpace == monoSpace
                && arg.borderWidth == borderWidth && arg.outline == outline;
    }

    @Override
    public String toString() {
        if (borderWidth > 0) return fontName + "_" + fontSize + "_" + monoSpace + "_" + borderWidth + (outline ? "_outline" : "_text");
        return fontName + "_" + fontSize + "_" + monoSpace;
    }

    @Override
    public int hashCode() {
        int hash = fontName.hashCode();
        hash = 31 * hash + fontSize;
        hash = 31 * hash + (monoSpace ? 1 : 0);
        hash = 31 * hash + Float.floatToIntBits(borderWidth);
        return 31 * hash + (outline ? 1 : 0);
    }
}
