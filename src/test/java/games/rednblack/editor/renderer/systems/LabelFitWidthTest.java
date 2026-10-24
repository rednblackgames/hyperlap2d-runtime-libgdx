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
 * A label that fits its text. The font is made by hand, every glyph moving the pen ten on, since
 * nothing here can load a real one: measuring a text needs the advances of its glyphs, not their
 * pictures.
 */
public class LabelFitWidthTest {

    private static final float ADVANCE = 10, WIDTH = 8;

    /** As GlyphLayout measures a line: every glyph's advance, but the last one only as wide as it is drawn. */
    private static float textWidth(int characters) {
        return (characters - 1) * ADVANCE + WIDTH;
    }

    private InputTestScene scene;
    private int label;

    @Before
    public void setUp() {
        scene = new InputTestScene(new LabelMeasureSystem(), new LabelSystem());
        label = scene.entity(scene.root, 0, 0, 200, 20);
        LabelComponent component = scene.engine.edit(label).create(LabelComponent.class);
        component.setStyle(new Label.LabelStyle(font(), null));
        component.setText("Sail");
        scene.engine.process();
    }

    /** Glyphs added after the font is made: making it would ask their page for a texture. */
    private static BitmapFont font() {
        BitmapFont.BitmapFontData data = new BitmapFont.BitmapFontData();
        data.lineHeight = 12;
        data.capHeight = 10;
        data.down = -12;
        BitmapFont font = new BitmapFont(data, new Array<>(new TextureRegion[]{new TextureRegion()}), true);
        for (char c = ' '; c <= 'z'; c++) {
            BitmapFont.Glyph glyph = new BitmapFont.Glyph();
            glyph.id = c;
            glyph.width = (int) WIDTH;
            glyph.height = 10;
            glyph.xadvance = (int) ADVANCE;
            data.setGlyph(c, glyph);
        }
        return font;
    }

    private LabelComponent component() {
        return scene.engine.getMapper(LabelComponent.class).get(label);
    }

    private float width() {
        return scene.engine.getMapper(DimensionsComponent.class).get(label).width;
    }

    @Test
    public void withoutItTheLabelKeepsTheWidthItWasGiven() {
        assertEquals(200, width(), 0.001f);
    }

    @Test
    public void itMakesTheLabelAsWideAsItsText() {
        component().fitWidth = true;
        scene.engine.process();
        assertEquals(textWidth(4), width(), 0.001f);
    }

    @Test
    public void itFollowsTheTextWhenTheTextChanges() {
        component().fitWidth = true;
        component().setText("Sail into the unknown");
        scene.engine.process();
        assertEquals(textWidth(21), width(), 0.001f);
    }

    @Test
    public void aLabelThatFitsItsTextNeverWraps() {
        component().wrap = true;
        component().fitWidth = true;
        component().setText("Sail into the unknown");
        scene.engine.process();
        assertEquals(textWidth(21), width(), 0.001f);
        assertEquals(1, component().layout.runs.size);
    }
}
