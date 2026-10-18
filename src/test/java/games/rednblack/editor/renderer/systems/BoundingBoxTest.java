package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A composite at (500, 400) holding an item at (10, 20), 30 by 40. The viewport draws what it holds
 * from its own origin, so the boxes culling compares with the camera have to be measured the same way.
 */
public class BoundingBoxTest {

    private InputTestScene scene;
    private int composite;
    private int item;

    @Before
    public void setUp() {
        scene = new InputTestScene(new BoundingBoxSystem());
        composite = scene.entity(scene.root, 500, 400, 100, 100);
        item = scene.entity(composite, 10, 20, 30, 40);
        scene.engine.edit(composite).create(BoundingBoxComponent.class);
        scene.engine.edit(item).create(BoundingBoxComponent.class);
        scene.frame();
    }

    @Test
    public void boxesAreInSceneCoordinatesUnderTheRoot() {
        assertBox(item, 510, 420, 30, 40);
        assertBox(composite, 500, 400, 100, 100);
    }

    @Test
    public void theViewedCompositesOwnPlaceIsLeftOut() {
        view(composite);

        // Something else, such as a constraint, keeps the composite where it was.
        assertBox(item, 10, 20, 30, 40);
    }

    @Test
    public void movingTheViewedCompositeDoesNotMoveWhatItHolds() {
        view(composite);
        TransformComponent transform = scene.engine.getMapper(TransformComponent.class).get(composite);
        transform.x = 700;
        transform.y = 50;
        scene.frame();

        assertBox(item, 10, 20, 30, 40);
    }

    @Test
    public void boxesGoBackToTheSceneWhenTheViewportLeaves() {
        view(composite);
        view(scene.root);

        assertBox(item, 510, 420, 30, 40);
    }

    private void view(int entity) {
        int current = scene.engine.getMapper(ViewPortComponent.class).has(scene.root) ? scene.root : composite;
        if (current == entity) return;
        ViewPortComponent from = scene.engine.getMapper(ViewPortComponent.class).get(current);
        ViewPortComponent to = scene.engine.edit(entity).create(ViewPortComponent.class);
        to.viewPort = from.viewPort;
        scene.engine.edit(current).remove(ViewPortComponent.class);
        scene.frame();
        scene.frame();
    }

    private void assertBox(int entity, float x, float y, float width, float height) {
        Rectangle box = scene.engine.getMapper(BoundingBoxComponent.class).get(entity).rectangle;
        assertEquals("x", x, box.x, 0.001f);
        assertEquals("y", y, box.y, 0.001f);
        assertEquals("width", width, box.width, 0.001f);
        assertEquals("height", height, box.height, 0.001f);
    }
}
