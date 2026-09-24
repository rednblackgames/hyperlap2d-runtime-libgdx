package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.widget.TextFieldComponent;

/**
 * Says what may be typed into a text field, one character at a time: a filter keeps characters out
 * rather than judging what is already there, which is what a
 * {@link TextValidator} does.
 *
 * The {@link TextFilters} constants are the ones the editor offers; game code sets its own on
 * {@link TextFieldComponent#filter} for anything a list of names cannot say, and may build on a
 * built-in one by asking it first.
 */
public interface TextFilter {

    /**
     * @param entity    the field the character is going into
     * @param field     what it holds and where the caret is, before the character is added
     * @param character what is being typed, or pasted
     */
    boolean accepts(int entity, TextFieldComponent field, char character);
}
