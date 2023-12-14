package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;

public class ThreadSafeBitmapFont extends BitmapFont {
    public ThreadSafeBitmapFont() {
    }

    public ThreadSafeBitmapFont(boolean flip) {
        super(flip);
    }

    public ThreadSafeBitmapFont(FileHandle fontFile, TextureRegion region) {
        super(fontFile, region);
    }

    public ThreadSafeBitmapFont(FileHandle fontFile, TextureRegion region, boolean flip) {
        super(fontFile, region, flip);
    }

    public ThreadSafeBitmapFont(FileHandle fontFile) {
        super(fontFile);
    }

    public ThreadSafeBitmapFont(FileHandle fontFile, boolean flip) {
        super(fontFile, flip);
    }

    public ThreadSafeBitmapFont(FileHandle fontFile, FileHandle imageFile, boolean flip) {
        super(fontFile, imageFile, flip);
    }

    public ThreadSafeBitmapFont(FileHandle fontFile, FileHandle imageFile, boolean flip, boolean integer) {
        super(fontFile, imageFile, flip, integer);
    }

    public ThreadSafeBitmapFont(BitmapFontData data, TextureRegion region, boolean integer) {
        super(data, region, integer);
    }

    public ThreadSafeBitmapFont(BitmapFontData data, Array<TextureRegion> pageRegions, boolean integer) {
        super(data, pageRegions, integer);
    }

    @Override
    public BitmapFontCache newFontCache() {
        return new ThreadSafeBitmapFontCache(this, usesIntegerPositions());
    }

    static private class ThreadSafeBitmapFontCache extends BitmapFontCache {

        public ThreadSafeBitmapFontCache(BitmapFont font) {
            super(font);
        }

        public ThreadSafeBitmapFontCache(BitmapFont font, boolean integer) {
            super(font, integer);
        }

        @Override
        public GlyphLayout addText(CharSequence str, float x, float y, int start, int end, float targetWidth, int halign, boolean wrap, String truncate) {
            return super.addText(str, x, y, start, end, targetWidth, halign, wrap, truncate);
        }
    }
}
