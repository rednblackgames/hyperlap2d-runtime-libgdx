package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.widget.TextFilter;
import games.rednblack.editor.renderer.widget.TextValidator;
import games.rednblack.editor.renderer.widget.WidgetBehaviour;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * What a text field holds and where it is being edited: the text itself, the caret, the selection
 * and the window of characters that fits in the field.
 *
 * Nothing here is drawn. The {@link games.rednblack.editor.renderer.systems.TextFieldSystem} puts
 * the visible characters into the label part, and places the caret and the selection parts over
 * them. Long text is not clipped: only the characters that fit are given to the label, so the field
 * shows a window that follows the caret.
 */
@Transient
public class TextFieldComponent extends PooledComponent implements WidgetBehaviour {

    /** Everything the field holds, however much of it is on screen. */
    public final StringBuilder text = new StringBuilder();

    /** Where the caret sits, counted in gaps between characters: 0 is before the first one. */
    public int cursor = 0;
    /** The other end of the selection, the caret being this end. Meaningless without a selection. */
    public int selectionStart = 0;
    public boolean hasSelection = false;

    public boolean isTouchEnabled = true;
    public boolean isHovered = false;
    public boolean isFocused = false;
    /** Pointer selecting with a drag, -1 if none. */
    public int touchPointer = -1;

    /** Key being held down, -1 for none, and how long until it says itself again. */
    public int repeatKey = -1;
    public float repeatIn = 0;

    /** When the last press landed and how many came in a row: two close together select a word. */
    public float lastTapAt = -1;
    public int tapCount = 0;

    /** How long the caret has been in its current half of the blink, and whether it is showing. */
    public float blinkTime = 0;
    public boolean cursorOn = true;

    /**
     * Where every gap between characters falls, from the left of the text: one more entry than
     * there are characters, so the caret at the end has a place of its own. Measured by the system
     * from the label's font, and empty until it has one.
     */
    public final FloatArray glyphPositions = new FloatArray();
    /** The characters being shown, the window the caret keeps inside of. */
    public int visibleStart = 0, visibleEnd = 0;

    /**
     * Where the lines of a text area begin and end, two entries per line, worked out from the
     * width it was given. A single line field leaves it empty.
     */
    public final IntArray lineBreaks = new IntArray();
    /** The first line on screen, the window the caret keeps inside of going down the text. */
    public int visibleLine = 0;
    /** What the last layout made of the lines, which is what a press has to read to find one. */
    public float lineHeight = 0;
    /** Where the top of the first line sits inside the text part, its own bottom being zero. */
    public float textTop = 0;

    /**
     * Where the caret would like to be across the line, so walking up and down a ragged text keeps
     * to one column instead of drifting left. Negative until a sideways move sets it.
     */
    public float wantedColumn = -1;

    /** Last seen value of the text setting, so a change of setting is followed but typing is not undone. */
    public String textSetting = null;
    public boolean started = false;

    /** Last text the listeners were told about, and whether they have been told at all. */
    public String notifiedText = null;
    public boolean notified = false;

    /**
     * A rule of the game's own about what may be typed, instead of the one the editor named. It has
     * the last word: a rule that wants to build on a built-in one asks it and adds to its answer.
     */
    public TextFilter filter = null;

    /** The same for judging the whole text, instead of the validator the editor named. */
    public TextValidator validator = null;

    /** Whether what is written makes sense, which is what the invalid states follow. */
    public boolean valid = true;
    /**
     * What the answer above was given about: the text, the rule that judged it and whether an empty
     * one counted. It is worked out again when any of them is not what it was.
     */
    public String validatedText = null;
    public TextValidator validatedBy = null;
    public boolean validatedEmpty = false;

    public final Array<TextFieldListener> listeners = new Array<>(true, 1, TextFieldListener[]::new);

    public interface TextFieldListener {
        /** The field holds something else, whether typing, a setting or game code put it there. */
        default void textChanged(int entity, String text) {
        }

        /** Enter was pressed in the field. */
        default void entered(int entity, String text) {
        }

        /** What is written has become valid, or stopped being so. */
        default void validChanged(int entity, boolean valid) {
        }
    }

    /** A field starts with the text it was given in the editor. */
    @Override
    public void initialise(WidgetComponent widget) {
        textSetting = widget.properties.get(WidgetTypes.PROPERTY_TEXT);
        text.setLength(0);
        if (textSetting != null) text.append(textSetting);
        cursor = text.length();
    }

    public String getText() {
        return text.toString();
    }

    /**
     * Whether what is written is still the given text, without building a string to find out. The
     * systems ask this every frame, and {@link StringBuilder#toString()} copies the characters.
     *
     * Deliberately a loop of our own rather than {@link String#contentEquals(CharSequence)}: that
     * one only avoids the copy where the class library gives a builder its own path, which differs
     * between desktop, Android and the MobiVM fork, while this reads the same everywhere.
     */
    public boolean textEquals(String other) {
        if (other == null) return false;
        if (other.length() != text.length()) return false;

        for (int i = 0, n = text.length(); i < n; i++) {
            if (other.charAt(i) != text.charAt(i)) return false;
        }
        return true;
    }

    /** Replaces everything, putting the caret at the end and dropping any selection. */
    public void setText(String value) {
        text.setLength(0);
        if (value != null) text.append(value);
        cursor = text.length();
        clearSelection();
    }

    public boolean isEmpty() {
        return text.length() == 0;
    }

    public void selectAll() {
        selectionStart = 0;
        cursor = text.length();
        hasSelection = text.length() > 0;
    }

    public void clearSelection() {
        hasSelection = false;
        selectionStart = cursor;
    }

    /** First character of the selection, the caret and its other end being either way round. */
    public int selectionFrom() {
        return Math.min(cursor, selectionStart);
    }

    public int selectionTo() {
        return Math.max(cursor, selectionStart);
    }

    /** How many lines a text area was broken into, 0 for a field that holds one. */
    public int lineCount() {
        return lineBreaks.size / 2;
    }

    public int lineStart(int line) {
        return lineBreaks.get(line * 2);
    }

    public int lineEnd(int line) {
        return lineBreaks.get(line * 2 + 1);
    }

    /** The line a place in the text falls on, the last one for anything past the end. */
    public int lineOf(int index) {
        for (int line = 0, lines = lineCount(); line < lines; line++) {
            if (index <= lineEnd(line)) return line;
        }
        return Math.max(0, lineCount() - 1);
    }

    public String getSelectedText() {
        return hasSelection ? text.substring(selectionFrom(), selectionTo()) : "";
    }

    public void addListener(TextFieldListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TextFieldListener listener) {
        listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void reset() {
        text.setLength(0);
        cursor = 0;
        selectionStart = 0;
        hasSelection = false;
        isTouchEnabled = true;
        isHovered = false;
        isFocused = false;
        touchPointer = -1;
        repeatKey = -1;
        repeatIn = 0;
        lastTapAt = -1;
        tapCount = 0;
        blinkTime = 0;
        cursorOn = true;
        glyphPositions.clear();
        visibleStart = 0;
        visibleEnd = 0;
        lineBreaks.clear();
        visibleLine = 0;
        wantedColumn = -1;
        lineHeight = 0;
        textTop = 0;
        textSetting = null;
        started = false;
        notifiedText = null;
        notified = false;
        filter = null;
        validator = null;
        valid = true;
        validatedText = null;
        validatedBy = null;
        validatedEmpty = false;
        listeners.clear();
    }
}
