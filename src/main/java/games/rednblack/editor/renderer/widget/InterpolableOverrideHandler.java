package games.rednblack.editor.renderer.widget;

/**
 * A property made of numbers, which can therefore travel to its overridden value instead of
 * jumping there. The float based access is what a running transition uses on every frame.
 */
public interface InterpolableOverrideHandler extends StateOverrideHandler {

    /** @return how many floats make up the value, at most {@link StateTween#MAX_CHANNELS} */
    int getChannelCount();

    void captureChannels(int entity, float[] out);

    /** @return false if the value is not valid, {@code out} is then left untouched */
    boolean parseChannels(String value, float[] out);

    void applyChannels(int entity, float[] values);
}
