package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A label that is as tall as its text. The font is made by hand, as in {@link LabelFitWidthTest}:
 * every glyph moves the pen ten on, a line is 12 down, the caps are 10 high and the descent is 3.
 * The label is 50 wide, so a word of four letters fits and two do not.
 */
public class LabelFitHeightTest {

    private static final float CAP = 10, LINE = 12, DESCENT = 3;

    /** As GlyphLayout measures it, caps of the first line to the baseline of the last, a descent at either end. */
    static float textHeight(int lines) {
        return CAP + (lines - 1) * LINE + 2 * DESCENT;
    }

    private InputTestScene scene;
    private int label;

    @Before
    public void setUp() {
        scene = new InputTestScene(new LabelMeasureSystem(), new LabelSystem());
        label = scene.entity(scene.root, 0, 0, 50, 100);
        LabelComponent component = scene.engine.edit(label).create(LabelComponent.class);
        component.setStyle(new Label.LabelStyle(font(), null));
        component.setWrap(true);
        component.setText("Sail");
        scene.engine.process();
    }

    /** Glyphs added after the font is made: making it would ask their page for a texture. */
    static BitmapFont font() {
        BitmapFont.BitmapFontData data = new BitmapFont.BitmapFontData();
        data.lineHeight = LINE;
        data.capHeight = CAP;
        data.down = -LINE;
        data.descent = -DESCENT;
        BitmapFont font = new BitmapFont(data, new Array<>(new TextureRegion[]{new TextureRegion()}), true);
        for (char c = ' '; c <= 'z'; c++) {
            BitmapFont.Glyph glyph = new BitmapFont.Glyph();
            glyph.id = c;
            glyph.width = 8;
            glyph.height = 10;
            glyph.xadvance = 10;
            data.setGlyph(c, glyph);
        }
        return font;
    }

    private LabelComponent component() {
        return scene.engine.getMapper(LabelComponent.class).get(label);
    }

    private DimensionsComponent dimensions() {
        return scene.engine.getMapper(DimensionsComponent.class).get(label);
    }

    @Test
    public void withoutItTheLabelKeepsTheHeightItWasGiven() {
        assertEquals(100, dimensions().height, 0.001f);
    }

    @Test
    public void itMakesTheLabelAsTallAsItsText() {
        component().fitHeight = true;
        scene.engine.process();
        assertEquals(textHeight(1), dimensions().height, 0.001f);
    }

    @Test
    public void itGrowsWithTheLinesTheTextWrapsInto() {
        component().fitHeight = true;
        component().setText("Sail into dark seas");
        scene.engine.process();
        assertEquals(textHeight(4), dimensions().height, 0.001f);
    }

    @Test
    public void itKeepsTheWidthItWasGiven() {
        component().fitHeight = true;
        component().setText("Sail into dark seas");
        scene.engine.process();
        assertEquals(50, dimensions().width, 0.001f);
    }

    @Test
    public void withTheWidthFittedTooItIsOneLineHigh() {
        component().fitWidth = true;
        component().fitHeight = true;
        component().setText("Sail into dark seas");
        scene.engine.process();
        assertEquals(textHeight(1), dimensions().height, 0.001f);
    }
}
