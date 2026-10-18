package games.rednblack.editor.renderer.input;

import com.badlogic.gdx.Input;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.additional.OverlayComponent;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Overlays opened over the scene: found before anything else, whatever clips them, telling their
 * listener of a press they missed, and - once modal - keeping everything behind them out of reach.
 */
public class OverlayTest {

    private InputTestScene scene;
    private final List<String> events = new ArrayList<>();

    /** Writes down what it is told, and takes it. */
    private class Recorder implements UIInputListener {
        private final String name;

        Recorder(String name) {
            this.name = name;
        }

        private void record(UIEvent event) {
            events.add(name + ":" + event.type);
            event.handle();
        }

        @Override public void touchDown(UIEvent event) { record(event); }
        @Override public void scrolled(UIEvent event) { record(event); }
        @Override public void keyDown(UIEvent event) { record(event); }
    }

    /** Writes down the overlays told of a press they missed, and what it landed on instead. */
    private class Outside implements OverlayListener {
        private final String name;

        Outside(String name) {
            this.name = name;
        }

        @Override
        public void touchedOutside(int overlay, UIEvent event) {
            events.add(name + ":outside:" + event.relatedEntity);
        }
    }

    @Before
    public void setUp() {
        scene = new InputTestScene();
        events.clear();
    }

    private int recorded(int parent, String name, float x, float y, float width, float height) {
        int entity = scene.target(parent, x, y, width, height);
        scene.inputOf(entity).addListener(new Recorder(name));
        return entity;
    }

    /** A list hanging out of a box that clips: without the overlay the part outside takes nothing. */
    @Test
    public void anOverlayIsFoundOutsideTheClipOfWhatHoldsIt() {
        int pane = scene.entity(scene.root, 100, 300, 200, 100);
        scene.clip(pane);
        int list = scene.target(pane, 0, -150, 200, 150);

        assertEquals(-1, scene.hitAt(150, 200));

        scene.input.openOverlay(list, false, null);

        assertEquals(list, scene.hitAt(150, 200));
    }

    @Test
    public void anOverlayIsFoundBeforeWhatIsDrawnAfterIt() {
        int list = scene.target(scene.root, 100, 100, 200, 200);
        int later = scene.target(scene.root, 150, 150, 200, 200);

        assertEquals(later, scene.hitAt(200, 200));

        scene.input.openOverlay(list, false, null);

        assertEquals(list, scene.hitAt(200, 200));
    }

    @Test
    public void theLastOverlayOpenedIsInFront() {
        int first = scene.target(scene.root, 100, 100, 200, 200);
        int second = scene.target(scene.root, 150, 150, 200, 200);
        scene.input.openOverlay(second, false, null);
        scene.input.openOverlay(first, false, null);

        assertEquals(first, scene.hitAt(200, 200));

        //opened again: it comes back to the front
        scene.input.openOverlay(second, false, null);
        assertEquals(second, scene.hitAt(200, 200));
    }

    @Test
    public void closingAnOverlayPutsItBackInItsPlace() {
        int pane = scene.entity(scene.root, 100, 300, 200, 100);
        scene.clip(pane);
        int list = scene.target(pane, 0, -150, 200, 150);
        scene.input.openOverlay(list, false, null);

        scene.input.closeOverlay(list);

        assertFalse(scene.input.isOverlayOpen(list));
        assertFalse(scene.engine.getMapper(OverlayComponent.class).has(list));
        assertEquals(-1, scene.hitAt(150, 200));
    }

    @Test
    public void anOverlayInAHiddenBranchTakesNothing() {
        int box = scene.entity(scene.root, 100, 100, 200, 200);
        int list = scene.target(box, 0, 0, 200, 200);
        scene.input.openOverlay(list, false, null);
        scene.engine.getMapper(MainItemComponent.class).get(box).visible = false;

        assertEquals(-1, scene.hitAt(150, 150));
    }

    /** scene2d's way: the press that closes a list goes on to what it landed on. */
    @Test
    public void aPressMissingAnOverlayTellsItAndGoesOnToWhatItLandedOn() {
        int button = recorded(scene.root, "button", 400, 100, 100, 50);
        int list = scene.target(scene.root, 100, 100, 200, 200);
        scene.input.openOverlay(list, false, new Outside("list"));

        assertTrue(scene.press(450, 120));

        assertEquals("[list:outside:" + button + ", button:touchDown]", events.toString());
    }

    @Test
    public void aPressOnNothingStillTellsTheOverlay() {
        int list = scene.target(scene.root, 100, 100, 200, 200);
        scene.input.openOverlay(list, false, new Outside("list"));

        assertFalse(scene.press(600, 500));

        assertEquals("[list:outside:-1]", events.toString());
    }

    @Test
    public void aPressInsideTheOverlayIsNotOutside() {
        int list = scene.target(scene.root, 100, 100, 200, 200);
        recorded(list, "row", 0, 0, 200, 50);
        scene.input.openOverlay(list, false, new Outside("list"));

        scene.press(150, 120);

        assertEquals("[row:touchDown]", events.toString());
    }

