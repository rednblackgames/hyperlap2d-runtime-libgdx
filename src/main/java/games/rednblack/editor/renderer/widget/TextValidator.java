package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.widget.TextFieldComponent;

/**
 * Judges what a text field holds, all of it at once: a validator says whether what is written makes
 * sense, while a {@link TextFilter} decides what may be typed in the first place.
 *
 * A field whose text is not valid shows its invalid states, where it declares any, and says so in
 * {@link TextFieldComponent#valid}. The {@link TextValidators} constants are the ones the editor
 * offers; game code sets its own on {@link TextFieldComponent#validator}.
 */
public interface TextValidator {

    /**
     * @param entity the field being judged
     * @param field  everything else about it, for a rule that needs more than the words
     * @param text   what it holds right now
     */
    boolean isValid(int entity, TextFieldComponent field, String text);
}
