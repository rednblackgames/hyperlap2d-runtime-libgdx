package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.AutoLayoutItemComponent;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** A composite at (100, 100), 300 by 200, laid out in a row unless said otherwise. */
public class AutoLayoutTest {

    private InputTestScene scene;
    private int container;
    private AutoLayoutComponent layout;

    @Before
    public void setUp() {
        scene = new InputTestScene(new CompositeSystem(), new ParentLocalAABBSystem(), new AutoLayoutMeasureSystem(),
                new LayoutSystem(), new AutoLayoutSystem());
        container = box(scene.root, 100, 100, 300, 200);
        layout = scene.engine.edit(container).create(AutoLayoutComponent.class);
        scene.frame();
    }

    private int box(int parent, float x, float y, float width, float height) {
        int entity = scene.entity(parent, x, y, width, height);
        scene.engine.edit(entity).create(BoundingBoxComponent.class);
        scene.frame();
        return entity;
    }

    private AutoLayoutItemComponent itemOf(int entity) {
        AutoLayoutItemComponent item = scene.engine.getMapper(AutoLayoutItemComponent.class).get(entity);
        if (item == null) item = scene.engine.edit(entity).create(AutoLayoutItemComponent.class);
        return item;
    }

    private TransformComponent transformOf(int entity) {
        return scene.engine.getMapper(TransformComponent.class).get(entity);
    }

    private DimensionsComponent dimensionsOf(int entity) {
        return scene.engine.getMapper(DimensionsComponent.class).get(entity);
    }

    private void settle() {
        scene.frame();
        scene.frame();
    }

    private void assertAt(int entity, float x, float y) {
        assertEquals("x", x, transformOf(entity).x, 0.001f);
        assertEquals("y", y, transformOf(entity).y, 0.001f);
    }

    private void assertSize(int entity, float width, float height) {
        assertEquals("width", width, dimensionsOf(entity).width, 0.001f);
        assertEquals("height", height, dimensionsOf(entity).height, 0.001f);
    }

    @Test
    public void childrenArePlacedInTheCompositesOrder() {
        layout.gap = 10;
        int a = box(container, 77, 77, 20, 30);
        int b = box(container, 5, 5, 40, 10);
        settle();

        assertAt(a, 0, 170);
        assertAt(b, 30, 190);
    }

    @Test
    public void constraintsOfAPlacedChildAreLeftAside() {
        int a = box(container, 0, 0, 20, 30);
        LayoutComponent constraints = scene.engine.edit(a).create(LayoutComponent.class);
        constraints.right = new LayoutComponent.ConstraintData();
        constraints.right.targetSide = LayoutComponent.ConstraintSide.RIGHT;
        constraints.right.resolved = true;
        settle();

        assertAt(a, 0, 170);

        itemOf(a).ignore = true;
        settle();
        assertAt(a, 280, 170);
    }

    @Test
    public void ignoredAndInvisibleChildrenTakeNoRoom() {
        int ignored = box(container, 50, 50, 20, 20);
        itemOf(ignored).ignore = true;
        int hidden = box(container, 60, 60, 20, 20);
        scene.engine.getMapper(MainItemComponent.class).get(hidden).visible = false;
        // put back where they were before the layout saw they were to be left alone
        transformOf(ignored).x = transformOf(ignored).y = 50;
        transformOf(hidden).x = transformOf(hidden).y = 60;
        int shown = box(container, 70, 70, 20, 20);
        settle();

        assertAt(ignored, 50, 50);
        assertAt(hidden, 60, 60);
        assertAt(shown, 0, 180);

        layout.skipInvisible = false;
        settle();
        assertAt(hidden, 0, 180);
        assertAt(shown, 20, 180);
    }

    @Test
    public void growingAndStretchedChildrenAreResized() {
        layout.align = AutoLayoutComponent.Align.STRETCH;
        int fixed = box(container, 0, 0, 100, 10);
        int grows = box(container, 0, 0, 10, 10);
        itemOf(grows).grow = 1;
        settle();

        assertSize(fixed, 100, 200);
        assertSize(grows, 200, 200);
        assertAt(grows, 100, 0);
    }

    @Test
    public void aScaledChildIsPlacedByItsFootprint() {
        int scaled = box(container, 0, 0, 20, 10);
        transformOf(scaled).scaleX = 2;
        transformOf(scaled).originX = 10;
        int next = box(container, 0, 0, 20, 10);
        settle();

        // its footprint runs from -10 to 30 around its x
        assertAt(scaled, 10, 190);
        assertAt(next, 40, 190);
    }

    @Test
    public void aCompositeFittingItsContentTakesItsSize() {
        layout.fitWidth = true;
        layout.fitHeight = true;
        layout.padLeft = 5;
        layout.padTop = 5;
        layout.gap = 2;
        box(container, 0, 0, 20, 30);
        box(container, 0, 0, 40, 10);
        settle();

        assertSize(container, 5 + 20 + 2 + 40, 5 + 30);
    }

    @Test
    public void automaticResizeDoesNotUndoTheLayout() {
        CompositeTransformComponent composite = scene.engine.edit(container).create(CompositeTransformComponent.class);
        composite.automaticResize = true;
        layout.padLeft = 10;
        int a = box(container, 0, 0, 20, 20);
        settle();

        assertAt(a, 10, 180);
        assertSize(container, 300, 200);
    }

