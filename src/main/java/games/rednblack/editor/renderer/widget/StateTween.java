package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Pool;

/**
 * A property on its way to the value a widget state wants it at. Values travel as floats (one for
 * a number, four for a color), so nothing is allocated while it runs.
 */
public class StateTween implements Pool.Poolable {
    public static final int MAX_CHANNELS = 4;

    public String key;
    public InterpolableOverrideHandler handler;
    /** Value the property is heading to, as written in the overrides: applied as is on arrival. */
    public String target;
    /** True when heading back to the base value: the property is no longer overridden on arrival. */
    public boolean toBase;

    public final float[] from = new float[MAX_CHANNELS];
    public final float[] to = new float[MAX_CHANNELS];
    public final float[] current = new float[MAX_CHANNELS];

    public float duration;
    public float elapsed;
    public Interpolation interpolation;

    @Override
    public void reset() {
        key = null;
        handler = null;
        target = null;
        toBase = false;
        duration = 0;
        elapsed = 0;
        interpolation = null;
    }
}
