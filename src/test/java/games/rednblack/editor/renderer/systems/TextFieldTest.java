package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Input;
import games.rednblack.editor.renderer.components.widget.TextFieldComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.widget.TextFilters;
import games.rednblack.editor.renderer.widget.TextValidators;
import games.rednblack.editor.renderer.widget.WidgetType;
import games.rednblack.editor.renderer.widget.WidgetTypes;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The field without a font: everything that is text, keys and focus. Where the characters land on
 * screen needs a real font, which the tests have not, so the placing is covered through
 * {@link TextFieldSystem#window} with measurements made up by hand.
 */
public class TextFieldTest {

    /** A system that can be told what the keyboard's modifiers are doing. */
    @All({TextFieldComponent.class, WidgetComponent.class})
    private static class TestableTextFieldSystem extends TextFieldSystem {
        boolean shift, control;

        @Override protected boolean shift() { return shift; }
        @Override protected boolean control() { return control; }
    }

    private InputTestScene scene;
    private TestableTextFieldSystem system;
    private int field;
    private TextFieldComponent text;
    private WidgetComponent widget;
    private final List<String> changes = new ArrayList<>();
    private final List<String> entered = new ArrayList<>();

    @Before
    public void setUp() {
        system = new TestableTextFieldSystem();
        scene = new InputTestScene(system);
        changes.clear();
        entered.clear();

        field = scene.entity(scene.root, 100, 100, 200, 40);
        widget = scene.engine.edit(field).create(WidgetComponent.class);
        WidgetTypes.get(WidgetTypes.TEXT_FIELD).setup(widget);
        text = scene.engine.edit(field).create(TextFieldComponent.class);
        text.addListener(new TextFieldComponent.TextFieldListener() {
            @Override public void textChanged(int entity, String value) { changes.add(value); }
            @Override public void entered(int entity, String value) { entered.add(value); }
        });
        scene.frame();
    }

    /** Puts the caret in the field and gives it the keyboard, the way a click would. */
    private void focus() {
        scene.press(150, 120);
        scene.release(150, 120);
        scene.frame();
    }

    private void type(String characters) {
        for (int i = 0; i < characters.length(); i++) scene.input.keyTyped(characters.charAt(i));
        scene.frame();
    }

    /** A frame that took that long, so anything on a clock moves on. */
    private void tick(float seconds) {
        scene.engine.setDelta(seconds);
        scene.frame();
        scene.engine.setDelta(0);
    }

    private void key(int keycode) {
        scene.input.keyDown(keycode);
        scene.frame();
    }

    @Test
    public void aPressGivesItTheKeyboardAndTypingGoesIn() {
        focus();

        assertEquals(field, scene.input.getKeyboardFocus());
        assertTrue(text.isFocused);

        type("hi");

        assertEquals("hi", text.getText());
        assertEquals(2, text.cursor);
        assertEquals(WidgetTypes.STATE_FOCUSED, widget.getState());
    }

    /** Once per change and never twice: what the listeners hear is the text as each frame leaves it. */
    @Test
    public void theListenersHearEachChangeOnce() {
        focus();
        type("a");
        type("b");
        scene.frame();

        assertEquals("[a, ab]", changes.toString());
    }

    @Test
    public void enterTellsTheListenersWhatWasWritten() {
        focus();
        type("go");
        key(Input.Keys.ENTER);

        assertEquals("[go]", entered.toString());
    }

    @Test
    public void charactersGoInWhereTheCaretIs() {
        focus();
        type("ac");
        key(Input.Keys.LEFT);
        type("b");

        assertEquals("abc", text.getText());
    }

    @Test
    public void backspaceAndDeleteTakeCharactersOutOnEitherSide() {
        focus();
        type("abc");
        key(Input.Keys.BACKSPACE);
        assertEquals("ab", text.getText());

        key(Input.Keys.HOME);
        key(Input.Keys.FORWARD_DEL);
        assertEquals("b", text.getText());
        assertEquals(0, text.cursor);
    }

    @Test
    public void homeAndEndGoToEitherEnd() {
        focus();
        type("abc");

        key(Input.Keys.HOME);
        assertEquals(0, text.cursor);

        key(Input.Keys.END);
        assertEquals(3, text.cursor);
    }

    @Test
    public void shiftAndAnArrowSelects() {
        focus();
        type("abc");

        system.shift = true;
        key(Input.Keys.LEFT);
        key(Input.Keys.LEFT);

        assertTrue(text.hasSelection);
        assertEquals("bc", text.getSelectedText());

        //typing over a selection replaces it
        system.shift = false;
        type("Z");
        assertEquals("aZ", text.getText());
    }

    @Test
    public void controlAndASelectsEverythingAndTheNextKeyReplacesIt() {
        focus();
        type("abc");

        system.control = true;
        key(Input.Keys.A);
        system.control = false;

        assertEquals("abc", text.getSelectedText());

        key(Input.Keys.BACKSPACE);
        assertEquals("", text.getText());
    }

    @Test
    public void aFieldStopsAtItsMaximumLength() {
        widget.properties.put(WidgetTypes.PROPERTY_MAX_LENGTH, "3");
        focus();

        type("abcdef");

        assertEquals("abc", text.getText());
    }

    @Test
    public void theDigitsFilterKeepsLettersOut() {
        widget.properties.put(WidgetTypes.PROPERTY_FILTER, TextFilters.DIGITS.id);
        focus();

        type("1a2b3");

        assertEquals("123", text.getText());
    }

    @Test
    public void theDecimalFilterTakesOneSeparatorOnly() {
        widget.properties.put(WidgetTypes.PROPERTY_FILTER, TextFilters.DECIMAL.id);
        focus();

        type("1.5.7");

        assertEquals("1.57", text.getText());
    }

    /** The setting is where the text starts; changing it later replaces what is in the field. */
    @Test
    public void theTextSettingIsFollowedWhenItChanges() {
        widget.properties.put(WidgetTypes.PROPERTY_TEXT, "start");
        text.setText("start");
        text.textSetting = "start";
        text.started = true;
        scene.frame();
        assertEquals("start", text.getText());

        focus();
        type("!");
        assertEquals("start!", text.getText());

        widget.properties.put(WidgetTypes.PROPERTY_TEXT, "other");
        scene.frame();

        assertEquals("other", text.getText());
    }

    @Test
    public void aFieldNobodyIsTypingIntoIsBackToItsDefaultLook() {
        focus();
        assertEquals(WidgetTypes.STATE_FOCUSED, widget.getState());

        scene.input.setKeyboardFocus(-1);
        scene.frame();

        assertEquals(widget.defaultState, widget.getState());
        assertFalse(text.isFocused);
    }

    @Test
    public void aFieldThatIsOffDoesNotTakeAnything() {
        focus();
        text.isTouchEnabled = false;
        scene.frame();

        type("nope");

        assertEquals("", text.getText());
        assertEquals(WidgetTypes.STATE_DISABLED, widget.getState());
    }

    /** The platform repeats characters but not these, so the field keeps the clock itself. */
    @Test
    public void holdingAnArrowKeepsMovingTheCaret() {
        focus();
        type("abcdef");

        scene.input.keyDown(Input.Keys.LEFT);
        scene.frame();
        assertEquals(5, text.cursor);

        //held for a moment, it is still one step: a tap must not run away
        tick(0.3f);
        assertEquals(5, text.cursor);

        tick(0.2f);
        assertTrue(text.cursor < 5);

        int reached = text.cursor;
        scene.input.keyUp(Input.Keys.LEFT);
        tick(1f);

        assertEquals(reached, text.cursor);
    }

    /** One line has no line above or below: the arrows run to its ends instead of doing nothing. */
    @Test
    public void upAndDownRunToTheEndsOfTheLine() {
        focus();
        type("abcdef");

        key(Input.Keys.UP);
        assertEquals(0, text.cursor);

        key(Input.Keys.DOWN);
        assertEquals(6, text.cursor);
    }

    @Test
    public void shiftAndUpSelectsBackToTheBeginning() {
        focus();
        type("abcdef");

        system.shift = true;
        key(Input.Keys.UP);

        assertTrue(text.hasSelection);
        assertEquals("abcdef", text.getSelectedText());
    }

    @Test
    public void holdingBackspaceKeepsErasing() {
        focus();
        type("abcdef");

        scene.input.keyDown(Input.Keys.BACKSPACE);
        scene.frame();
        assertEquals("abcde", text.getText());

        tick(0.5f);

        assertTrue(text.getText().length() < 5);
    }

    /** Losing the keyboard mid-press must not leave a key saying itself forever. */
    @Test
    public void aFieldThatLosesTheKeyboardStopsRepeating() {
        focus();
        type("abcdef");
        scene.input.keyDown(Input.Keys.LEFT);
        scene.frame();

        scene.input.setKeyboardFocus(-1);
        tick(1f);

        assertEquals(5, text.cursor);
        assertEquals(-1, text.repeatKey);
    }

    /** A filter of the game's own has the last word over the one named in the editor. */
    @Test
    public void aFilterOfItsOwnDecidesWhatGoesIn() {
        widget.properties.put(WidgetTypes.PROPERTY_FILTER, TextFilters.DIGITS.id);
        //digits, but never a zero to start with: something no list of names could say
        text.filter = (entity, field, character) ->
                TextFilters.DIGITS.accepts(entity, field, character)
                        && !(character == '0' && field.text.length() == 0);
        focus();

        type("0102a");

        assertEquals("102", text.getText());
    }

    // ------------------------------------------------------------------ validating

    @Test
    public void aFieldNobodyHasWrittenInIsNotWrongYet() {
        widget.properties.put(WidgetTypes.PROPERTY_VALIDATOR, TextValidators.INTEGER.id);
        scene.frame();

        assertTrue(text.valid);
        assertEquals(widget.defaultState, widget.getState());
    }

    @Test
    public void anEmptyFieldIsWrongWhereThatWasAskedFor() {
        widget.properties.put(WidgetTypes.PROPERTY_VALIDATOR, TextValidators.NOT_EMPTY.id);
        widget.properties.put(WidgetTypes.PROPERTY_VALIDATE_EMPTY, "true");
        scene.frame();

        assertFalse(text.valid);
    }

    /** The state follows what is written, and prefers the variant that says what else is going on. */
    @Test
    public void whatIsWrittenWronglyShowsTheInvalidStates() {
        widget.properties.put(WidgetTypes.PROPERTY_VALIDATOR, TextValidators.INTEGER.id);
        focus();

        type("12");
        assertTrue(text.valid);
        assertEquals(WidgetTypes.STATE_FOCUSED, widget.getState());

        type("a");
        assertFalse(text.valid);
        assertEquals(WidgetTypes.STATE_INVALID_FOCUSED, widget.getState());

        //nobody typing in it any more: the plain invalid look
        scene.input.setKeyboardFocus(-1);
        scene.frame();
        assertEquals(WidgetTypes.STATE_INVALID, widget.getState());

        focus();
        key(Input.Keys.BACKSPACE);
        assertTrue(text.valid);
        assertEquals(WidgetTypes.STATE_FOCUSED, widget.getState());
    }

    @Test
    public void theListenersHearWhenItStopsMakingSense() {
        List<String> said = new ArrayList<>();
        text.addListener(new TextFieldComponent.TextFieldListener() {
            @Override public void validChanged(int entity, boolean valid) { said.add(String.valueOf(valid)); }
        });
        widget.properties.put(WidgetTypes.PROPERTY_VALIDATOR, TextValidators.EMAIL.id);
        focus();

        type("someone@");
        type("place.com");

        assertEquals("[false, true]", said.toString());
    }

    /** A rule of the game's own judges the whole text, over whatever the editor named. */
    @Test
    public void aValidatorOfItsOwnJudgesTheText() {
        widget.properties.put(WidgetTypes.PROPERTY_VALIDATOR, TextValidators.NONE.id);
        text.validator = (entity, field, value) -> value.startsWith("h");
        focus();

        type("hi");
        assertTrue(text.valid);

        key(Input.Keys.HOME);
        type("o");
        assertFalse(text.valid);
    }

    /**
     * The list the editor offers is the constants themselves, and what is stored is still the plain
     * word: a scene written before the enum existed reads the same.
     */
    @Test
    public void theFilterIsOfferedFromItsOwnConstants() {
        WidgetType.Property filter = null;
        for (WidgetType.Property property : WidgetTypes.get(WidgetTypes.TEXT_FIELD).properties) {
            if (WidgetTypes.PROPERTY_FILTER.equals(property.key)) filter = property;
        }

        assertEquals(WidgetType.PropertyKind.CHOICE, filter.kind);
        assertEquals(TextFilters.NONE.id, filter.defaultValue);
        assertEquals(TextFilters.values().length, filter.choices.size);
        assertEquals("none, digits, decimal, alphanumeric", filter.choices.toString(", "));
    }

    /**
     * A caret and a selection band are shapes, not art: the type asks the editor for them, drawn
     * from the image every project is given.
     */
    @Test
    public void theTypeAsksForACaretAndASelectionOfItsOwn() {
        WidgetType type = WidgetTypes.get(WidgetTypes.TEXT_FIELD);

        assertEquals(2, type.defaultParts.size);
        assertEquals(WidgetTypes.ROLE_SELECTION, type.defaultParts.get(0).role);
        assertEquals(WidgetTypes.ROLE_CURSOR, type.defaultParts.get(1).role);
        for (WidgetType.DefaultPart part : type.defaultParts) {
            assertEquals(WidgetTypes.REGION_WHITE_PIXEL, part.region);
        }
        //the band goes behind the characters, the caret over them
        assertTrue(type.defaultParts.get(0).zIndex < type.defaultParts.get(1).zIndex);
    }

    // ------------------------------------------------------------------ the window

    /** Ten characters ten wide each, so a window is easy to read: position i sits at i * 10. */
    private void measured(int characters, int cursor) {
        text.glyphPositions.clear();
        for (int i = 0; i <= characters; i++) text.glyphPositions.add(i * 10);
        text.cursor = cursor;
    }

    @Test
    public void textThatFitsIsShownWhole() {
        measured(5, 0);

        system.window(text, 200);

        assertEquals(0, text.visibleStart);
        assertEquals(5, text.visibleEnd);
    }

    @Test
    public void theWindowFollowsTheCaretToTheRight() {
        measured(20, 20);

        system.window(text, 50);

        //the caret is at the end, so the last five characters are the ones on screen
        assertEquals(15, text.visibleStart);
        assertEquals(20, text.visibleEnd);
    }

    @Test
    public void theWindowFollowsTheCaretBackToTheLeft() {
        measured(20, 20);
        system.window(text, 50);

        text.cursor = 2;
        system.window(text, 50);

        assertEquals(2, text.visibleStart);
        assertEquals(7, text.visibleEnd);
    }

    /** A field is never left half empty while there is text to the left that could fill it. */
    @Test
    public void theWindowIsPulledBackWhenTheTextEnds() {
        measured(20, 20);
        system.window(text, 50);
        assertEquals(15, text.visibleStart);

        //the last characters go, and the window slides left rather than leaving a gap
        measured(16, 16);
        text.visibleStart = 15;
        system.window(text, 50);

        assertEquals(11, text.visibleStart);
        assertEquals(16, text.visibleEnd);
    }

    /** What the listeners and the validator are asked every frame, without copying the text out. */
    @Test
    public void theFieldSaysWhetherItStillHoldsTheSameText() {
        focus();
        type("abc");

        assertTrue(text.textEquals("abc"));
        assertFalse(text.textEquals("ab"));
        assertFalse(text.textEquals("abcd"));
        assertFalse(text.textEquals("abd"));
        assertFalse(text.textEquals(null));
        assertTrue(text.getText(), text.textEquals(text.getText()));
    }

    // ------------------------------------------------------------- focus traversal

    /** A second field beside the first, to have somewhere for the keyboard to go. */
    private int anotherField() {
        int other = scene.entity(scene.root, 100, 200, 200, 40);
        WidgetComponent otherWidget = scene.engine.edit(other).create(WidgetComponent.class);
        WidgetTypes.get(WidgetTypes.TEXT_FIELD).setup(otherWidget);
        scene.engine.edit(other).create(TextFieldComponent.class);
        scene.frame();
        return other;
    }

    @Test
    public void tabHandsTheKeyboardToTheNextField() {
        int other = anotherField();
        focus();
        assertTrue(text.isFocused);

        key(Input.Keys.TAB);

        assertEquals(other, scene.input.getKeyboardFocus());
        assertFalse(text.isFocused);
        assertTrue(scene.engine.getMapper(TextFieldComponent.class).get(other).isFocused);
    }

    /** What has been typed stays put: tab only moves the keyboard on. */
    @Test
    public void tabLeavesNoTabBehind() {
        anotherField();
        focus();
        type("ab");

        key(Input.Keys.TAB);

        assertEquals("ab", text.getText());
    }

    @Test
    public void aFieldNobodyCanTypeInIsNotATabStop() {
        int other = anotherField();
        TextFieldComponent otherText = scene.engine.getMapper(TextFieldComponent.class).get(other);
        otherText.isTouchEnabled = false;
        scene.frame();
        focus();

        key(Input.Keys.TAB);

        //nowhere else to go, so the keyboard stays where it is
        assertEquals(field, scene.input.getKeyboardFocus());
        assertTrue(text.isFocused);
    }

    /** A field switched off while it holds the keyboard lets go of it. */
    @Test
    public void aFieldTurnedOffGivesTheKeyboardBack() {
        focus();
        assertTrue(text.isFocused);

        text.isTouchEnabled = false;
        scene.frame();

        assertEquals(-1, scene.input.getKeyboardFocus());
        assertFalse(text.isFocused);
    }
}
