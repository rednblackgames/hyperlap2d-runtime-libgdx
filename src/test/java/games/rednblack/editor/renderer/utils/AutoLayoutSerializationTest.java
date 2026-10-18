package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.utils.Json;
import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.AutoLayoutItemComponent;
import games.rednblack.editor.renderer.data.AutoLayoutItemVO;
import games.rednblack.editor.renderer.data.AutoLayoutVO;
import games.rednblack.editor.renderer.data.CompositeItemVO;
import games.rednblack.editor.renderer.data.SimpleImageVO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class AutoLayoutSerializationTest {

    @Test
    public void autoLayoutSurvivesJsonRoundTrip() {
        CompositeItemVO composite = new CompositeItemVO();
        composite.autoLayout = new AutoLayoutVO();
        composite.autoLayout.mode = AutoLayoutComponent.Mode.GRID;
        composite.autoLayout.columns = 3;
        composite.autoLayout.cellAlignY = AutoLayoutComponent.Align.STRETCH;
        composite.autoLayout.padTop = 4;
        composite.autoLayout.fitHeight = true;

        SimpleImageVO image = new SimpleImageVO();
        image.layerName = "Default";
        image.autoLayoutItem = new AutoLayoutItemVO();
        image.autoLayoutItem.grow = 2;
        image.autoLayoutItem.alignSelf = AutoLayoutComponent.Align.END;
        composite.addItem(image);

        Json json = HyperJson.newJson();
        CompositeItemVO read = json.fromJson(CompositeItemVO.class, json.toJson(composite));

        assertEquals(composite.autoLayout, read.autoLayout);
        assertEquals(image.autoLayoutItem, read.getElementsArray(SimpleImageVO.class).first().autoLayoutItem);
    }

    @Test
    public void itemsWithoutAutoLayoutStayClean() {
        String text = HyperJson.newJson().toJson(new SimpleImageVO());
        assertFalse(text.contains("autoLayout"));
    }

    @Test
    public void settingsGoBetweenComponentAndData() {
        AutoLayoutComponent component = new AutoLayoutComponent();
        component.mode = AutoLayoutComponent.Mode.COLUMN;
        component.justify = AutoLayoutComponent.Justify.SPACE_BETWEEN;
        component.gap = 6;
        component.reverse = true;

        AutoLayoutVO vo = new AutoLayoutVO();
        vo.loadFromComponent(component);
        AutoLayoutComponent copy = new AutoLayoutComponent();
        vo.applyToComponent(copy);
        AutoLayoutVO again = new AutoLayoutVO();
        again.loadFromComponent(copy);

        assertEquals(vo, again);
        assertEquals(vo, new AutoLayoutVO(vo));
    }

    @Test
    public void aMissingAlignmentOfAChildFollowsTheLayout() {
        AutoLayoutItemComponent component = new AutoLayoutItemComponent();
        new AutoLayoutItemVO().applyToComponent(component);
        assertNull(component.alignSelf);
    }
}
