package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.graphics.OrthographicCamera;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.widget.TooltipComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.widget.WidgetTypes;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A tooltip of 200 by 80 with its pointer, 20 by 10, drawn under the middle of the box, a gap of 12
 * and a margin of 10; its target is a cell of 100 by 50 at (350, 100), on a screen of 800 by 600.
 */
public class TooltipTest {

    @All({TooltipComponent.class, WidgetComponent.class})
    private static class TestableTooltipSystem extends TooltipSystem {
        boolean live = true;

        @Override
        protected boolean interactive(int entity) {
            return live;
        }
    }

    private InputTestScene scene;
    private TestableTooltipSystem system;
    private int tooltipEntity, pointer, cell;
    private TooltipComponent tooltip;
    private WidgetComponent widget;
    private final List<String> events = new ArrayList<>();

    @Before
    public void setUp() {
        system = new TestableTooltipSystem();
        scene = new InputTestScene(system, new WidgetStateSystem());
        events.clear();
        camera();
        cell = scene.target(scene.root, 350, 100, 100, 50);

        tooltipEntity = scene.entity(scene.root, 0, 0, 200, 80);
        widget = scene.engine.edit(tooltipEntity).create(WidgetComponent.class);
        WidgetTypes.get(WidgetTypes.TOOLTIP).setup(widget);
        widget.properties.put(WidgetTypes.PROPERTY_GAP, "12");
        widget.properties.put(WidgetTypes.PROPERTY_MARGIN, "10");
        pointer = scene.entity(tooltipEntity, 90, -10, 20, 10);
        scene.engine.edit(pointer).create(WidgetPartComponent.class).role = WidgetTypes.ROLE_POINTER;
        tooltip = scene.engine.edit(tooltipEntity).create(TooltipComponent.class);
        tooltip.addListener(new TooltipComponent.TooltipListener() {
            @Override public void opened(int entity, int target) { events.add("opened " + (target == cell ? "cell" : target)); }
            @Override public void closed(int entity) { events.add("closed"); }
            @Override public void hidden(int entity) { events.add("hidden"); }
        });
        scene.frame();
    }

    /** A camera over the whole test screen, set by hand: one that updates itself needs natives. */
    private void camera() {
        OrthographicCamera camera = new OrthographicCamera();
        camera.viewportWidth = InputTestScene.WIDTH;
        camera.viewportHeight = InputTestScene.HEIGHT;
        camera.position.set(InputTestScene.WIDTH / 2f, InputTestScene.HEIGHT / 2f, 0);
        scene.engine.getMapper(ViewPortComponent.class).get(scene.root).viewPort.setCamera(camera);
    }

    private TransformComponent transform(int entity) {
        return scene.engine.getMapper(TransformComponent.class).get(entity);
    }

    private boolean visible(int entity) {
        return scene.engine.getMapper(MainItemComponent.class).get(entity).visible;
    }

    private void open() {
        tooltip.showAt(cell);
        scene.frame();
    }

    private void moveCell(float x, float y) {
        transform(cell).x = x;
        transform(cell).y = y;
    }

    /** What reaches a target: its presses. */
    private List<String> heard(int target) {
        List<String> heard = new ArrayList<>();
        scene.inputOf(target).addListener(new UIInputListener() {
            @Override public void touchDown(UIEvent event) { heard.add("down"); }
        });
        return heard;
    }

    // ------------------------------------------------------------------ opening and placing

    @Test
    public void itStartsClosedAndHidden() {
        assertFalse(tooltip.isOpen());
        assertFalse(visible(tooltipEntity));
    }

    @Test
    public void itOpensAboveItsTargetCentredOnIt() {
        open();

        assertTrue(visible(tooltipEntity));
        assertTrue(scene.input.isOverlayOpen(tooltipEntity));
        assertEquals(300, transform(tooltipEntity).x, 0.001f);
        assertEquals(162, transform(tooltipEntity).y, 0.001f);
        assertEquals("[opened cell]", events.toString());
    }

    @Test
    public void itOpensBelowWhenOnlyBelowHasRoom() {
        moveCell(350, 520);
        open();

        assertEquals(520 - 12 - 80, transform(tooltipEntity).y, 0.001f);
    }

    @Test
    public void thePlacementSettingCanAskForBelow() {
        widget.properties.put(WidgetTypes.PROPERTY_PLACEMENT, WidgetTypes.PLACEMENT_BELOW);
        open();

        assertEquals(100 - 12 - 80, transform(tooltipEntity).y, 0.001f);
    }

    @Test
    public void itIsPushedSidewaysToStayAMarginInsideTheView() {
        moveCell(0, 100);
        open();
        assertEquals(10, transform(tooltipEntity).x, 0.001f);

        moveCell(760, 100);
        scene.frame();
        assertEquals(800 - 10 - 200, transform(tooltipEntity).x, 0.001f);
    }

