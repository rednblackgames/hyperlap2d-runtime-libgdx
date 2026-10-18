package games.rednblack.editor.renderer.input;

import com.badlogic.gdx.Input;
import games.rednblack.editor.renderer.components.MainItemComponent;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tab walking the keyboard from one focusable to the next.
 *
 * Going forward is pressed as a real key; going back would need a shift the tests have no keyboard
 * to hold down, so it is asked for directly, which is the same road from one step further on.
 */
public class FocusTraversalTest {

    private InputTestScene scene;

    @Before
    public void setUp() {
        scene = new InputTestScene();
    }

    /** A focusable the keyboard can land on. */
    private int stop(int parent, float x) {
        int entity = scene.target(parent, x, 100, 50, 30);
        scene.inputOf(entity).focusable = true;
        return entity;
    }

    private boolean tab() {
        return scene.input.keyDown(Input.Keys.TAB);
    }

    @Test
    public void tabWalksTheFocusablesInTheOrderTheTreeReads() {
        int first = stop(scene.root, 0);
        int second = stop(scene.root, 100);
        int third = stop(scene.root, 200);
        scene.input.setKeyboardFocus(first);

        assertTrue(tab());
        assertEquals(second, scene.input.getKeyboardFocus());

        assertTrue(tab());
        assertEquals(third, scene.input.getKeyboardFocus());
    }

    /** The tree is read downwards: what is inside an entity comes before what follows it. */
    @Test
    public void whatIsInsideComesBeforeWhatComesAfter() {
        int first = stop(scene.root, 0);
        int composite = scene.entity(scene.root, 100, 100, 200, 100);
        int inside = stop(composite, 0);
        int deeper = stop(scene.entity(composite, 0, 0, 100, 100), 0);
        int last = stop(scene.root, 400);
        scene.input.setKeyboardFocus(first);

        tab();
        assertEquals(inside, scene.input.getKeyboardFocus());
        tab();
        assertEquals(deeper, scene.input.getKeyboardFocus());
        tab();
        assertEquals(last, scene.input.getKeyboardFocus());
    }

    @Test
    public void theLastOneLeadsBackToTheFirst() {
        int first = stop(scene.root, 0);
        int second = stop(scene.root, 100);
        scene.input.setKeyboardFocus(second);

        assertTrue(tab());
        assertEquals(first, scene.input.getKeyboardFocus());
    }

    @Test
    public void goingBackWalksTheOtherWayAndWrapsToo() {
        int first = stop(scene.root, 0);
        int second = stop(scene.root, 100);
        scene.input.setKeyboardFocus(second);

        assertTrue(scene.input.moveFocus(-1));
        assertEquals(first, scene.input.getKeyboardFocus());

        assertTrue(scene.input.moveFocus(-1));
        assertEquals(second, scene.input.getKeyboardFocus());
    }

    @Test
    public void tabWithNothingFocusedTakesTheFirstOne() {
        int first = stop(scene.root, 0);
        stop(scene.root, 100);

        assertTrue(tab());
        assertEquals(first, scene.input.getKeyboardFocus());
    }

    /** Only what says it takes the keyboard is a stop: a plain button is clicked, never tabbed to. */
    @Test
    public void whatIsNotFocusableIsNoStop() {
        int first = stop(scene.root, 0);
        scene.target(scene.root, 100, 100, 50, 30);
        int third = stop(scene.root, 200);
        scene.input.setKeyboardFocus(first);

        tab();
        assertEquals(third, scene.input.getKeyboardFocus());
    }

    /** What a click could not reach the keyboard does not reach either. */
    @Test
    public void whatInputCannotReachIsPassedOver() {
        int first = stop(scene.root, 0);
        int disabled = stop(scene.root, 100);
        scene.inputOf(disabled).touchable = Touchable.DISABLED;
        int invisible = stop(scene.root, 200);
        scene.engine.getMapper(MainItemComponent.class).get(invisible).visible = false;
        int last = stop(scene.root, 300);
        scene.input.setKeyboardFocus(first);

        tab();
        assertEquals(last, scene.input.getKeyboardFocus());
    }

    /** A branch nobody can touch takes the focusables inside it with it. */
    @Test
    public void aStopInsideSomethingDisabledIsGoneToo() {
        int first = stop(scene.root, 0);
        int composite = scene.entity(scene.root, 100, 100, 200, 100);
        scene.makeTarget(composite).touchable = Touchable.DISABLED;
        stop(composite, 0);
        int last = stop(scene.root, 400);
        scene.input.setKeyboardFocus(first);

        tab();
        assertEquals(last, scene.input.getKeyboardFocus());
    }

    /** Whoever holds the keys has the first word: a listener taking tab keeps it for itself. */
    @Test
    public void aListenerThatTakesTabKeepsTheFocus() {
        int first = stop(scene.root, 0);
        stop(scene.root, 100);
        scene.inputOf(first).addListener(new UIInputListener() {
            @Override
            public void keyDown(UIEvent event) {
                event.handle();
            }
        });
        scene.input.setKeyboardFocus(first);

        assertTrue(tab());
        assertEquals(first, scene.input.getKeyboardFocus());
    }

    @Test
    public void aGameCanKeepTabForItself() {
        int first = stop(scene.root, 0);
        stop(scene.root, 100);
        scene.input.setKeyboardFocus(first);
        scene.input.setFocusTraversal(false);

        assertFalse(tab());
        assertEquals(first, scene.input.getKeyboardFocus());
    }

    @Test
    public void aSceneWithNowhereToGoLeavesTabAlone() {
        scene.target(scene.root, 100, 100, 50, 30);

        assertFalse(tab());
        assertEquals(-1, scene.input.getKeyboardFocus());
    }
}
