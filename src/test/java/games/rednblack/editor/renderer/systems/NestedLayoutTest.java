package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A panel stretched over a box 300 by 200, a child 20 by 30 pinned to the panel's top, and a
 * sibling 20 by 30 pinned under the panel's top. When the box grows both follow in the same frame,
 * though they were made after the panel.
 */
public class NestedLayoutTest {

    private InputTestScene scene;
    private int outer, panel, child, sibling;

    @Before
    public void setUp() {
        scene = new InputTestScene(new CompositeSystem(), new ParentLocalAABBSystem(), new AutoLayoutMeasureSystem(),
                new LayoutSystem(), new AutoLayoutSystem());
        outer = box(scene.root, 0, 0, 300, 200);
        panel = box(outer, 0, 0, 300, 200);
        LayoutComponent stretched = scene.engine.edit(panel).create(LayoutComponent.class);
        stretched.left = toParent(LayoutComponent.ConstraintSide.LEFT);
        stretched.right = toParent(LayoutComponent.ConstraintSide.RIGHT);
        stretched.top = toParent(LayoutComponent.ConstraintSide.TOP);
        stretched.bottom = toParent(LayoutComponent.ConstraintSide.BOTTOM);
        stretched.matchConstraintWidth = true;
        stretched.matchConstraintHeight = true;
        child = box(panel, 0, 0, 20, 30);
        LayoutComponent pinned = scene.engine.edit(child).create(LayoutComponent.class);
        pinned.right = toParent(LayoutComponent.ConstraintSide.RIGHT);
        pinned.top = toParent(LayoutComponent.ConstraintSide.TOP);
        sibling = box(outer, 0, 0, 20, 30);
        LayoutComponent.ConstraintData underPanel = toParent(LayoutComponent.ConstraintSide.TOP);
        underPanel.targetEntity = panel;
        scene.engine.edit(sibling).create(LayoutComponent.class).top = underPanel;
        scene.frame();
        scene.frame();
    }

    private int box(int parent, float x, float y, float width, float height) {
        int entity = scene.entity(parent, x, y, width, height);
        scene.engine.edit(entity).create(BoundingBoxComponent.class);
        scene.frame();
        return entity;
    }

    private static LayoutComponent.ConstraintData toParent(LayoutComponent.ConstraintSide side) {
        LayoutComponent.ConstraintData data = new LayoutComponent.ConstraintData();
        data.targetSide = side;
        data.resolved = true;
        return data;
    }

    @Test
    public void aChildFollowsTheParentItIsPinnedToInTheSameFrame() {
        DimensionsComponent size = scene.engine.getMapper(DimensionsComponent.class).get(outer);
        size.width = 500;
        size.height = 400;
        scene.frame();

        TransformComponent transform = scene.engine.getMapper(TransformComponent.class).get(child);
        assertEquals(480, transform.x, 0.001f);
        assertEquals(370, transform.y, 0.001f);
    }

    @Test
    public void aSiblingFollowsTheEdgeOfAStretchedPanelInTheSameFrame() {
        scene.engine.getMapper(DimensionsComponent.class).get(outer).height = 400;
        scene.frame();

        assertEquals(370, scene.engine.getMapper(TransformComponent.class).get(sibling).y, 0.001f);
    }
}
