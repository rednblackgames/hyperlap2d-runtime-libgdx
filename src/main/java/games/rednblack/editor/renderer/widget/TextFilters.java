package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.widget.TextFieldComponent;

/**
 * The filters the editor offers. The setting holds the {@link #id}, so what is written in a scene
 * and shown in the list stays a plain word, while the system reading it gets something the compiler
 * knows about.
 *
 * Each constant is a {@link TextFilter} of its own, so game code wanting a rule of its own can ask
 * one of these first and add to its answer.
 */
public enum TextFilters implements TextFilter {

    /** Everything goes in. */
    NONE("none") {
        @Override
        public boolean accepts(int entity, TextFieldComponent field, char character) {
            return true;
        }
    },

    /** Whole numbers, with a minus in front of them. */
    DIGITS("digits") {
        @Override
        public boolean accepts(int entity, TextFieldComponent field, char character) {
            return Character.isDigit(character) || (character == '-' && field.cursor == 0);
        }
    },

    /** Numbers with a point in them, one point at most and never where the minus belongs. */
    DECIMAL("decimal") {
        @Override
        public boolean accepts(int entity, TextFieldComponent field, char character) {
            if (Character.isDigit(character)) return true;
            if (character == '-') return field.cursor == 0;

            return character == '.' && field.text.indexOf(".") == -1 && field.cursor > 0;
        }
    },

    /** Letters and numbers, nothing else: no spaces and no punctuation. */
    ALPHANUMERIC("alphanumeric") {
        @Override
        public boolean accepts(int entity, TextFieldComponent field, char character) {
            return Character.isLetterOrDigit(character);
        }
    };

    /** What the setting holds, and what the editor shows in its list. */
    public final String id;

    TextFilters(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * @param id       what the setting holds, empty or unknown being nothing in particular
     * @param fallback what to make of a setting that says nothing this knows
     */
    /** {@link #values()} copies its array on every call, and this is read every frame. */
    private static final TextFilters[] ALL = values();

    public static TextFilters from(String id, TextFilters fallback) {
        if (id == null || id.isEmpty()) return fallback;

        for (TextFilters filter : ALL) {
            if (filter.id.equals(id)) return filter;
        }
        return fallback;
    }
}
