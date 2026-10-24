package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import org.junit.Before;
import org.junit.Test;

import static games.rednblack.editor.renderer.systems.LabelFitHeightTest.textHeight;
import static org.junit.Assert.assertEquals;

/**
 * A column as tall as what it holds: a label 50 wide that fits its height, with the font of
 * {@link LabelFitHeightTest}, and a box 10 high under it. A new text moves the box in the same frame.
 */
public class LabelFitInLayoutTest {

    private InputTestScene scene;
    private int column, label, box;

    @Before
    public void setUp() {
        scene = new InputTestScene(new LabelMeasureSystem(), new CompositeSystem(), new ParentLocalAABBSystem(),
                new AutoLayoutMeasureSystem(), new LayoutSystem(), new AutoLayoutSystem(), new LabelSystem());
        column = boxIn(scene.root, 0, 0, 50, 100);
        AutoLayoutComponent layout = scene.engine.edit(column).create(AutoLayoutComponent.class);
        layout.mode = AutoLayoutComponent.Mode.COLUMN;
        layout.fitHeight = true;

        label = boxIn(column, 0, 0, 50, 100);
        LabelComponent component = scene.engine.edit(label).create(LabelComponent.class);
        component.setStyle(new Label.LabelStyle(LabelFitHeightTest.font(), null));
        component.setWrap(true);
        component.fitHeight = true;
        component.setText("Sail");
        box = boxIn(column, 0, 0, 20, 10);
        scene.frame();
        scene.frame();
    }

    private int boxIn(int parent, float x, float y, float width, float height) {
        int entity = scene.entity(parent, x, y, width, height);
        scene.engine.edit(entity).create(BoundingBoxComponent.class);
        return entity;
    }

    @Test
    public void theColumnMakesRoomForANewTextInTheSameFrame() {
        scene.engine.getMapper(LabelComponent.class).get(label).setText("Sail into dark seas");
        scene.frame();

        assertEquals(textHeight(4) + 10, scene.engine.getMapper(DimensionsComponent.class).get(column).height, 0.001f);
        assertEquals(10, scene.engine.getMapper(TransformComponent.class).get(label).y, 0.001f);
        assertEquals(0, scene.engine.getMapper(TransformComponent.class).get(box).y, 0.001f);
    }
}
