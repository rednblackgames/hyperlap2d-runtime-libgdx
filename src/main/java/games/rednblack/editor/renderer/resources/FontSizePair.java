package games.rednblack.editor.renderer.resources;

/**
 * Created by azakhary on 9/9/2014.
 */
public class FontSizePair {

    public String fontName;
    public int fontSize;
    public boolean monoSpace;

    public FontSizePair(String name, int size, boolean mono) {
        fontName = name;
        fontSize = size;
        monoSpace = mono;
    }

    @Override
    public boolean equals(Object arg0) {
        FontSizePair arg = (FontSizePair)arg0;
        if(arg.fontName.equals(fontName) && arg.fontSize == fontSize && arg.monoSpace == monoSpace) return true;

        return false;
    }

    @Override
    public String toString() {
        return fontName + "_" + fontSize + "_" + monoSpace;
    }

    @Override
    public int hashCode() {
        return (fontName + "_" + fontSize + "_" + monoSpace).hashCode();
    }


}
