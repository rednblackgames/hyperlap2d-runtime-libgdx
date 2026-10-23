package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.systems.action.ActionSystem;
import games.rednblack.editor.renderer.systems.action.Actions;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * An action on a layout's constraint: the entity stays placed by its layout, only the margin moves,
 * and what is anchored to it follows.
 */
public class LayoutMarginActionTest {

    private static final LayoutComponent.ConstraintSide BOTTOM = LayoutComponent.ConstraintSide.BOTTOM;
    private static final LayoutComponent.ConstraintSide TOP = LayoutComponent.ConstraintSide.TOP;

    private InputTestScene scene;
    private int container, panel;

    @Before
    public void setUp() {
        scene = new InputTestScene(new ActionSystem(), new CompositeSystem(), new ParentLocalAABBSystem(),
                new AutoLayoutMeasureSystem(), new LayoutSystem(), new AutoLayoutSystem());
        container = box(scene.root, 0, 0, 300, 200);
        panel = box(container, 0, 0, 100, 40);
        scene.engine.edit(panel).create(LayoutComponent.class).bottom = toParent(BOTTOM, 10);
        settle();
    }

    private int box(int parent, float x, float y, float width, float height) {
        int entity = scene.entity(parent, x, y, width, height);
        scene.engine.edit(entity).create(BoundingBoxComponent.class);
        scene.frame();
        return entity;
    }

    private static LayoutComponent.ConstraintData toParent(LayoutComponent.ConstraintSide side, float margin) {
        LayoutComponent.ConstraintData data = new LayoutComponent.ConstraintData();
        data.targetSide = side;
        data.margin = margin;
        data.resolved = true;
        return data;
    }

    private void settle() {
        scene.frame();
        scene.frame();
    }

    /** In steps the action system takes whole: it never moves an action on by more than a quarter of a second a frame. */
    private void tick(float seconds) {
        tick(scene, seconds);
    }

    private static void tick(InputTestScene scene, float seconds) {
        for (float left = seconds; left > 0; left -= 0.25f) {
            scene.engine.setDelta(Math.min(0.25f, left));
            scene.frame();
        }
        scene.engine.setDelta(0);
        scene.frame();
        scene.frame();
    }

    private float margin(int entity) {
        return scene.engine.getMapper(LayoutComponent.class).get(entity).bottom.margin;
    }

    private float y(int entity) {
        return scene.engine.getMapper(TransformComponent.class).get(entity).y;
    }

    @Test
    public void theMarginGetsWhereItIsAskedAndTheLayoutPlacesTheEntityThere() {
        Actions.addAction(panel, Actions.marginTo(BOTTOM, 50, 1f), scene.engine);
        tick(1.1f);
        assertEquals(50, margin(panel), 0.001f);
        assertEquals(50, y(panel), 0.001f);
    }

    @Test
    public void halfwayTheMarginIsHalfwayToo() {
        Actions.addAction(panel, Actions.marginTo(BOTTOM, 50, 1f), scene.engine);
        tick(0.5f);
        assertEquals(30, margin(panel), 0.001f);
    }

    @Test
    public void anActionTakingOverHalfwayStartsFromWhereTheMarginIs() {
        Actions.addAction(panel, Actions.marginTo(BOTTOM, 50, 1f), scene.engine);
        tick(0.5f);
        Actions.removeActions(panel, scene.engine);
        Actions.addAction(panel, Actions.marginTo(BOTTOM, 0, 1f), scene.engine);
        tick(0.5f);
        assertEquals(15, margin(panel), 0.001f);
    }

    @Test
    public void whatIsAnchoredToItFollows() {
        int label = box(container, 0, 0, 50, 10);
        LayoutComponent.ConstraintData onTop = toParent(BOTTOM, 5);
        onTop.targetEntity = panel;
        onTop.targetSide = TOP;
        scene.engine.edit(label).create(LayoutComponent.class).bottom = onTop;
        settle();
        assertEquals(10 + 40 + 5, y(label), 0.001f);

        Actions.addAction(panel, Actions.marginTo(BOTTOM, -60, 1f), scene.engine);
        tick(1.1f);
        assertEquals(-60 + 40 + 5, y(label), 0.001f);
    }

    /** One logic serves every engine: run in another first, it must still read this one's layouts. */
    @Test
    public void itWorksInEveryEngineItIsRunIn() {
        InputTestScene other = new InputTestScene(new ActionSystem(), new CompositeSystem(), new ParentLocalAABBSystem(),
                new AutoLayoutMeasureSystem(), new LayoutSystem(), new AutoLayoutSystem());
        int there = other.entity(other.root, 0, 0, 10, 10);
        other.engine.edit(there).create(LayoutComponent.class).bottom = toParent(BOTTOM, 0);
        Actions.addAction(there, Actions.marginTo(BOTTOM, 20, 0.5f), other.engine);
        tick(other, 0.6f);
        assertEquals(20, other.engine.getMapper(LayoutComponent.class).get(there).bottom.margin, 0.001f);

        Actions.addAction(panel, Actions.marginTo(BOTTOM, 50, 0.5f), scene.engine);
        tick(0.6f);
        assertEquals(50, margin(panel), 0.001f);
    }

    @Test
    public void aConstraintTheEntityHasNotIsLeftAlone() {
        Actions.addAction(panel, Actions.marginTo(TOP, 50, 1f), scene.engine);
        tick(1.1f);
        assertEquals(10, margin(panel), 0.001f);
        assertEquals(10, y(panel), 0.001f);
    }
}
