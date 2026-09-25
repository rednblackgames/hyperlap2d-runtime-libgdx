package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.FloatArray;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.widget.TextFieldComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.widget.TextFilter;
import games.rednblack.editor.renderer.widget.TextFilters;
import games.rednblack.editor.renderer.widget.TextValidator;
import games.rednblack.editor.renderer.widget.TextValidators;
import games.rednblack.editor.renderer.widget.WidgetType;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * A line of text the keyboard writes into.
 *
 * Long text is not clipped: the field measures every character, works out the window of them that
 * fits in the label part, and gives the label only that. The window follows the caret, so typing
 * past the right edge scrolls the text the way it does anywhere else, and the label stays the size
 * it was drawn.
 *
 * The caret and the selection are parts like any other, moved and sized over the characters they
 * belong to; without them the field still works, it just says nothing about where it is being
 * edited. The message part is a second label, shown while the field is empty and unfocused.
 *
 * It runs before {@link LabelSystem} so the text it hands over is laid out in the same frame.
 */
@All({TextFieldComponent.class, WidgetComponent.class})
public class TextFieldSystem extends BaseEntitySystem implements UIInputListener {

    /** How long two clicks may be apart and still count as a double one. */
    private static final float TAP_INTERVAL = 0.4f;
    /** How long a key is held before it starts saying itself again, and how often it does. */
    private static final float KEY_REPEAT_DELAY = 0.4f;
    private static final float KEY_REPEAT_INTERVAL = 0.05f;

    protected ComponentMapper<TextFieldComponent> fieldCM;
    protected ComponentMapper<WidgetComponent> widgetCM;
    protected ComponentMapper<WidgetPartComponent> partCM;
    protected ComponentMapper<ViewPortComponent> viewPortCM;
    protected ComponentMapper<InputTargetComponent> inputTargetCM;
    protected ComponentMapper<NodeComponent> nodeCM;
    protected ComponentMapper<TransformComponent> transformCM;
    protected ComponentMapper<DimensionsComponent> dimensionsCM;
    protected ComponentMapper<MainItemComponent> mainItemCM;
    protected ComponentMapper<LabelComponent> labelCM;
    protected ComponentMapper<CompositeTransformComponent> compositeCM;

    /** Wired by the engine: who has the keyboard is what focus means. */
    protected UIInputSystem inputSystem;

    /** Measures the whole text, while the label's own layout only ever holds the visible part. */
    private final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder display = new StringBuilder();
    /** The lines on screen, which is all a text area's label is ever given. */
    private final StringBuilder shown = new StringBuilder();
    /** The characters handed to the label, kept so a window is not copied out anew every frame. */
    private final StringBuilder windowText = new StringBuilder();
    /** Time since the engine started, for telling a double click from two clicks. */
    private float elapsed = 0;

    public TextFieldSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public TextFieldSystem() {
    }

    /** Makes the field an input target the keyboard can land on. */
    @Override
    protected void inserted(int entityId) {
        InputTargetComponent target = inputTargetCM.create(entityId);
        target.addListener(this);
        target.focusable = true;
    }

    @Override
    protected void removed(int entityId) {
        InputTargetComponent target = inputTargetCM.get(entityId);
        if (target != null) target.removeListener(this);
        if (inputSystem != null) inputSystem.cancelTouchFocus(entityId);
    }

    @Override
    protected final void processSystem() {
        elapsed += engine.getDelta();

        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        for (int i = 0, s = actives.size(); i < s; i++) {
            process(ids[i]);
        }
    }

    protected void process(int entity) {
        TextFieldComponent field = fieldCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);

        //the composite being viewed from the inside in the editor is opened, not typed into
        if (viewPortCM.has(entity) || !field.isTouchEnabled) releaseSilently(entity, field);

        keepItsRectangle(entity);
        followTouchable(entity, field);
        followTextSetting(field, widget);
        followFocus(entity, field, widget);
        clampCursor(field);

        repeatKey(entity, field);
        measure(entity, field, widget);
        layout(entity, field, widget);
        blink(field, widget);

