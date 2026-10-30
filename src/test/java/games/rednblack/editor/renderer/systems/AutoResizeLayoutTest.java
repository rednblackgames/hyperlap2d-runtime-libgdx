package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A badge: a composite that resizes to its children, a text 20 by 10 pinned 14 from its left, and
 * a background stretched over the composite 10 past its right edge. The composite takes the text's
 * size and keeps it frame after frame, the background following it, as the text grows and shrinks.
 */
public class AutoResizeLayoutTest {

    private InputTestScene scene;
    private int badge, text, background, loose;

    @Before
    public void setUp() {
        scene = new InputTestScene(new CompositeSystem(), new ParentLocalAABBSystem(), new AutoLayoutMeasureSystem(),
                new LayoutSystem(), new AutoLayoutSystem());
        badge = box(scene.root, 50, 50, 0, 0);
        background = box(badge, 0, 0, 1, 10);
        LayoutComponent stretched = scene.engine.edit(background).create(LayoutComponent.class);
        stretched.left = toParent(LayoutComponent.ConstraintSide.LEFT, 0);
        stretched.right = toParent(LayoutComponent.ConstraintSide.RIGHT, -10);
        stretched.matchConstraintWidth = true;
        text = box(badge, 0, 0, 20, 10);
        scene.engine.edit(text).create(LayoutComponent.class).left = toParent(LayoutComponent.ConstraintSide.LEFT, 14);
        resizing(badge);
        frames(5);

        loose = box(scene.root, 0, 0, 0, 0);
        box(loose, 5, 3, 20, 10);
        resizing(loose);
        frames(2);
    }

    private int box(int parent, float x, float y, float width, float height) {
        int entity = scene.entity(parent, x, y, width, height);
        TransformComponent transform = transform(entity);
        transform.originX = 0;                       // as the loader leaves it: NaN until then
        transform.originY = 0;
        scene.engine.edit(entity).create(BoundingBoxComponent.class);
        scene.frame();
        return entity;
    }

    /** A composite as the loader makes one, resizing to its children, once they are all in. */
    private void resizing(int entity) {
        scene.engine.edit(entity).create(CompositeTransformComponent.class);
        size(entity).boundBox = new Rectangle();
    }

    private void frames(int n) {
        for (int i = 0; i < n; i++) scene.frame();
    }

    private static LayoutComponent.ConstraintData toParent(LayoutComponent.ConstraintSide side, float margin) {
        LayoutComponent.ConstraintData data = new LayoutComponent.ConstraintData();
        data.targetSide = side;
        data.margin = margin;
        data.resolved = true;
        return data;
    }

    private DimensionsComponent size(int entity) {
        return scene.engine.getMapper(DimensionsComponent.class).get(entity);
    }

    private TransformComponent transform(int entity) {
        return scene.engine.getMapper(TransformComponent.class).get(entity);
    }

    @Test
    public void theCompositeTakesItsContentNotTheBackgroundStretchedPastIt() {
        frames(10);
        assertEquals(34, size(badge).width, 0.001f);
        assertEquals(44, size(background).width, 0.001f);
        assertEquals(14, transform(text).x, 0.001f);
        assertEquals(0, transform(background).x, 0.001f);
        assertEquals(10, size(badge).height, 0.001f);
    }

    @Test
    public void theBackgroundFollowsTheTextAsItGrowsAndShrinks() {
        size(text).width = 40;
        frames(3);
        assertEquals(54, size(badge).width, 0.001f);
        assertEquals(64, size(background).width, 0.001f);

        size(text).width = 10;
        frames(3);
        assertEquals(24, size(badge).width, 0.001f);
        assertEquals(34, size(background).width, 0.001f);
    }

    @Test
    public void aCompositeWithoutConstraintsStillClosesOnItsChildren() {
        assertEquals(20, size(loose).width, 0.001f);
        assertEquals(10, size(loose).height, 0.001f);
        int child = scene.engine.getMapper(NodeComponent.class).get(loose).children.first();
        assertEquals(0, transform(child).x, 0.001f);
        assertEquals(0, transform(child).y, 0.001f);
    }
}
