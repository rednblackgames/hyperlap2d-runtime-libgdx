package games.rednblack.editor.renderer.input;

import com.badlogic.gdx.Input;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UIInputSystemTest {

    private InputTestScene scene;
    private final List<String> events = new ArrayList<>();

    /** Writes down everything it is told, and takes the event when asked to. */
    private class Recorder implements UIInputListener {
        private final String name;
        private final boolean takes;

        Recorder(String name, boolean takes) {
            this.name = name;
            this.takes = takes;
        }

        private void record(UIEvent event) {
            events.add(name + ":" + event.type + (event.capture ? "(capture)" : ""));
            if (takes) event.handle();
        }

        @Override public void touchDown(UIEvent event) { record(event); }
        @Override public void touchDragged(UIEvent event) { record(event); }
        @Override public void touchUp(UIEvent event) { record(event); }
        @Override public void mouseMoved(UIEvent event) { record(event); }
        @Override public void enter(UIEvent event) { record(event); }
        @Override public void exit(UIEvent event) { record(event); }
        @Override public void scrolled(UIEvent event) { record(event); }
        @Override public void keyDown(UIEvent event) { record(event); }
        @Override public void keyTyped(UIEvent event) { record(event); }
    }

    @Before
    public void setUp() {
        scene = new InputTestScene();
        events.clear();
    }

    /**
     * A host taking its input back lets go of everything: the press it was following ends as
     * cancelled, and nothing is left hovered or holding the keys.
     */
    @Test
    public void clearFocusEndsTheGestureAndLetsGoOfHoverAndKeys() {
        int button = scene.target(scene.root, 100, 100, 100, 50);
        scene.inputOf(button).focusable = true;

        boolean[] cancelled = {false};
        int[] ups = {0};
        scene.inputOf(button).addListener(new UIInputListener() {
            @Override public void touchDown(UIEvent event) { event.handle(); }
            @Override public void touchUp(UIEvent event) { ups[0]++; cancelled[0] = event.cancelled; }
        });

        scene.move(150, 120);
        scene.press(150, 120);
        assertEquals(button, scene.input.getKeyboardFocus());

        scene.input.clearFocus();

        assertEquals(1, ups[0]);
        assertTrue(cancelled[0]);
        assertEquals(-1, scene.input.getKeyboardFocus());
        assertEquals(-1, scene.input.getHoveredEntity());
        assertFalse(scene.input.isAnyPointerDown());

        //the pointer belongs to the host now, so what is left of the gesture finds nobody
        assertFalse(scene.drag(160, 120));
        assertEquals(1, ups[0]);
    }

    private void listen(int entity, String name, boolean takes) {
        scene.makeTarget(entity).addListener(new Recorder(name, takes));
    }

    private void capture(int entity, String name, boolean takes) {
        scene.makeTarget(entity).addCaptureListener(new Recorder(name, takes));
    }

    @Test
    public void thePressGoesToTheEntityUnderThePointer() {
        int left = scene.target(scene.root, 0, 0, 100, 100);
        int right = scene.target(scene.root, 200, 0, 100, 100);
        listen(left, "left", true);
        listen(right, "right", true);

        assertTrue(scene.press(250, 50));
        assertEquals("[right:touchDown]", events.toString());
    }

    @Test
    public void theEntityInFrontTakesThePress() {
        int behind = scene.target(scene.root, 0, 0, 100, 100);
        int inFront = scene.target(scene.root, 50, 0, 100, 100);
        listen(behind, "behind", true);
        listen(inFront, "front", true);

        //x=70 is over both, the one drawn last is the one hit
        scene.press(70, 50);
        assertEquals("[front:touchDown]", events.toString());
    }

    @Test
    public void nothingUnderThePointerMeansNothingIsTaken() {
        int target = scene.target(scene.root, 0, 0, 100, 100);
        listen(target, "target", true);

        assertFalse(scene.press(500, 500));
        assertEquals("[]", events.toString());
    }

    @Test
    public void anEventNobodyTookTravelsUpToTheParents() {
        int composite = scene.target(scene.root, 100, 100, 200, 200);
        int child = scene.target(composite, 10, 10, 50, 50);
        listen(child, "child", false);
        listen(composite, "composite", false);
        listen(scene.root, "root", false);

        assertFalse(scene.press(120, 120));
        assertEquals("[child:touchDown, composite:touchDown, root:touchDown]", events.toString());
    }

    @Test
    public void takingAnEventEndsItsJourney() {
        int composite = scene.target(scene.root, 100, 100, 200, 200);
        int child = scene.target(composite, 10, 10, 50, 50);
        listen(child, "child", true);
        listen(composite, "composite", false);

        assertTrue(scene.press(120, 120));
        assertEquals("[child:touchDown]", events.toString());
    }

    @Test
    public void aCaptureListenerSeesTheEventFirstAndCanSwallowIt() {
        int composite = scene.target(scene.root, 0, 0, 400, 400);
        int child = scene.target(composite, 10, 10, 50, 50);
        capture(composite, "modal", true);
        listen(child, "child", true);

        assertTrue(scene.press(20, 20));
        assertEquals("[modal:touchDown(capture)]", events.toString());
    }

    @Test
    public void whoeverTookThePressKeepsThePointerWhereverItGoes() {
        int target = scene.target(scene.root, 0, 0, 100, 100);
        listen(target, "target", true);

        scene.press(50, 50);
        // the whole gesture belongs to the interface, so a game underneath sees none of it
        assertTrue(scene.drag(500, 500));
        assertTrue(scene.release(500, 500));

        assertEquals("[target:touchDown, target:touchDragged, target:touchUp]", events.toString());
    }

    @Test
    public void aTakenPressKeepsTheDragEvenFromAListenerThatIgnoresIt() {
        int target = scene.target(scene.root, 0, 0, 100, 100);
        scene.inputOf(target).addListener(new UIInputListener() {
            @Override
            public void touchDown(UIEvent event) {
                event.handle();
            }
        });

        scene.press(50, 50);
        assertTrue(scene.drag(60, 50));
        assertTrue(scene.release(60, 50));
    }

    @Test
    public void apressNobodyTookLeavesTheDragToTheGame() {
        int target = scene.target(scene.root, 0, 0, 100, 100);
        listen(target, "target", false);

        scene.press(50, 50);
        assertFalse(scene.drag(60, 50));
        assertFalse(scene.release(60, 50));
        assertEquals("[target:touchDown]", events.toString());
    }

    @Test
    public void aListenerCanFollowThePointerWithoutTakingTheEvent() {
        int pane = scene.target(scene.root, 0, 0, 400, 400);
        int child = scene.target(pane, 10, 10, 50, 50);
        listen(child, "child", true);
        scene.makeTarget(pane).addCaptureListener(new UIInputListener() {
            @Override
            public void touchDown(UIEvent event) {
                events.add("pane:touchDown(follow)");
                event.followPointer();
            }

            @Override
            public void touchDragged(UIEvent event) {
                events.add("pane:touchDragged");
                //the drag is the pane's now: the child must not think it is still pressed
                scene.input.cancelTouchFocus(event.pointer, event.listenerEntity);
            }

            @Override
            public void touchUp(UIEvent event) {
                events.add("pane:touchUp");
            }
        });

        scene.press(20, 20);
        assertEquals("[pane:touchDown(follow), child:touchDown]", events.toString());

        events.clear();
        scene.drag(20, 60);
        assertEquals("[pane:touchDragged, child:touchUp]", events.toString());

        // from here the pointer is the pane's alone
        events.clear();
        scene.drag(20, 90);
        scene.release(20, 90);
        assertEquals("[pane:touchDragged, pane:touchUp]", events.toString());
    }

    @Test
    public void theLocalCoordinatesAreTheEntitysOwn() {
        int composite = scene.target(scene.root, 100, 100, 200, 200);
        int child = scene.target(composite, 10, 10, 50, 50);
        final float[] local = new float[2];
        scene.inputOf(child).addListener(new UIInputListener() {
            @Override
            public void touchDown(UIEvent event) {
                local[0] = event.localX;
                local[1] = event.localY;
                event.handle();
            }
        });

        scene.press(120, 130);
        assertEquals(10, local[0], 0.001f);
        assertEquals(20, local[1], 0.001f);
    }

    @Test
    public void onlyTheEntityInFrontIsHovered() {
        int behind = scene.target(scene.root, 0, 0, 100, 100);
        int inFront = scene.target(scene.root, 50, 0, 100, 100);
        listen(behind, "behind", false);
        listen(inFront, "front", false);

        scene.move(70, 50);
        assertEquals(inFront, scene.input.getHoveredEntity());
        assertEquals("[front:enter, front:mouseMoved]", events.toString());
    }

    @Test
    public void theEntitiesAroundTheHoveredOneAreEnteredToo() {
        int composite = scene.target(scene.root, 0, 0, 400, 400);
        int child = scene.target(composite, 10, 10, 50, 50);
        listen(composite, "composite", false);
        listen(child, "child", false);

        scene.move(20, 20);
        //from the outside in, then out of the innermost first
        assertEquals("[composite:enter, child:enter, child:mouseMoved, composite:mouseMoved]", events.toString());

        events.clear();
        scene.move(300, 300);
        assertEquals("[child:exit, composite:mouseMoved]", events.toString());
    }

    @Test
    public void hoverFollowsAnEntityThatMovesUnderAStillPointer() {
        int target = scene.target(scene.root, 200, 0, 100, 100);
        listen(target, "target", false);

        scene.move(50, 50);
        assertEquals(-1, scene.input.getHoveredEntity());

        scene.engine.getMapper(TransformComponent.class).get(target).x = 0;
        scene.frame();
        assertEquals(target, scene.input.getHoveredEntity());
        assertEquals("[target:enter]", events.toString());
    }

    @Test
    public void aPointerHoldingSomethingHoversNothing() {
        int target = scene.target(scene.root, 0, 0, 100, 100);
        listen(target, "target", true);

        scene.move(50, 50);
        events.clear();

        scene.press(50, 50);
        assertEquals(-1, scene.input.getHoveredEntity());

        scene.release(50, 50);
        assertEquals(target, scene.input.getHoveredEntity());
        assertEquals("[target:exit, target:touchDown, target:touchUp, target:enter]", events.toString());
    }

    @Test
    public void aDisabledBranchTakesNoInputAtAll() {
        int composite = scene.target(scene.root, 0, 0, 400, 400);
        int child = scene.target(composite, 10, 10, 50, 50);
        listen(composite, "composite", true);
        listen(child, "child", true);
        scene.inputOf(composite).touchable = Touchable.DISABLED;

        assertFalse(scene.press(20, 20));
        assertEquals("[]", events.toString());
    }

    @Test
    public void aContainerLetsThePressThroughToItsChildren() {
        int composite = scene.target(scene.root, 0, 0, 400, 400);
        int child = scene.target(composite, 10, 10, 50, 50);
        listen(composite, "composite", true);
        listen(child, "child", true);
        scene.inputOf(composite).touchable = Touchable.CHILDREN_ONLY;

        scene.press(20, 20);
        assertEquals("[child:touchDown]", events.toString());

        //where there is no child the container itself is not hit either
        events.clear();
        assertFalse(scene.press(300, 300));
        assertEquals("[]", events.toString());
    }

    @Test
    public void whatIsClippedAwayIsNotHit() {
        int pane = scene.target(scene.root, 0, 0, 100, 100);
        scene.clip(pane);
        int inside = scene.target(pane, 10, 10, 20, 20);
        int outside = scene.target(pane, 10, 200, 20, 20);
        listen(inside, "inside", true);
        listen(outside, "outside", true);

        assertTrue(scene.press(20, 20));
        assertEquals("[inside:touchDown]", events.toString());

        // the child is drawn nowhere, so the press finds nothing there, not even the pane
        events.clear();
        assertFalse(scene.press(20, 210));
        assertEquals("[]", events.toString());
    }

    @Test
    public void whatIsNotDrawnIsNotHit() {
        int target = scene.target(scene.root, 0, 0, 100, 100);
        listen(target, "target", true);
        scene.engine.getMapper(MainItemComponent.class).get(target).visible = false;

        assertFalse(scene.press(50, 50));
        assertEquals("[]", events.toString());
    }

    @Test
    public void aClickGivesTheKeyboardFocusToWhatTakesIt() {
        int field = scene.target(scene.root, 0, 0, 100, 100);
        int plain = scene.target(scene.root, 200, 0, 100, 100);
        scene.inputOf(field).focusable = true;
        listen(field, "field", true);

        scene.click(50, 50);
        assertEquals(field, scene.input.getKeyboardFocus());

        events.clear();
        assertTrue(scene.input.keyDown(Input.Keys.A));
        assertTrue(scene.input.keyTyped('a'));
        assertEquals("[field:keyDown, field:keyTyped]", events.toString());

        //clicking something else takes the focus away
        scene.click(250, 50);
        assertEquals(-1, scene.input.getKeyboardFocus());
        assertFalse(scene.input.keyDown(Input.Keys.A));
    }

    @Test
    public void keysReachTheEntitiesAroundTheFocusedOne() {
        int composite = scene.target(scene.root, 0, 0, 400, 400);
        int field = scene.target(composite, 10, 10, 50, 50);
        scene.inputOf(field).focusable = true;
        listen(field, "field", false);
        listen(composite, "composite", false);

        scene.click(20, 20);
        events.clear();
        scene.input.keyDown(Input.Keys.TAB);
        assertEquals("[field:keyDown, composite:keyDown]", events.toString());
    }

    @Test
    public void scrollingGoesToWhatIsUnderThePointer() {
        int composite = scene.target(scene.root, 0, 0, 400, 400);
        int child = scene.target(composite, 10, 10, 50, 50);
        listen(child, "child", false);
        listen(composite, "composite", true);

        scene.move(20, 20);
        events.clear();
        assertTrue(scene.input.scrolled(0, 1));
        assertEquals("[child:scrolled, composite:scrolled]", events.toString());
    }

    @Test
    public void aPointerLeavingTheSceneHoversNothing() {
        int target = scene.target(scene.root, 0, 0, 100, 100);
        listen(target, "target", false);

        scene.move(50, 50);
        events.clear();

        scene.input.clearHover();
        assertEquals(-1, scene.input.getHoveredEntity());
        assertEquals("[target:exit]", events.toString());

        //and the frames that follow do not bring it back by themselves
        scene.frame();
        assertEquals("[target:exit]", events.toString());
    }
}