    /** A list open in a window: a press on the window is outside the list, but not the window. */
    @Test
    public void onlyTheOverlaysThePressMissedAreTold() {
        int window = scene.target(scene.root, 100, 100, 400, 300);
        int list = scene.target(window, 50, 50, 100, 100);
        scene.input.openOverlay(window, false, new Outside("window"));
        scene.input.openOverlay(list, false, new Outside("list"));

        scene.press(450, 350);

        assertEquals("[list:outside:" + window + "]", events.toString());
    }

    @Test
    public void aListenerClosingItsOverlayLeavesTheOthersTold() {
        int first = scene.target(scene.root, 100, 100, 100, 100);
        int second = scene.target(scene.root, 300, 100, 100, 100);
        scene.input.openOverlay(first, false, (overlay, event) -> {
            events.add("first");
            scene.input.closeOverlay(overlay);
        });
        scene.input.openOverlay(second, false, (overlay, event) -> {
            events.add("second");
            scene.input.closeOverlay(overlay);
        });

        scene.press(600, 500);

        assertEquals("[second, first]", events.toString());
        assertFalse(scene.input.isOverlayOpen(first));
        assertFalse(scene.input.isOverlayOpen(second));
    }

    // ------------------------------------------------------------------ modal

    @Test
    public void aModalSwallowsAPressBehindIt() {
        recorded(scene.root, "button", 400, 100, 100, 50);
        int window = scene.target(scene.root, 100, 100, 200, 200);
        scene.input.openOverlay(window, true, null);

        assertTrue(scene.press(450, 120));

        assertEquals("[]", events.toString());
    }

    @Test
    public void whatIsInsideAModalStillTakesThePointer() {
        int window = scene.target(scene.root, 100, 100, 200, 200);
        int button = recorded(window, "button", 10, 10, 100, 50);
        scene.input.openOverlay(window, true, null);

        assertEquals(button, scene.hitAt(150, 120));
        scene.press(150, 120);

        assertEquals("[button:touchDown]", events.toString());
    }

    @Test
    public void aModalKeepsTheHoverAndTheWheelFromWhatIsBehind() {
        int behind = recorded(scene.root, "behind", 400, 100, 100, 50);
        int window = scene.target(scene.root, 100, 100, 200, 200);
        scene.input.openOverlay(window, true, null);

        assertTrue(scene.move(450, 120));
        assertEquals(-1, scene.input.getHoveredEntity());
        assertTrue(scene.input.scrolled(0, 1));
        assertEquals("[]", events.toString());

        scene.input.closeOverlay(window);
        scene.move(451, 120);
        assertEquals(behind, scene.input.getHoveredEntity());
    }

    @Test
    public void aPressBehindAModalLeavesTheKeyboardWhereItWas() {
        int window = scene.target(scene.root, 100, 100, 200, 200);
        int field = scene.target(window, 10, 10, 100, 30);
        scene.inputOf(field).focusable = true;
        scene.input.openOverlay(window, true, null);
        scene.press(150, 120);
        assertEquals(field, scene.input.getKeyboardFocus());

        scene.press(600, 500);

        assertEquals(field, scene.input.getKeyboardFocus());
    }

    @Test
    public void aFieldBehindAModalHearsNoKeys() {
        int field = recorded(scene.root, "field", 400, 100, 100, 30);
        scene.input.setKeyboardFocus(field);
        int window = scene.target(scene.root, 100, 100, 200, 200);
        scene.input.openOverlay(window, true, null);

        assertFalse(scene.input.keyDown(Input.Keys.A));
        assertEquals("[]", events.toString());

        scene.input.closeOverlay(window);
        assertTrue(scene.input.keyDown(Input.Keys.A));
        assertEquals("[field:keyDown]", events.toString());
    }

    @Test
    public void tabGoesRoundTheModalAndNeverLeavesIt() {
        int outside = scene.target(scene.root, 400, 100, 100, 30);
        scene.inputOf(outside).focusable = true;
        int window = scene.target(scene.root, 100, 100, 200, 200);
        int first = scene.target(window, 10, 10, 100, 30);
        int second = scene.target(window, 10, 50, 100, 30);
        scene.inputOf(first).focusable = true;
        scene.inputOf(second).focusable = true;
        scene.input.setKeyboardFocus(outside);
        scene.input.openOverlay(window, true, null);

        scene.input.keyDown(Input.Keys.TAB);
        assertEquals(first, scene.input.getKeyboardFocus());
        scene.input.keyDown(Input.Keys.TAB);
        assertEquals(second, scene.input.getKeyboardFocus());
        scene.input.keyDown(Input.Keys.TAB);
        assertEquals(first, scene.input.getKeyboardFocus());
    }

    @Test
    public void onlyTheModalInFrontCounts() {
        int window = scene.target(scene.root, 100, 100, 400, 300);
        int dialog = scene.target(scene.root, 150, 150, 100, 100);
        scene.input.openOverlay(window, false, null);
        scene.input.openOverlay(dialog, true, null);

        assertEquals(dialog, scene.input.topModal());
        //the window is behind the modal dialog, so it is out of reach too
        assertEquals(-1, scene.hitAt(450, 350));
    }
}
