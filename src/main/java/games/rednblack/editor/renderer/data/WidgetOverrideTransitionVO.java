package games.rednblack.editor.renderer.data;

import java.util.Objects;

/**
 * How an overridden property gets to its value instead of jumping there: over how long, and along
 * which of the libGDX interpolation functions (by field name, see
 * {@link games.rednblack.editor.renderer.utils.InterpolationMap}).
 */
public class WidgetOverrideTransitionVO {
    public static final float DEFAULT_DURATION = 0.15f;
    public static final String DEFAULT_INTERPOLATION = "linear";

    public float duration = DEFAULT_DURATION;
    public String interpolation = DEFAULT_INTERPOLATION;

    public WidgetOverrideTransitionVO() {
    }

    public WidgetOverrideTransitionVO(float duration, String interpolation) {
        this.duration = duration;
        this.interpolation = interpolation;
    }

    public WidgetOverrideTransitionVO(WidgetOverrideTransitionVO vo) {
        duration = vo.duration;
        interpolation = vo.interpolation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetOverrideTransitionVO that = (WidgetOverrideTransitionVO) o;
        return Float.compare(that.duration, duration) == 0 && Objects.equals(interpolation, that.interpolation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, interpolation);
    }
}