    @Test
    public void aNestedCompositeStretchedByItsParentKeepsTheStretchAndLaysOutInsideIt() {
        layout.mode = AutoLayoutComponent.Mode.COLUMN;
        layout.align = AutoLayoutComponent.Align.STRETCH;
        layout.fitHeight = true;

        int row = box(container, 0, 0, 10, 10);
        AutoLayoutComponent rowLayout = scene.engine.edit(row).create(AutoLayoutComponent.class);
        rowLayout.fitWidth = true;
        rowLayout.fitHeight = true;
        rowLayout.justify = AutoLayoutComponent.Justify.END;
        int inner = box(row, 0, 0, 50, 40);
        settle();

        // stretched across the column instead of fitting its 50, and its child goes to its end
        assertSize(row, 300, 40);
        assertSize(container, 300, 40);
        assertAt(inner, 250, 0);

        float x = transformOf(inner).x;
        settle();
        assertEquals(x, transformOf(inner).x, 0.001f);
        assertSize(row, 300, 40);
    }

    @Test
    public void whatIsPinnedInsideAResizedChildFollowsInTheSameFrame() {
        int grows = box(container, 0, 0, 10, 10);
        itemOf(grows).grow = 1;
        int pinned = box(grows, 0, 0, 5, 5);
        LayoutComponent constraints = scene.engine.edit(pinned).create(LayoutComponent.class);
        constraints.right = new LayoutComponent.ConstraintData();
        constraints.right.targetSide = LayoutComponent.ConstraintSide.RIGHT;
        constraints.right.resolved = true;
        settle();

        assertSize(grows, 300, 10);
        assertAt(pinned, 295, 0);

        layout.padRight = 100;
        scene.frame();
        assertSize(grows, 200, 10);
        assertAt(pinned, 195, 0);
    }

    @Test
    public void aChildGetsItsOwnSizeBackWhenNoLongerStretched() {
        layout.align = AutoLayoutComponent.Align.STRETCH;
        int a = box(container, 0, 0, 20, 30);
        settle();
        assertSize(a, 20, 200);

        layout.align = AutoLayoutComponent.Align.CENTER;
        settle();
        assertSize(a, 20, 30);
        assertAt(a, 0, 85);
    }

    @Test
    public void aChildGetsItsOwnLengthBackWhenItStopsGrowing() {
        layout.mode = AutoLayoutComponent.Mode.COLUMN;
        int a = box(container, 0, 0, 20, 30);
        itemOf(a).grow = 1;
        settle();
        assertSize(a, 20, 200);

        itemOf(a).grow = 0;
        settle();
        assertSize(a, 20, 30);
    }

    @Test
    public void gridCellsAreSizedByTheChildrensOwnSizesWhenTheyAreStretched() {
        layout.mode = AutoLayoutComponent.Mode.GRID;
        layout.columns = 2;
        layout.cellAlignX = AutoLayoutComponent.Align.STRETCH;
        layout.cellAlignY = AutoLayoutComponent.Align.STRETCH;
        int a = box(container, 0, 0, 20, 30);
        int b = box(container, 0, 0, 40, 10);
        settle();
        settle();

        assertSize(a, 40, 30);
        assertSize(b, 40, 30);
        assertAt(b, 40, 170);

        layout.cellAlignX = AutoLayoutComponent.Align.CENTER;
        layout.cellAlignY = AutoLayoutComponent.Align.END;
        settle();
        assertSize(a, 20, 30);
        assertSize(b, 40, 10);
        assertAt(a, 10, 170);
        assertAt(b, 40, 170);
    }

    @Test
    public void ownSizesComeBackOnAChangeOfModeLeavingOutOrLosingTheLayout() {
        layout.align = AutoLayoutComponent.Align.STRETCH;
        int a = box(container, 0, 0, 20, 30);
        int b = box(container, 0, 0, 20, 30);
        int c = box(container, 0, 0, 20, 30);
        settle();
        assertSize(a, 20, 200);

        layout.mode = AutoLayoutComponent.Mode.GRID;
        settle();
        assertSize(a, 20, 30);

        layout.mode = AutoLayoutComponent.Mode.ROW;
        settle();
        assertSize(b, 20, 200);
        itemOf(b).ignore = true;
        settle();
        assertSize(b, 20, 30);

        assertSize(c, 20, 200);
        scene.engine.edit(container).remove(AutoLayoutComponent.class);
        settle();
        assertSize(c, 20, 30);
    }

    @Test
    public void theBoundBoxOfAFittingCompositeFollowsItsSize() {
        DimensionsComponent dimensions = dimensionsOf(container);
        dimensions.boundBox = new com.badlogic.gdx.math.Rectangle(0, 0, 300, 200);
        layout.fitWidth = true;
        layout.fitHeight = true;
        box(container, 0, 0, 20, 30);
        settle();

        assertEquals(20, dimensions.boundBox.width, 0.001f);
        assertEquals(30, dimensions.boundBox.height, 0.001f);
    }
}
