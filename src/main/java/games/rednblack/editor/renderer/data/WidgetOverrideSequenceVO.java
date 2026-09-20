package games.rednblack.editor.renderer.data;

import java.util.Objects;

/**
 * What plays around the value a state holds a property at: an animation played once when the state
 * begins, and one played once when it ends. Named after what they are, an entry and an exit, since
 * the value itself is what keeps playing in between.
 */
public class WidgetOverrideSequenceVO {
    /** Played once when the state is entered, before the value takes over. Empty for none. */
    public String enter = "";
    /** Played once when the state is left, before whatever comes next. Empty for none. */
    public String exit = "";

    public WidgetOverrideSequenceVO() {
    }

    public WidgetOverrideSequenceVO(String enter, String exit) {
        this.enter = enter == null ? "" : enter;
        this.exit = exit == null ? "" : exit;
    }

    public WidgetOverrideSequenceVO(WidgetOverrideSequenceVO vo) {
        this(vo.enter, vo.exit);
    }

    public boolean isEmpty() {
        return enter.isEmpty() && exit.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetOverrideSequenceVO that = (WidgetOverrideSequenceVO) o;
        return Objects.equals(enter, that.enter) && Objects.equals(exit, that.exit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enter, exit);
    }
}
