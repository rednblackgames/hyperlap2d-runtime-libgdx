package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.widget.TextFieldComponent;

/**
 * The validators the editor offers, the {@link #id} being what a scene holds and what the list
 * shows. Each is a {@link TextValidator} of its own, so a rule of the game's own can ask one of
 * these first and add to its answer.
 *
 * An empty field is valid to all of them: whether nothing written yet counts as a mistake is the
 * widget's own setting, not something each rule should have an opinion about.
 */
public enum TextValidators implements TextValidator {

    /** Anything written is fine. */
    NONE("none") {
        @Override
        public boolean isValid(int entity, TextFieldComponent field, String text) {
            return true;
        }
    },

    /** Something has to be written, spaces not counting as something. */
    NOT_EMPTY("notEmpty") {
        @Override
        public boolean isValid(int entity, TextFieldComponent field, String text) {
            return text.trim().length() > 0;
        }
    },

    /** A whole number, and one that fits in a long. */
    INTEGER("integer") {
        @Override
        public boolean isValid(int entity, TextFieldComponent field, String text) {
            try {
                Long.parseLong(text.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    },

    /** A number, with or without a point in it. */
    DECIMAL("decimal") {
        @Override
        public boolean isValid(int entity, TextFieldComponent field, String text) {
            try {
                Double.parseDouble(text.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    },

    /**
     * Something that looks like an address: a name, an at sign, a domain with a dot in it. Nothing
     * more is checked, since the only way to know an address is real is to write to it.
     */
    EMAIL("email") {
        @Override
        public boolean isValid(int entity, TextFieldComponent field, String text) {
            String value = text.trim();
            int at = value.indexOf('@');
            if (at <= 0 || at != value.lastIndexOf('@')) return false;

            String domain = value.substring(at + 1);
            int dot = domain.indexOf('.');
            return dot > 0 && dot < domain.length() - 1 && value.indexOf(' ') == -1;
        }
    };

    public final String id;

    TextValidators(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    public static TextValidators from(String id, TextValidators fallback) {
        if (id == null || id.isEmpty()) return fallback;

        for (TextValidators validator : values()) {
            if (validator.id.equals(id)) return validator;
        }
        return fallback;
    }
}
