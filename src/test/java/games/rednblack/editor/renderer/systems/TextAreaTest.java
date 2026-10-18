package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Input;
import games.rednblack.editor.renderer.components.widget.TextFieldComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.widget.WidgetType;
import games.rednblack.editor.renderer.widget.WidgetTypes;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The text area: the same field over several lines. Where the lines fall is worked out from
 * measurements, so {@link TextFieldSystem#breakLines} is given some made up by hand; the rest goes
 * through real keys.
 */
public class TextAreaTest {

    @All({TextFieldComponent.class, WidgetComponent.class})
    private static class TestableTextFieldSystem extends TextFieldSystem {
        boolean shift;

        @Override protected boolean shift() { return shift; }
        @Override protected boolean control() { return false; }
    }

    private InputTestScene scene;
    private TestableTextFieldSystem system;
    private TextFieldComponent text;

    @Before
    public void setUp() {
        system = new TestableTextFieldSystem();
        scene = new InputTestScene(system);

        int area = scene.entity(scene.root, 100, 100, 200, 80);
        WidgetComponent widget = scene.engine.edit(area).create(WidgetComponent.class);
        WidgetTypes.get(WidgetTypes.TEXT_AREA).setup(widget);
        text = scene.engine.edit(area).create(TextFieldComponent.class);
        scene.frame();

        scene.press(150, 120);
        scene.release(150, 120);
        scene.frame();
    }

    private void type(String characters) {
        for (int i = 0; i < characters.length(); i++) scene.input.keyTyped(characters.charAt(i));
        scene.frame();
    }

    private void key(int keycode) {
        scene.input.keyDown(keycode);
        scene.input.keyUp(keycode);
        scene.frame();
    }

    /** Ten characters ten wide each: position i sits at i * 10. */
    private void measured(String content) {
        text.glyphPositions.clear();
        for (int i = 0; i <= content.length(); i++) text.glyphPositions.add(i * 10);
    }

    /** The lines the text would be drawn as, since nothing here can measure a real font. */
    private void breakInto(String content) {
        measured(content);
        system.breakLines(text, content, false, 1000);
    }

    @Test
    public void theTypeIsThereAndSaysItHoldsSeveralLines() {
        WidgetType type = WidgetTypes.get(WidgetTypes.TEXT_AREA);

        assertTrue(type.multiline);
        assertTrue(type.getPart(WidgetTypes.ROLE_SELECTION_MIDDLE) != null);
        assertTrue(type.getPart(WidgetTypes.ROLE_SELECTION_END) != null);
        //the bands and the caret are drawn for the author, like the field's
        assertEquals(4, type.defaultParts.size);
    }

    @Test
    public void enterMakesANewLineInsteadOfSayingTheWritingIsDone() {
        type("one");
        key(Input.Keys.ENTER);
        type("two");

        assertEquals("one\ntwo", text.getText());
    }

    @Test
    public void theArrowsWalkTheLines() {
        type("one");
        key(Input.Keys.ENTER);
        type("two");
        //the lines are worked out from a font the tests have not, so they are given here
        breakInto(text.getText());
        assertEquals(2, text.lineCount());

        key(Input.Keys.UP);
        assertEquals(0, text.lineOf(text.cursor));

        key(Input.Keys.DOWN);
        assertEquals(1, text.lineOf(text.cursor));
    }

    /** Nothing above the first line, so up goes to the very beginning; down to the very end. */
    @Test
    public void theArrowsRunOutAtEitherEnd() {
        type("one");
        key(Input.Keys.ENTER);
        type("two");

        key(Input.Keys.UP);
        key(Input.Keys.UP);
        assertEquals(0, text.cursor);

        key(Input.Keys.DOWN);
        key(Input.Keys.DOWN);
        assertEquals(text.getText().length(), text.cursor);
    }

    @Test
    public void shiftAndAnArrowSelectsDownTheLines() {
        type("one");
        key(Input.Keys.ENTER);
        type("two");
        breakInto(text.getText());

        system.shift = true;
        key(Input.Keys.UP);

        assertTrue(text.hasSelection);
        assertTrue(text.getSelectedText().contains("\n"));
    }

    /** A frame that took that long, so anything on a clock moves on. */
    private void tick(float seconds) {
        scene.engine.setDelta(seconds);
        scene.frame();
        scene.engine.setDelta(0);
    }

    /** The platform repeats characters but not these, so the lines are walked on a clock of our own. */
    @Test
    public void holdingAnArrowUpKeepsWalkingTheLines() {
        type("one");
        key(Input.Keys.ENTER);
        type("two");
        key(Input.Keys.ENTER);
        type("three");
        breakInto(text.getText());
        assertEquals(3, text.lineCount());

        scene.input.keyDown(Input.Keys.UP);
        scene.frame();
        assertEquals(1, text.lineOf(text.cursor));

        //held for a moment, it is still one step: a tap must not run away
        tick(0.3f);
        assertEquals(1, text.lineOf(text.cursor));

        tick(0.2f);
        assertEquals(0, text.lineOf(text.cursor));

        scene.input.keyUp(Input.Keys.UP);
    }

    // -------------------------------------------------------------- breaking lines

    private String linesOf(String content) {
        StringBuilder out = new StringBuilder();
        for (int line = 0; line < text.lineCount(); line++) {
            if (line > 0) out.append('|');
            out.append(content, text.lineStart(line), text.lineEnd(line));
        }
        return out.toString();
    }

    @Test
    public void newLinesMakeLines() {
        String content = "one\ntwo\nthree";
        measured(content);

        system.breakLines(text, content, false, 1000);

        assertEquals("one|two|three", linesOf(content));
    }

    @Test
    public void aLineTooLongIsBrokenAtItsLastSpace() {
        String content = "aa bb cc";
        measured(content);

        //room for five characters: "aa bb" fits, "aa bb " does not
        system.breakLines(text, content, true, 55);

        assertEquals("aa bb|cc", linesOf(content));
    }

    /** A word with nowhere to break is broken inside rather than running out of the widget. */
    @Test
    public void aWordLongerThanTheLineIsBrokenInside() {
        String content = "abcdefgh";
        measured(content);

        system.breakLines(text, content, true, 35);

        //three characters a line, since a fourth would run past the edge
        assertEquals("abc|def|gh", linesOf(content));
    }

    @Test
    public void withoutWrappingALongLineStaysOne() {
        String content = "aa bb cc";
        measured(content);

        system.breakLines(text, content, false, 15);

        assertEquals("aa bb cc", linesOf(content));
    }

    /** The caret always has a line to sit on, even where there is nothing written. */
    @Test
    public void anEmptyTextIsStillOneLine() {
        measured("");

        system.breakLines(text, "", true, 100);

        assertEquals(1, text.lineCount());
        assertEquals(0, text.lineStart(0));
        assertEquals(0, text.lineEnd(0));
        assertEquals(0, text.lineOf(0));
    }
}
