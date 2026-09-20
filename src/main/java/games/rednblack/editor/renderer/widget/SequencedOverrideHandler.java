package games.rednblack.editor.renderer.widget;

/**
 * A property whose value is something that plays, and which can therefore be reached through other
 * things playing first: the animation a state ends on can be preceded by the one the state before
 * it goes out on, and by one of its own to get in.
 */
public interface SequencedOverrideHandler extends StateOverrideHandler {

    /**
     * Plays, in this order, whatever of the three is given: the exit of the state being left, the
     * entry of the state being applied, and then {@code value}, which keeps playing.
     *
     * @param enter played once before the value, null or empty for none
     * @param exit  played once before everything else, null or empty for none
     */
    void applySequence(int entity, String value, String enter, String exit);
}
