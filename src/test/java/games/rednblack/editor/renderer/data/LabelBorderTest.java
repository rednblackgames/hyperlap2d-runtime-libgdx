package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.factory.component.LabelComponentFactory;
import games.rednblack.editor.renderer.resources.FontSizePair;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.HyperJson;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A label's outline is a layer of its own under the text: two fonts made with the same outline
 * width, the outline's and the text's, so the tint colours the text and the outline keeps its own
 * colour. No colour is in either font: a label without an outline shares the font it always had,
 * and labels with the same outline width share the pair whatever their colours.
 */
public class LabelBorderTest {

    private static final float[] INK = {0.07f, 0.07f, 0.07f, 1};

    @Test
    public void withoutAnOutlineAFontIsTheOneItAlwaysWas() {
        FontSizePair plain = new FontSizePair("Noto Serif Bold", 39, false);
        assertEquals(plain, new FontSizePair("Noto Serif Bold", 39, false, 0, false));
        assertEquals(plain, new FontSizePair("Noto Serif Bold", 39, false, 0, true));
        assertEquals(plain.hashCode(), new FontSizePair("Noto Serif Bold", 39, false, 0, true).hashCode());
        assertEquals("Noto Serif Bold_39_false", plain.toString());
    }

    @Test
    public void anOutlineMakesTwoFontsOfItsOwnAndNoColourIsInThem() {
        FontSizePair plain = new FontSizePair("Noto Serif Bold", 39, false);
        FontSizePair text = new FontSizePair("Noto Serif Bold", 39, false, 2, false);
        FontSizePair outline = new FontSizePair("Noto Serif Bold", 39, false, 2, true);
        assertNotEquals(plain, text);
        assertNotEquals(text, outline);
        assertNotEquals(outline, new FontSizePair("Noto Serif Bold", 39, false, 3, true));
        assertEquals(outline, new FontSizePair("Noto Serif Bold", 39, false, 2, true));
    }

    @Test
    public void aSceneAsksForBothLayersOfAnOutlinedLabel() {
        CompositeItemVO composite = new CompositeItemVO();
        composite.addItem(label(0));
        LabelVO sepia = label(2);
        sepia.tint = new float[]{0.24f, 0.16f, 0.09f, 1};
        composite.addItem(sepia);
        composite.addItem(label(2));
        Array<FontSizePair> fonts = composite.getRecursiveFontList();

        assertEquals("two outlined labels of any colour share one pair", 3, fonts.size);
        assertTrue(fonts.contains(new FontSizePair("Noto Serif Bold", 39, false), false));
        assertTrue(fonts.contains(new FontSizePair("Noto Serif Bold", 39, false, 2, false), false));
        assertTrue(fonts.contains(new FontSizePair("Noto Serif Bold", 39, false, 2, true), false));
    }

    @Test
    public void theOutlineIsSavedOnlyWhenThereIsOne() {
        String plain = HyperJson.newJson().toJson(label(0));
        assertFalse(plain.contains("border"));

        String outlined = HyperJson.newJson().toJson(label(2));
        LabelVO read = HyperJson.newJson().fromJson(LabelVO.class, outlined);
        assertEquals(2, read.borderWidth, 0);
        assertEquals(INK[0], read.borderColor[0], 1e-6);
        assertEquals(INK[0], new LabelVO(read).borderColor[0], 1e-6);
    }

    @Test
    public void aLabelAsksForTheTextLayerAndTheOutlineLayer() {
        LabelComponent component = new LabelComponent();
        component.fontName = "Noto Serif Bold";
        component.fontSize = 39;
        LabelComponentFactory.setBorder(component, label(2));
        assertEquals(INK[0], component.borderColor.r, 1e-6);

        Array<FontSizePair> asked = new Array<>();
        IResourceRetriever retriever = (IResourceRetriever) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IResourceRetriever.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getFont") && args.length == 5) {
                        asked.add(new FontSizePair((String) args[0], (int) args[1], (boolean) args[2], (float) args[3], (boolean) args[4]));
                    }
                    return null;
                });
        LabelComponentFactory.generateStyle(retriever, component);
        LabelComponentFactory.outlineFont(retriever, component);

        assertEquals(new FontSizePair("Noto Serif Bold", 39, false, 2, false), asked.get(0));
        assertEquals(new FontSizePair("Noto Serif Bold", 39, false, 2, true), asked.get(1));
    }

    @Test
    public void withoutAnOutlineThereIsNoOutlineLayer() {
        LabelComponent component = new LabelComponent();
        component.fontName = "Noto Serif Bold";
        component.fontSize = 39;
        LabelComponentFactory.setBorder(component, label(0));
        assertNull(LabelComponentFactory.outlineFont(null, component));
    }

    private static LabelVO label(float border) {
        LabelVO vo = new LabelVO();
        vo.style = "Noto Serif Bold";
        vo.size = 39;
        if (border > 0) {
            vo.borderWidth = border;
            vo.borderColor = INK.clone();
        }
        return vo;
    }
}
