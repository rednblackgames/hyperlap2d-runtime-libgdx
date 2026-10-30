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
import static org.junit.Assert.assertTrue;

/**
 * A label's outline: part of the font it asks for, so a label with one gets a font of its own, and
 * one without shares the font it always had, whatever colour it was left with.
 */
public class LabelBorderTest {

    private static final int INK = Color.rgba8888(Color.valueOf("111111ff"));

    @Test
    public void withoutAnOutlineAFontIsTheOneItAlwaysWas() {
        FontSizePair plain = new FontSizePair("Noto Serif Bold", 39, false);
        assertEquals(plain, new FontSizePair("Noto Serif Bold", 39, false, 0, INK));
        assertEquals(plain.hashCode(), new FontSizePair("Noto Serif Bold", 39, false, 0, INK).hashCode());
        assertEquals("Noto Serif Bold_39_false", plain.toString());
    }

    @Test
    public void theOutlineMakesAFontOfItsOwn() {
        FontSizePair plain = new FontSizePair("Noto Serif Bold", 39, false);
        FontSizePair outlined = new FontSizePair("Noto Serif Bold", 39, false, 2, INK);
        assertNotEquals(plain, outlined);
        assertNotEquals(outlined, new FontSizePair("Noto Serif Bold", 39, false, 3, INK));
        assertNotEquals(outlined, new FontSizePair("Noto Serif Bold", 39, false, 2, Color.rgba8888(Color.WHITE)));
        assertEquals(outlined, new FontSizePair("Noto Serif Bold", 39, false, 2, INK));
    }

    @Test
    public void aSceneAsksForTheFontsItsLabelsUse() {
        CompositeItemVO composite = new CompositeItemVO();
        composite.addItem(label(0));
        composite.addItem(label(2));
        Array<FontSizePair> fonts = composite.getRecursiveFontList();

        assertEquals(2, fonts.size);
        assertTrue(fonts.contains(new FontSizePair("Noto Serif Bold", 39, false), false));
        assertTrue(fonts.contains(new FontSizePair("Noto Serif Bold", 39, false, 2, INK), false));
    }

    @Test
    public void theOutlineIsSavedOnlyWhenThereIsOne() {
        String plain = HyperJson.newJson().toJson(label(0));
        assertFalse(plain.contains("border"));

        String outlined = HyperJson.newJson().toJson(label(2));
        LabelVO read = HyperJson.newJson().fromJson(LabelVO.class, outlined);
        assertEquals(2, read.borderWidth, 0);
        assertEquals(INK, FontSizePair.rgba8888(read.borderColor));
        assertEquals(INK, FontSizePair.rgba8888(new LabelVO(read).borderColor));
    }

    @Test
    public void aLabelAsksForItsFontWithItsOutline() {
        LabelComponent component = new LabelComponent();
        component.fontName = "Noto Serif Bold";
        component.fontSize = 39;
        LabelComponentFactory.setBorder(component, label(2));

        FontSizePair[] asked = new FontSizePair[1];
        IResourceRetriever retriever = (IResourceRetriever) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IResourceRetriever.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getFont") && args.length == 5) {
                        asked[0] = new FontSizePair((String) args[0], (int) args[1], (boolean) args[2], (float) args[3], (int) args[4]);
                    }
                    return null;
                });
        LabelComponentFactory.generateStyle(retriever, component);

        assertEquals(new FontSizePair("Noto Serif Bold", 39, false, 2, INK), asked[0]);
    }

    private static LabelVO label(float border) {
        LabelVO vo = new LabelVO();
        vo.style = "Noto Serif Bold";
        vo.size = 39;
        if (border > 0) {
            vo.borderWidth = border;
            Color ink = Color.valueOf("111111ff");
            vo.borderColor = new float[]{ink.r, ink.g, ink.b, ink.a};
        }
        return vo;
    }
}