        validate(entity, field, widget);
        notifyText(entity, field);
        updateWidgetState(entity, widget, field);
    }

    /**
     * A field is the rectangle it was drawn as. Its label is given a different number of characters
     * every time the caret moves, so a composite growing to its children would breathe with the
     * typing; the switch stays where the author left it, but the widget does not follow it.
     */
    private void keepItsRectangle(int entity) {
        CompositeTransformComponent composite = compositeCM.get(entity);
        if (composite != null) composite.automaticResize = false;
    }

    /**
     * A field nobody may type in is nowhere for the keyboard to land: it is passed over by tab, and
     * a click on it takes the keys away from whatever had them rather than going nowhere.
     */
    private void followTouchable(int entity, TextFieldComponent field) {
        InputTargetComponent target = inputTargetCM.get(entity);
        if (target != null) target.focusable = field.isTouchEnabled;
    }

    /** Follows the text setting when it changes, without undoing what has been typed since. */
    private void followTextSetting(TextFieldComponent field, WidgetComponent widget) {
        String setting = widget.properties.get(WidgetTypes.PROPERTY_TEXT);
        if (!field.started) {
            field.started = true;
            field.textSetting = setting;
            //a field built without going through the factory has still to be given its text
            if (field.isEmpty() && setting != null) field.setText(setting);
            return;
        }

        if (setting == null ? field.textSetting == null : setting.equals(field.textSetting)) return;

        field.textSetting = setting;
        field.setText(setting);
    }

    /** The field is focused while it holds the keyboard, whoever gave it to it. */
    private void followFocus(int entity, TextFieldComponent field, WidgetComponent widget) {
        boolean focused = inputSystem != null && inputSystem.getKeyboardFocus() == entity;
        if (focused == field.isFocused) return;

        field.isFocused = focused;
        field.blinkTime = 0;
        field.cursorOn = true;
        if (focused) {
            if (flag(widget, WidgetTypes.PROPERTY_SELECT_ALL_ON_FOCUS, false)) field.selectAll();
        } else {
            field.clearSelection();
        }
    }

    private void clampCursor(TextFieldComponent field) {
        field.cursor = MathUtils.clamp(field.cursor, 0, field.text.length());
        field.selectionStart = MathUtils.clamp(field.selectionStart, 0, field.text.length());
        if (field.selectionStart == field.cursor) field.hasSelection = false;
    }

    // ------------------------------------------------------------------ measuring

    /**
     * Where every gap between characters falls, measured with the label's own font so the caret
     * lands exactly between the glyphs the label draws.
     */
    private void measure(int entity, TextFieldComponent field, WidgetComponent widget) {
        field.glyphPositions.clear();
        field.glyphPositions.add(0);

        displayTextOf(field, widget);

        BitmapFont font = fontOf(entity);
        if (font == null || display.length() == 0) return;

        LabelComponent label = labelOf(findPart(entity, WidgetTypes.ROLE_TEXT));
        float oldScaleX = font.getScaleX(), oldScaleY = font.getScaleY();
        boolean scaled = label != null && (label.fontScaleX != 1 || label.fontScaleY != 1);
        if (scaled) font.getData().setScale(label.fontScaleX, label.fontScaleY);

        layout.setText(font, display);
        FloatArray positions = field.glyphPositions;
        if (layout.runs.size > 0) {
            FloatArray advances = layout.runs.first().xAdvances;
            float x = 0;
            //the first advance is the run's own offset, every other one carries a character along
            for (int i = 1; i < advances.size; i++) {
                x += advances.get(i);
                positions.add(x);
            }
        }

        if (scaled) font.getData().setScale(oldScaleX, oldScaleY);
    }

    /** What is drawn for the text: the text itself, or one character per character for a password. */
    private StringBuilder displayTextOf(TextFieldComponent field, WidgetComponent widget) {
        display.setLength(0);
        if (!flag(widget, WidgetTypes.PROPERTY_PASSWORD_MODE, false)) {
            display.append(field.text);
            return display;
        }

        String mask = widget.properties.get(WidgetTypes.PROPERTY_PASSWORD_CHARACTER);
        char character = mask == null || mask.isEmpty() ? '*' : mask.charAt(0);
        for (int i = 0, n = field.text.length(); i < n; i++) display.append(character);
        return display;
    }

    // -------------------------------------------------------------------- layout

    private void layout(int entity, TextFieldComponent field, WidgetComponent widget) {
        int textPart = findPart(entity, WidgetTypes.ROLE_TEXT);
        if (textPart == -1) return;

        DimensionsComponent area = dimensionsCM.get(textPart);
        TransformComponent areaTransform = transformOf(textPart);
        LabelComponent label = labelOf(textPart);
        if (area == null || areaTransform == null || label == null) return;

        //a label that breaks lines of its own would undo the ones worked out here
        label.wrap = false;

        if (multiline(widget)) {
            layoutLines(entity, field, widget, areaTransform, area, label);
            layoutBackground(entity);
            layoutMessage(entity, field, widget);
            return;
        }

        float width = area.width;
        window(field, width);
        windowText.setLength(0);
        windowText.append(display, field.visibleStart, field.visibleEnd);
        label.setText(windowText);

        float offset = field.glyphPositions.get(field.visibleStart);
        float visibleWidth = field.glyphPositions.get(field.visibleEnd) - offset;
        float left = areaTransform.x + alignmentOffset(label.lineAlign, width, visibleWidth);

        layoutCursor(entity, field, areaTransform, area, left, offset);
        layoutSelection(entity, field, areaTransform, area, left, offset, width);
        layoutBackground(entity);
        layoutMessage(entity, field, widget);
    }

    /** Whether the widget holds several lines of text rather than one. */
    private boolean multiline(WidgetComponent widget) {
        WidgetType type = widget == null ? null : WidgetTypes.get(widget.widgetType);
        return type != null && type.multiline;
    }

    // ---------------------------------------------------------------- several lines

    /**
     * Breaks the text into the lines it is drawn as: at every newline, and, where the widget wraps,
     * wherever a line has run past the width it was given - at the last space if it has one, so
     * words are kept whole.
     */
    void breakLines(TextFieldComponent field, CharSequence text, boolean wrap, float width) {
        field.lineBreaks.clear();

        FloatArray positions = field.glyphPositions;
        int length = text.length();
        boolean wraps = wrap && width > 0;

        int start = 0, lastSpace = -1;
        for (int i = 0; i < length; i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                field.lineBreaks.add(start);
                field.lineBreaks.add(i);
                start = i + 1;
                lastSpace = -1;
                continue;
            }
            if (character == ' ') lastSpace = i;

            if (!wraps || i + 1 >= positions.size) continue;
            if (positions.get(i + 1) - positions.get(start) <= width) continue;

            //past the edge: back to the last space of this line, or here for a word with none
            int end = lastSpace > start ? lastSpace : i;
            if (end <= start) end = start + 1;
            field.lineBreaks.add(start);
            field.lineBreaks.add(end);

            start = end;
            //the spaces a line was broken at belong to it, not to the next one
            while (start < length && text.charAt(start) == ' ') start++;
            i = start - 1;
            lastSpace = -1;
        }

        //whatever is left, and an empty line for an empty text: the caret needs a line to sit on
        field.lineBreaks.add(Math.min(start, length));
        field.lineBreaks.add(length);
    }

    /**
     * Gives the label the lines that fit, following the caret down the text, and puts the caret and
     * the selection bands over them. What is above and below is simply not handed over.
     */
    private void layoutLines(int entity, TextFieldComponent field, WidgetComponent widget,
                             TransformComponent areaTransform, DimensionsComponent area, LabelComponent label) {
        breakLines(field, display, flag(widget, WidgetTypes.PROPERTY_WRAP, true), area.width);

        float lineHeight = lineHeightOf(entity, label);
        int fits = lineHeight <= 0 ? 1 : Math.max(1, (int) (area.height / lineHeight));
        int lines = field.lineCount();

        int caretLine = field.lineOf(field.cursor);
        field.visibleLine = MathUtils.clamp(field.visibleLine, 0, Math.max(0, lines - fits));
        if (caretLine < field.visibleLine) field.visibleLine = caretLine;
        else if (caretLine >= field.visibleLine + fits) field.visibleLine = caretLine - fits + 1;
        int lastLine = Math.min(lines - 1, field.visibleLine + fits - 1);

        shown.setLength(0);
        for (int line = field.visibleLine; line <= lastLine; line++) {
            if (line > field.visibleLine) shown.append('\n');
            shown.append(display, field.lineStart(line), field.lineEnd(line));
        }
        label.setText(shown);

        int rows = lastLine - field.visibleLine + 1;
        field.lineHeight = lineHeight;
        field.textTop = verticalOffset(label.labelAlign, area.height, rows * lineHeight);

        layoutCursorOnLine(entity, field, areaTransform, area, label, caretLine);
        layoutBands(entity, field, areaTransform, area, label, lastLine);
    }

    /** Where the top of the first line sits inside the text part, following how it was aligned. */
    private static float verticalOffset(int align, float height, float textHeight) {
        if ((align & Align.top) != 0) return height;
        if ((align & Align.bottom) != 0) return textHeight;
        return (height + textHeight) / 2;
    }

    /** Where a place in the text falls across its line, from the left of that line. */
    private float columnOf(TextFieldComponent field, int line, int index) {
        FloatArray positions = field.glyphPositions;
        int start = MathUtils.clamp(field.lineStart(line), 0, positions.size - 1);
        return caretAt(field, index) - positions.get(start);
    }

    /** Where a line starts inside the text part, following how it was aligned across. */
    private float lineLeft(TextFieldComponent field, DimensionsComponent area, LabelComponent label, int line) {
        float lineWidth = columnOf(field, line, field.lineEnd(line));
        return alignmentOffset(label.lineAlign, area.width, lineWidth);
    }

    /** The y of the bottom of a line, the widget's own coordinates. */
    private float lineBottom(TextFieldComponent field, TransformComponent areaTransform, int line) {
        int row = line - field.visibleLine;
        return areaTransform.y + field.textTop - (row + 1) * field.lineHeight;
    }

    private void layoutCursorOnLine(int entity, TextFieldComponent field, TransformComponent areaTransform,
                                    DimensionsComponent area, LabelComponent label, int caretLine) {
        int cursor = findPart(entity, WidgetTypes.ROLE_CURSOR);
        if (cursor == -1) return;

        TransformComponent transform = transformOf(cursor);
        DimensionsComponent dimensions = dimensionsCM.get(cursor);
        if (transform == null || dimensions == null) return;

        boolean shownNow = field.isFocused && field.cursorOn && onScreen(field, caretLine);
        show(cursor, shownNow);
        if (!shownNow) return;

        float x = areaTransform.x + lineLeft(field, area, label, caretLine)
                + columnOf(field, caretLine, field.cursor);
        resize(dimensions, transform, Float.NaN, field.lineHeight);
        place(transform, x, lineBottom(field, areaTransform, caretLine));
    }

    /**
     * The selection as three bands: the rest of the first line, the whole lines between, and the
     * beginning of the last one. A selection on one line only ever needs the first.
     */
    private void layoutBands(int entity, TextFieldComponent field, TransformComponent areaTransform,
                             DimensionsComponent area, LabelComponent label, int lastLine) {
        int first = findPart(entity, WidgetTypes.ROLE_SELECTION);
        int middle = findPart(entity, WidgetTypes.ROLE_SELECTION_MIDDLE);
        int end = findPart(entity, WidgetTypes.ROLE_SELECTION_END);

        if (!field.isFocused || !field.hasSelection) {
            show(first, false);
            show(middle, false);
            show(end, false);
            return;
        }

        int fromLine = field.lineOf(field.selectionFrom());
        int toLine = field.lineOf(field.selectionTo());

        //the first line: from where the selection began to the end of that line, or to where it ends
        float fromX = lineLeft(field, area, label, fromLine) + columnOf(field, fromLine, field.selectionFrom());
        float firstEnd = fromLine == toLine
                ? lineLeft(field, area, label, toLine) + columnOf(field, toLine, field.selectionTo())
                : lineLeft(field, area, label, fromLine) + columnOf(field, fromLine, field.lineEnd(fromLine));
        band(first, field, areaTransform, fromLine, fromX, firstEnd - fromX, 1, lastLine);

        if (fromLine == toLine) {
            show(middle, false);
            show(end, false);
            return;
        }

        //everything in between, whole lines of it
        int betweenFrom = fromLine + 1;
        int rows = toLine - betweenFrom;
        if (rows > 0) {
            band(middle, field, areaTransform, betweenFrom, 0, area.width, rows, lastLine);
        } else {
            show(middle, false);
        }

        float toX = lineLeft(field, area, label, toLine) + columnOf(field, toLine, field.selectionTo());
        float lineStart = lineLeft(field, area, label, toLine);
        band(end, field, areaTransform, toLine, lineStart, toX - lineStart, 1, lastLine);
    }

    /** One band of the selection, hidden when the lines it covers are not on screen. */
    private void band(int part, TextFieldComponent field, TransformComponent areaTransform,
                      int line, float x, float width, int rows, int lastLine) {
        if (part == -1) return;

        TransformComponent transform = transformOf(part);
        DimensionsComponent dimensions = dimensionsCM.get(part);
        if (transform == null || dimensions == null) return;

        //clip to the lines being drawn rather than reaching outside the widget
        int from = Math.max(line, field.visibleLine);
        int to = Math.min(line + rows - 1, lastLine);
        if (width <= 0 || to < from) {
            show(part, false);
            return;
        }

        show(part, true);
        resize(dimensions, transform, width, (to - from + 1) * field.lineHeight);
        place(transform, areaTransform.x + x, lineBottom(field, areaTransform, to));
    }

    private boolean onScreen(TextFieldComponent field, int line) {
        return line >= field.visibleLine;
    }

    private float lineHeightOf(int entity, LabelComponent label) {
        BitmapFont font = fontOf(entity);
        if (font == null) return 0;

        return font.getData().lineHeight * (label == null ? 1 : label.fontScaleY);
    }

    /**
     * Moves the window of characters so the caret is inside it, and so the field is as full as the
     * text allows: a window is only ever shorter than the text when there is more text to its right.
     */
    void window(TextFieldComponent field, float width) {
        FloatArray positions = field.glyphPositions;
        int last = positions.size - 1;
        field.visibleStart = MathUtils.clamp(field.visibleStart, 0, last);

        float total = positions.get(last);
        if (total <= width) {
            field.visibleStart = 0;
            field.visibleEnd = last;
            return;
        }

        int cursor = MathUtils.clamp(field.cursor, 0, last);
        if (cursor < field.visibleStart) field.visibleStart = cursor;

        //the caret must fit in the window, and so must the character it is in front of
        while (positions.get(cursor) - positions.get(field.visibleStart) > width) field.visibleStart++;

        //never leave a gap on the right while there is text on the left to pull in
        while (field.visibleStart > 0 && total - positions.get(field.visibleStart - 1) <= width) {
            field.visibleStart--;
        }

        float start = positions.get(field.visibleStart);
        int end = field.visibleStart;
        while (end < last && positions.get(end + 1) - start <= width) end++;
        field.visibleEnd = Math.max(end, field.visibleStart);
    }

    /** Where the drawn text starts inside the label, which lays it out the way it was aligned. */
    private static float alignmentOffset(int align, float width, float textWidth) {
        if ((align & Align.left) != 0) return 0;
        if ((align & Align.right) != 0) return width - textWidth;
        return (width - textWidth) / 2;
    }

    /**
     * The caret between two characters, as tall as the text area and as wide as it was drawn: a
     * field is a line, so how high the caret reaches is the field's business, not the author's.
     */
    private void layoutCursor(int entity, TextFieldComponent field, TransformComponent area,
                              DimensionsComponent areaSize, float left, float offset) {
        int cursor = findPart(entity, WidgetTypes.ROLE_CURSOR);
        if (cursor == -1) return;

        TransformComponent transform = transformOf(cursor);
        DimensionsComponent dimensions = dimensionsCM.get(cursor);
        if (transform == null || dimensions == null) return;

        boolean shown = field.isFocused && field.cursorOn;
        show(cursor, shown);
        if (!shown) return;

        float x = left + caretAt(field, field.cursor) - offset;
        resize(dimensions, transform, Float.NaN, areaSize.height);
        place(transform, x, area.y);
    }

    /** The band under the selected characters, from one caret place to the other. */
    private void layoutSelection(int entity, TextFieldComponent field, TransformComponent area,
                                 DimensionsComponent areaSize, float left, float offset, float width) {
        int selection = findPart(entity, WidgetTypes.ROLE_SELECTION);
        if (selection == -1) return;

        TransformComponent transform = transformOf(selection);
        DimensionsComponent dimensions = dimensionsCM.get(selection);
        if (transform == null || dimensions == null) return;

        boolean shown = field.isFocused && field.hasSelection;
        show(selection, shown);
        if (!shown) return;

        //only the part of the selection inside the window is drawn, the rest is not on screen
        float from = MathUtils.clamp(caretAt(field, field.selectionFrom()) - offset, 0, width);
        float to = MathUtils.clamp(caretAt(field, field.selectionTo()) - offset, 0, width);

        resize(dimensions, transform, Math.max(0, to - from), areaSize.height);
        place(transform, left + from, area.y);
    }

    /** The backdrop of the field: its whole rectangle, behind everything else. */
    private void layoutBackground(int entity) {
        int background = findPart(entity, WidgetTypes.ROLE_BACKGROUND);
        if (background == -1) return;

        TransformComponent transform = transformOf(background);
        DimensionsComponent dimensions = dimensionsCM.get(background);
        DimensionsComponent field = dimensionsCM.get(entity);
        if (transform == null || dimensions == null || field == null) return;

        resize(dimensions, transform, field.width, field.height);
        place(transform, 0, 0);
    }

    /** The placeholder: shown while there is nothing to read and nobody is typing. */
    private void layoutMessage(int entity, TextFieldComponent field, WidgetComponent widget) {
        int message = findPart(entity, WidgetTypes.ROLE_MESSAGE);
        if (message == -1) return;

        show(message, field.isEmpty() && !field.isFocused);

        //the setting says what it reads; left empty, the label keeps the words it was given
        String hint = widget == null ? null : widget.properties.get(WidgetTypes.PROPERTY_MESSAGE_TEXT);
        LabelComponent label = labelOf(message);
        if (label != null && hint != null && !hint.isEmpty()) label.setText(hint);
    }

    private void blink(TextFieldComponent field, WidgetComponent widget) {
        float period = number(widget, WidgetTypes.PROPERTY_CURSOR_BLINK_TIME, 0.45f);
        if (!field.isFocused || period <= 0) {
            field.cursorOn = true;
            field.blinkTime = 0;
            return;
        }

        field.blinkTime += engine.getDelta();
        while (field.blinkTime >= period) {
            field.blinkTime -= period;
            field.cursorOn = !field.cursorOn;
        }
    }

    /** Picks the most relevant state the widget declares: disabled, focused, hover, default. */
    protected void updateWidgetState(int entity, WidgetComponent widget, TextFieldComponent field) {
        boolean invalid = !field.valid;

        // What is written wrongly prefers the invalid variant of a state, where the widget declares
        // one; a widget that declares none simply goes on showing the plain states.
        if (!field.isTouchEnabled) {
            if (invalid && widget.setState(WidgetTypes.STATE_INVALID_DISABLED)) return;
            if (widget.setState(WidgetTypes.STATE_DISABLED)) return;
        }
        if (field.isFocused) {
            if (invalid && widget.setState(WidgetTypes.STATE_INVALID_FOCUSED)) return;
            if (widget.setState(WidgetTypes.STATE_FOCUSED)) return;
        }
        if (invalid) {
            if (field.isHovered && widget.setState(WidgetTypes.STATE_INVALID_HOVER)) return;
            if (widget.setState(WidgetTypes.STATE_INVALID)) return;
        }
        if (field.isHovered && widget.setState(WidgetTypes.STATE_HOVER)) return;
        widget.currentState = null; //back to the default state
    }

    /**
     * Judges what is written, whenever it has changed: a rule the game set if there is one, else the
     * one the editor named. A field nobody has written in yet is right until it is told otherwise,
     * so an untouched form does not open covered in mistakes.
     */
    private void validate(int entity, TextFieldComponent field, WidgetComponent widget) {
        TextValidator rule = field.validator != null ? field.validator
                : TextValidators.from(widget == null ? null : widget.properties.get(WidgetTypes.PROPERTY_VALIDATOR),
                TextValidators.NONE);
        boolean judgeEmpty = flag(widget, WidgetTypes.PROPERTY_VALIDATE_EMPTY, false);

        //the rule and the setting may change under a text that has not; nothing is written out
        //until one of the three really did, since this is asked of every field every frame
        if (rule == field.validatedBy && judgeEmpty == field.validatedEmpty && field.textEquals(field.validatedText)) {
            return;
        }

        String text = field.getText();
        field.validatedText = text;
        field.validatedBy = rule;
        field.validatedEmpty = judgeEmpty;
        boolean valid = text.isEmpty() && !judgeEmpty || rule.isValid(entity, field, text);

        if (valid == field.valid) return;

        field.valid = valid;
        for (int i = 0; i < field.listeners.size; i++) field.listeners.get(i).validChanged(entity, valid);
    }

    /** Tells the listeners once per change, whoever made it: typing, the settings or game code. */
    private void notifyText(int entity, TextFieldComponent field) {
        //asked every frame, so what is written is compared where it lies instead of being copied out
        if (field.notified && field.textEquals(field.notifiedText)) return;

        String text = field.getText();
        if (!field.notified) {
            field.notified = true;
            field.notifiedText = text;
            return;
        }

        field.notifiedText = text;
        for (int i = 0; i < field.listeners.size; i++) {
            field.listeners.get(i).textChanged(entity, text);
        }
    }

    // ------------------------------------------------------------------ the pointer

    @Override
    public void touchDown(UIEvent event) {
        int entity = event.listenerEntity;
        TextFieldComponent field = fieldCM.get(entity);
        if (field == null || !field.isTouchEnabled) return;
        if (viewPortCM.has(entity)) return;
        if (field.touchPointer != -1) return;

        field.touchPointer = event.pointer;
        field.repeatKey = -1;
        field.blinkTime = 0;
        field.cursorOn = true;

        if (nativeInput(entity, field)) {
            event.handle();
            return;
        }

        int taps = countTap(field);
        int at = indexAt(entity, field, event.localX, event.localY);
        if (taps >= 3) {
            field.selectAll();
        } else if (taps == 2) {
            selectWordAt(field, at);
        } else if (shift()) {
            //a shifted click keeps where the selection began and drags the caret to here
            if (!field.hasSelection) field.selectionStart = field.cursor;
            field.cursor = at;
            field.hasSelection = field.cursor != field.selectionStart;
        } else {
            field.cursor = at;
            field.clearSelection();
        }

        event.handle();
    }

    @Override
    public void touchDragged(UIEvent event) {
        int entity = event.listenerEntity;
        TextFieldComponent field = fieldCM.get(entity);
        if (field == null || field.touchPointer != event.pointer) return;

        field.cursor = indexAt(entity, field, event.localX, event.localY);
        field.hasSelection = field.cursor != field.selectionStart;
    }

    @Override
    public void touchUp(UIEvent event) {
        TextFieldComponent field = fieldCM.get(event.listenerEntity);
        if (field == null || field.touchPointer != event.pointer) return;

        field.touchPointer = -1;
    }

    @Override
    public void enter(UIEvent event) {
        TextFieldComponent field = fieldCM.get(event.listenerEntity);
        if (field != null) field.isHovered = true;
    }

    @Override
    public void exit(UIEvent event) {
        TextFieldComponent field = fieldCM.get(event.listenerEntity);
        if (field != null) field.isHovered = false;
    }

    /** Two clicks close together in the same field, however far apart they are on the line. */
    private int countTap(TextFieldComponent field) {
        if (elapsed - field.lastTapAt > TAP_INTERVAL) field.tapCount = 0;
        field.lastTapAt = elapsed;
        return ++field.tapCount;
    }

    private void selectWordAt(TextFieldComponent field, int at) {
        int from = at, to = at;
        while (from > 0 && isWordCharacter(field.text.charAt(from - 1))) from--;
        while (to < field.text.length() && isWordCharacter(field.text.charAt(to))) to++;

        field.selectionStart = from;
        field.cursor = to;
        field.hasSelection = to > from;
    }

    private static boolean isWordCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    /** The gap between characters nearest to that place in the widget. */
    private int indexAt(int entity, TextFieldComponent field, float localX, float localY) {
        int textPart = findPart(entity, WidgetTypes.ROLE_TEXT);
        if (textPart == -1) return field.cursor;

        TransformComponent area = transformOf(textPart);
        DimensionsComponent size = dimensionsCM.get(textPart);
        LabelComponent label = labelCM.get(textPart);
        if (area == null || size == null || label == null) return field.cursor;

        if (multiline(widgetCM.get(entity))) return indexAtLine(field, area, size, label, localX, localY);

        FloatArray positions = field.glyphPositions;
        float offset = positions.get(MathUtils.clamp(field.visibleStart, 0, positions.size - 1));
        float visibleWidth = positions.get(MathUtils.clamp(field.visibleEnd, 0, positions.size - 1)) - offset;
        float left = area.x + alignmentOffset(label.lineAlign, size.width, visibleWidth);

        float x = localX - left + offset;
        //every character, not only the ones on screen: a drag past the edge carries the window along
        int nearest = 0;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < positions.size; i++) {
            float distance = Math.abs(positions.get(i) - x);
            if (distance >= best) break;
            best = distance;
            nearest = i;
        }
        return nearest;
    }

    /** The same, down a text area: the line under the pointer, then the place across it. */
    private int indexAtLine(TextFieldComponent field, TransformComponent area, DimensionsComponent size,
                            LabelComponent label, float localX, float localY) {
        if (field.lineHeight <= 0 || field.lineCount() == 0) return field.cursor;

        //which row of the ones on screen the pointer is over, counted down from the top
        int row = (int) Math.floor((field.textTop - (localY - area.y)) / field.lineHeight);
        int line = MathUtils.clamp(field.visibleLine + row, 0, field.lineCount() - 1);

        return indexOnLine(field, line, localX - area.x - lineLeft(field, size, label, line));
    }

    // ---------------------------------------------------------------- the keyboard

    @Override
    public void keyDown(UIEvent event) {
        int entity = event.listenerEntity;
        TextFieldComponent field = fieldCM.get(entity);
        if (field == null || !field.isTouchEnabled || !field.isFocused) return;

        if (!apply(entity, field, event.keyCode)) return;

        //a key held down keeps saying itself: the platform repeats characters, not these
        if (repeats(event.keyCode)) {
            field.repeatKey = event.keyCode;
            field.repeatIn = KEY_REPEAT_DELAY;
        }
        event.handle();
    }

    @Override
    public void keyUp(UIEvent event) {
        TextFieldComponent field = fieldCM.get(event.listenerEntity);
        if (field == null) return;

        if (field.repeatKey == event.keyCode) field.repeatKey = -1;
    }

    /** Whether holding it down should go on doing what it did. */
    private static boolean repeats(int keyCode) {
        return keyCode == Input.Keys.LEFT || keyCode == Input.Keys.RIGHT
                || keyCode == Input.Keys.UP || keyCode == Input.Keys.DOWN
                || keyCode == Input.Keys.BACKSPACE || keyCode == Input.Keys.FORWARD_DEL;
    }

    /** Says the held key again, as long as it is held and the field is still the one being typed in. */
    private void repeatKey(int entity, TextFieldComponent field) {
        if (field.repeatKey == -1) return;
        if (!field.isFocused || !field.isTouchEnabled) {
            field.repeatKey = -1;
            return;
        }

        field.repeatIn -= engine.getDelta();
        //a frame that took a long while says the key a few times, not a hundred
        for (int i = 0; i < 10 && field.repeatIn <= 0; i++) {
            field.repeatIn += KEY_REPEAT_INTERVAL;
            apply(entity, field, field.repeatKey);
        }
        if (field.repeatIn <= 0) field.repeatIn = KEY_REPEAT_INTERVAL;
    }

    /** @return true when the key meant something here, false when it may still mean something above */
    private boolean apply(int entity, TextFieldComponent field, int keyCode) {
        boolean shift = shift();
        boolean control = control();
        boolean multiline = multiline(widgetCM.get(entity));
        field.blinkTime = 0;
        field.cursorOn = true;
        //only walking up and down keeps a column in mind
        field.wantedColumn = -1;

        switch (keyCode) {
            case Input.Keys.LEFT:
                moveCursor(field, control ? wordLeft(field) : field.cursor - 1, shift);
                break;
            case Input.Keys.RIGHT:
                moveCursor(field, control ? wordRight(field) : field.cursor + 1, shift);
                break;
            //one line has nothing above or below it, so the caret runs to the end it was sent to,
            //which is what walking out of an area's first or last line does as well
            case Input.Keys.UP:
                if (multiline) moveLine(field, -1, shift);
                else moveCursor(field, 0, shift);
                break;
            case Input.Keys.DOWN:
                if (multiline) moveLine(field, 1, shift);
                else moveCursor(field, field.text.length(), shift);
                break;
            case Input.Keys.HOME:
                moveCursor(field, multiline ? field.lineStart(field.lineOf(field.cursor)) : 0, shift);
                break;
            case Input.Keys.END:
                moveCursor(field, multiline ? field.lineEnd(field.lineOf(field.cursor))
                        : field.text.length(), shift);
                break;
            case Input.Keys.BACKSPACE:
                if (!deleteSelection(field) && field.cursor > 0) {
                    field.text.deleteCharAt(field.cursor - 1);
                    field.cursor--;
                }
                break;
            case Input.Keys.FORWARD_DEL:
                if (!deleteSelection(field) && field.cursor < field.text.length()) {
                    field.text.deleteCharAt(field.cursor);
                }
                break;
            case Input.Keys.ENTER:
            case Input.Keys.NUMPAD_ENTER: {
                //in an area the key makes a line; in a field it says the writing is done
                if (multiline) {
                    insert(entity, field, "\n");
                    break;
                }
                String text = field.getText();
                for (int i = 0; i < field.listeners.size; i++) field.listeners.get(i).entered(entity, text);
                break;
            }
            case Input.Keys.A:
                if (control) field.selectAll();
                break;
            case Input.Keys.C:
                if (control) copy(field);
                break;
            case Input.Keys.X:
                if (control) cut(field);
                break;
            case Input.Keys.V:
                if (control) paste(entity, field);
                break;
            default:
                return false; //everything else may still mean something further up
        }

        return true;
    }

    @Override
    public void keyTyped(UIEvent event) {
        int entity = event.listenerEntity;
        TextFieldComponent field = fieldCM.get(entity);
        if (field == null || !field.isTouchEnabled || !field.isFocused) return;

        char character = event.character;
        //editing keys are handled as keys: here only what leaves a mark
        if (character < 32 || character == 127) return;
        if (control()) return;

        insert(entity, field, String.valueOf(character));
        event.handle();
    }

    private void moveCursor(TextFieldComponent field, int to, boolean keepSelection) {
        if (keepSelection && !field.hasSelection) field.selectionStart = field.cursor;

        field.cursor = MathUtils.clamp(to, 0, field.text.length());
        if (keepSelection) field.hasSelection = field.cursor != field.selectionStart;
        else field.clearSelection();
    }

    /**
     * Up or down a line, keeping to the column the caret set off from: walking through a ragged
     * text and back returns to where it began instead of drifting to the left.
     */
    private void moveLine(TextFieldComponent field, int delta, boolean keepSelection) {
        int line = field.lineOf(field.cursor);
        int target = MathUtils.clamp(line + delta, 0, Math.max(0, field.lineCount() - 1));

        if (target == line) {
            //nothing above, or nothing below: go to the very beginning or the very end
            moveCursor(field, delta < 0 ? 0 : field.text.length(), keepSelection);
            return;
        }

        float wanted = field.wantedColumn >= 0 ? field.wantedColumn : columnOf(field, line, field.cursor);
        moveCursor(field, indexOnLine(field, target, wanted), keepSelection);
        field.wantedColumn = wanted;
    }

    /** The place on a line nearest to a distance across it. */
    private int indexOnLine(TextFieldComponent field, int line, float x) {
        int start = field.lineStart(line), end = field.lineEnd(line);
        int nearest = start;
        float best = Float.MAX_VALUE;
        for (int i = start; i <= end; i++) {
            float distance = Math.abs(columnOf(field, line, i) - x);
            if (distance >= best) break;
            best = distance;
            nearest = i;
        }
        return nearest;
    }

    private int wordLeft(TextFieldComponent field) {
        int at = field.cursor;
        while (at > 0 && !isWordCharacter(field.text.charAt(at - 1))) at--;
        while (at > 0 && isWordCharacter(field.text.charAt(at - 1))) at--;
        return at;
    }

    private int wordRight(TextFieldComponent field) {
        int at = field.cursor, length = field.text.length();
        while (at < length && isWordCharacter(field.text.charAt(at))) at++;
        while (at < length && !isWordCharacter(field.text.charAt(at))) at++;
        return at;
    }

    /** @return true when there was a selection to take out */
    private boolean deleteSelection(TextFieldComponent field) {
        if (!field.hasSelection) return false;

        int from = field.selectionFrom(), to = field.selectionTo();
        field.text.delete(from, to);
        field.cursor = from;
        field.clearSelection();
        return true;
    }

    /** Puts text in, over the selection if there is one, as far as the filter and the length allow. */
    private void insert(int entity, TextFieldComponent field, String text) {
        WidgetComponent widget = widgetCM.get(entity);
        deleteSelection(field);

        int maxLength = (int) number(widget, WidgetTypes.PROPERTY_MAX_LENGTH, 0);
        String filter = widget == null ? null : widget.properties.get(WidgetTypes.PROPERTY_FILTER);

        boolean multiline = multiline(widget);
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\r' || character == '\t') continue;
            //a line of its own is not something a filter has an opinion about
            boolean newLine = character == '\n';
            if (newLine && !multiline) continue;
            if (!newLine && !accepts(entity, field, filter, character)) continue;
            if (maxLength > 0 && field.text.length() >= maxLength) break;

            field.text.insert(field.cursor, character);
            field.cursor++;
        }
    }

    /**
     * Whether the field takes that character: a rule the game set if there is one, else the one the
     * editor named.
     */
    private boolean accepts(int entity, TextFieldComponent field, String filter, char character) {
        TextFilter rule = field.filter != null ? field.filter : TextFilters.from(filter, TextFilters.NONE);
        return rule.accepts(entity, field, character);
    }

    private void copy(TextFieldComponent field) {
        Clipboard clipboard = clipboard();
        if (clipboard != null && field.hasSelection) clipboard.setContents(field.getSelectedText());
    }

    private boolean cut(TextFieldComponent field) {
        if (!field.hasSelection) return false;

        copy(field);
        deleteSelection(field);
        return true;
    }

    private void paste(int entity, TextFieldComponent field) {
        Clipboard clipboard = clipboard();
        if (clipboard == null) return;

        String contents = clipboard.getContents();
        if (contents != null && !contents.isEmpty()) insert(entity, field, contents);
    }

    /**
     * On a phone the keyboard is the system's own, so a press opens it and takes the whole text
     * back when it is done. Everything else here is for a hardware keyboard.
     *
     * @return true when the platform is taking the typing over
     */
    private boolean nativeInput(final int entity, final TextFieldComponent field) {
        if (Gdx.app == null || Gdx.input == null) return false;

        Application.ApplicationType type = Gdx.app.getType();
        if (type != Application.ApplicationType.Android && type != Application.ApplicationType.iOS) return false;

        WidgetComponent widget = widgetCM.get(entity);
        String message = widget == null ? "" : widget.properties.get(WidgetTypes.PROPERTY_MESSAGE_TEXT);
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override
            public void input(String text) {
                field.setText(text);
            }

            @Override
            public void canceled() {
            }
        }, "", field.getText(), message == null ? "" : message);
        return true;
    }

    // ------------------------------------------------------------------- plumbing

    /** Drops the pointer and the focus the field may be holding, without a word to the listeners. */
    private void releaseSilently(int entity, TextFieldComponent field) {
        //the keys go too: a field that cannot be typed in must not sit there holding them
        if (inputSystem != null) inputSystem.cancelTouchFocus(entity);
        field.touchPointer = -1;
        field.repeatKey = -1;
        field.isHovered = false;
        field.clearSelection();
    }

    private float caretAt(TextFieldComponent field, int index) {
        FloatArray positions = field.glyphPositions;
        return positions.get(MathUtils.clamp(index, 0, positions.size - 1));
    }

    private BitmapFont fontOf(int entity) {
        LabelComponent label = labelOf(findPart(entity, WidgetTypes.ROLE_TEXT));
        return label == null || label.cache == null ? null : label.cache.getFont();
    }

    /** The label of a part, or null where the widget has no such part. */
    private LabelComponent labelOf(int part) {
        return part == -1 ? null : labelCM.get(part);
    }

    protected int findPart(int widget, String role) {
        if (widget == -1) return -1;

        NodeComponent node = nodeCM.get(widget);
        if (node == null) return -1;

        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            WidgetPartComponent part = partCM.get(child);
            if (part != null && role.equals(part.role)) return child;
        }
        return -1;
    }

    private void show(int part, boolean visible) {
        if (part == -1) return;

        MainItemComponent mainItem = mainItemCM.get(part);
        if (mainItem != null) mainItem.visible = visible;
    }

    private void resize(DimensionsComponent dimensions, TransformComponent transform, float width, float height) {
        if (!Float.isNaN(width)) dimensions.width = width / (transform.scaleX == 0 ? 1 : transform.scaleX);
        if (!Float.isNaN(height)) dimensions.height = height / (transform.scaleY == 0 ? 1 : transform.scaleY);
        if (dimensions.boundBox != null) {
            dimensions.boundBox.width = dimensions.width;
            dimensions.boundBox.height = dimensions.height;
        }
    }

    /** Puts a part's drawn rectangle exactly there, compensating for its own scale. */
    private void place(TransformComponent transform, float x, float y) {
        if (transform == null) return;

        transform.x = x - transform.originX * (1 - transform.scaleX);
        transform.y = y - transform.originY * (1 - transform.scaleY);
    }

    private TransformComponent transformOf(int part) {
        if (part == -1) return null;

        TransformComponent transform = transformCM.get(part);
        return transform == null ? null : transform.getRealComponent();
    }

    private static boolean flag(WidgetComponent widget, String key, boolean fallback) {
        if (widget == null) return fallback;

        String value = widget.properties.get(key);
        return value == null || value.isEmpty() ? fallback : Boolean.parseBoolean(value);
    }

    private static float number(WidgetComponent widget, String key, float fallback) {
        return widget == null ? fallback : widget.number(key, fallback);
    }

    /** Whether shift is held: asked of the platform, and of a test that wants to pretend. */
    protected boolean shift() {
        return Gdx.input != null
                && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));
    }

    /** Whether the copy and paste modifier is held. */
    protected boolean control() {
        if (Gdx.input == null) return false;

        boolean mac = Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.iOS;
        if (mac) return Gdx.input.isKeyPressed(Input.Keys.SYM);

        return Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
    }

    private static Clipboard clipboard() {
        return Gdx.app == null ? null : Gdx.app.getClipboard();
    }
}
