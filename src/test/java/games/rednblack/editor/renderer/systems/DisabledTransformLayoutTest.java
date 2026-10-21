package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The editor disables the transform of the composite it is inside, so its content is shown in its own space. The
 * layouts placing that composite must leave it there, keeping the place they work out for when it is enabled again.
 */
public class DisabledTransformLayoutTest {

    private InputTestScene scene;
    private int container;

    @Before
    public void setUp() {
        scene = new InputTestScene(new CompositeSystem(), new ParentLocalAABBSystem(), new AutoLayoutMeasureSystem(),
                new LayoutSystem(), new AutoLayoutSystem());
        container = box(scene.root, 100, 100, 300, 200);
    }

    private int box(int parent, float x, float y, float width, float height) {
        int entity = scene.entity(parent, x, y, width, height);
        scene.engine.edit(entity).create(BoundingBoxComponent.class);
        scene.frame();
        return entity;
    }

    private TransformComponent transformOf(int entity) {
        return scene.engine.getMapper(TransformComponent.class).get(entity);
    }

    private void settle() {
        scene.frame();
        scene.frame();
    }

    private void assertAt(int entity, float x, float y) {
        assertEquals("x", x, transformOf(entity).x, 0.001f);
        assertEquals("y", y, transformOf(entity).y, 0.001f);
    }

    private static LayoutComponent.ConstraintData toParent(LayoutComponent.ConstraintSide side, float margin) {
        LayoutComponent.ConstraintData data = new LayoutComponent.ConstraintData();
        data.targetSide = side;
        data.margin = margin;
        data.resolved = true;
        return data;
    }

    @Test
    public void aConstrainedCompositeTheEditorIsInsideStaysInItsOwnSpace() {
        int inside = box(container, 0, 0, 20, 30);
        LayoutComponent constraints = scene.engine.edit(inside).create(LayoutComponent.class);
        constraints.right = toParent(LayoutComponent.ConstraintSide.RIGHT, 0);
        constraints.top = toParent(LayoutComponent.ConstraintSide.TOP, 6);
        settle();
        assertAt(inside, 280, 164);

        transformOf(inside).disableTransform();
        settle();
        assertAt(inside, 0, 0);
        assertEquals(280, transformOf(inside).placedX(), 0.001f);
        assertEquals(164, transformOf(inside).placedY(), 0.001f);

        transformOf(inside).enableTransform();
        settle();
        assertAt(inside, 280, 164);
    }

    @Test
    public void aLaidOutChildTheEditorIsInsideKeepsItsPlaceForLater() {
        scene.engine.edit(container).create(AutoLayoutComponent.class);
        int first = box(container, 0, 0, 20, 30);
        int second = box(container, 0, 0, 40, 10);
        settle();
        assertAt(second, 20, 190);

        transformOf(second).disableTransform();
        settle();
        assertAt(second, 0, 0);
        assertEquals(20, transformOf(second).placedX(), 0.001f);

        transformOf(second).enableTransform();
        settle();
        assertAt(second, 20, 190);
        assertAt(first, 0, 170);
    }
}
