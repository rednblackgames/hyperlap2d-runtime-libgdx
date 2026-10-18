package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.Json;
import games.rednblack.editor.renderer.data.CompositeItemVO;
import games.rednblack.editor.renderer.data.SimpleImageVO;
import games.rednblack.editor.renderer.data.WidgetPartVO;
import games.rednblack.editor.renderer.data.WidgetVO;
import games.rednblack.editor.renderer.utils.HyperJson;
import games.rednblack.editor.renderer.widget.handlers.CoreStateOverrides;
import org.junit.Test;

import static org.junit.Assert.*;

public class WidgetSerializationTest {

    @Test
    public void widgetDataSurvivesJsonRoundTrip() {
        CompositeItemVO button = new CompositeItemVO();
        button.widget = new WidgetVO();
        button.widget.widgetType = WidgetTypes.BUTTON;
        button.widget.states.addAll("normal", "pressed");
        button.widget.properties.put("checkable", "true");

        SimpleImageVO image = new SimpleImageVO();
        image.imageName = "up";
        image.layerName = "Default";
        image.widgetPart = new WidgetPartVO();
        image.widgetPart.role = "background";
        image.widgetPart.setOverride("pressed", CoreStateOverrides.REGION, "down");
        image.widgetPart.setOverride("pressed", CoreStateOverrides.TINT, "ff0000ff");
        button.addItem(image);

        Json json = HyperJson.newJson();
        String text = json.toJson(button);
        CompositeItemVO read = json.fromJson(CompositeItemVO.class, text);

        assertEquals(button.widget, read.widget);
        SimpleImageVO readImage = read.getElementsArray(SimpleImageVO.class).first();
        assertEquals(image.widgetPart, readImage.widgetPart);
        assertEquals("down", readImage.widgetPart.getOverride("pressed", CoreStateOverrides.REGION));
    }

    /** The scene format is public: values are plain strings, no java class name in the data. */
    @Test
    public void overridesAreWrittenAsPlainStrings() {
        WidgetPartVO part = new WidgetPartVO();
        part.role = "knob";
        part.setOverride("pressed", CoreStateOverrides.REGION, "down");

        String text = HyperJson.newJson().toJson(part);

        assertEquals("{\"role\":\"knob\",\"overrides\":{\"pressed\":{\"values\":{\"region\":\"down\"}}}}", text);
    }

    @Test
    public void itemsWithoutWidgetDataStayClean() {
        Json json = HyperJson.newJson();
        String text = json.toJson(new SimpleImageVO());
        assertFalse(text.contains("widget"));

        SimpleImageVO read = json.fromJson(SimpleImageVO.class, text);
        assertNull(read.widget);
        assertNull(read.widgetPart);
    }

    @Test
    public void copyConstructorIsDeep() {
        WidgetPartVO part = new WidgetPartVO();
        part.setOverride("pressed", CoreStateOverrides.VISIBLE, "false");

        WidgetPartVO copy = new WidgetPartVO(part);
        assertEquals(part, copy);

        copy.setOverride("pressed", CoreStateOverrides.VISIBLE, "true");
        assertNotEquals(part, copy);
    }
}