    @Test
    public void itFollowsItsTargetAsItMoves() {
        open();
        moveCell(400, 150);
        scene.frame();

        assertEquals(350, transform(tooltipEntity).x, 0.001f);
        assertEquals(212, transform(tooltipEntity).y, 0.001f);
    }

    // ------------------------------------------------------------------ the pointer

    @Test
    public void thePointerIsUnderTheTargetsCentre() {
        moveCell(0, 100);
        open();
        //the cell's centre, 50, is 40 into the box: the pointer's middle goes there
        assertEquals(30, transform(pointer).x, 0.001f);
        assertEquals(-10, transform(pointer).y, 0.001f);
        assertEquals(1, transform(pointer).scaleY, 0.001f);
    }

    @Test
    public void thePointerStaysOnTheBox() {
        moveCell(-45, 100);
        open();

        assertEquals(0, transform(pointer).x, 0.001f);
    }

    @Test
    public void belowItsTargetThePointerIsTurnedOverToTheTop() {
        moveCell(350, 520);
        open();

        //turned about its bottom edge: from 90 down to the top of the box
        assertEquals(-1, transform(pointer).scaleY, 0.001f);
        assertEquals(90, transform(pointer).y, 0.001f);

        moveCell(350, 100);
        scene.frame();
        assertEquals(1, transform(pointer).scaleY, 0.001f);
        assertEquals(-10, transform(pointer).y, 0.001f);
    }

    // ------------------------------------------------------------------ closing

    @Test
    public void aPressElsewhereClosesItAndStillReachesWhatItLandedOn() {
        int other = scene.target(scene.root, 100, 400, 50, 50);
        List<String> heard = heard(other);
        open();

        scene.press(120, 420);
        scene.frame();

        assertFalse(tooltip.isOpen());
        assertEquals("[down]", heard.toString());
        assertEquals("[opened cell, closed]", events.toString());
    }

    @Test
    public void aPressOnItsTargetLeavesItToTheGameAndASecondShowCloses() {
        open();
        scene.press(400, 120);
        scene.frame();
        assertTrue(tooltip.isOpen());

        tooltip.showAt(cell);
        scene.frame();
        assertFalse(tooltip.isOpen());
    }

    @Test
    public void showingItOnAnotherTargetMovesIt() {
        int other = scene.target(scene.root, 100, 100, 100, 50);
        open();

        tooltip.showAt(other);
        scene.frame();

        assertTrue(tooltip.isOpen());
        assertEquals(50, transform(tooltipEntity).x, 0.001f);
        assertEquals("[opened cell, opened " + other + "]", events.toString());
    }

    @Test
    public void aPressOnItIsTakenAndReachesNothingBehind() {
        int behind = scene.target(scene.root, 300, 162, 200, 80);
        List<String> heard = heard(behind);
        open();

        scene.press(400, 200);
        scene.frame();

        assertTrue(tooltip.isOpen());
        assertEquals("[]", heard.toString());
    }

    @Test
    public void itClosesWhenItsTargetIsHidden() {
        open();
        scene.engine.getMapper(MainItemComponent.class).get(cell).visible = false;
        scene.frame();
        assertFalse(tooltip.isOpen());

        //once its closed look is on, the frame after
        scene.frame();
        assertFalse(scene.input.isOverlayOpen(tooltipEntity));
    }

    @Test
    public void itClosesWhenItsTargetIsDeleted() {
        open();
        scene.engine.delete(cell);
        scene.frame();
        scene.frame();

        assertFalse(tooltip.isOpen());
    }

    @Test
    public void theSettingCanKeepItOpenOnPressesElsewhere() {
        widget.properties.put(WidgetTypes.PROPERTY_CLOSE_ON_TOUCH_OUTSIDE, "false");
        open();

        scene.press(100, 500);
        scene.frame();

        assertTrue(tooltip.isOpen());
    }

    @Test
    public void closedItIsHiddenAndTellsSo() {
        open();
        tooltip.hide();
        scene.frame();
        scene.frame();

        assertFalse(visible(tooltipEntity));
        assertFalse(scene.input.isOverlayOpen(tooltipEntity));
        assertEquals("[opened cell, closed, hidden]", events.toString());
    }

    // ------------------------------------------------------------------ being designed

    @Test
    public void whileBeingDesignedItShowsWhereItWasDrawn() {
        system.live = false;
        transform(tooltipEntity).x = 40;
        transform(tooltipEntity).y = 30;
        tooltip.showAt(cell);
        scene.frame();

        assertTrue(visible(tooltipEntity));
        assertEquals(40, transform(tooltipEntity).x, 0.001f);
        assertEquals(30, transform(tooltipEntity).y, 0.001f);
        assertFalse(scene.input.isOverlayOpen(tooltipEntity));
    }
}
