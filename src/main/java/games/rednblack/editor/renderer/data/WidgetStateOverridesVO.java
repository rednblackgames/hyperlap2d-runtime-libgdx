package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * What one widget state overrides on a part: {@code property key -> value}, and for the properties
 * that should not jump to that value, {@code property key -> transition}.
 *
 * A class of its own rather than a bare map inside {@link WidgetPartVO#overrides}: the json writer
 * only knows the declared type of a map's values one level deep, and would wrap every value of a
 * nested map in an object carrying its class name. As fields, the maps are written plainly.
 */
public class WidgetStateOverridesVO {
    public ObjectMap<String, String> values = new ObjectMap<>(0);
    /** Only for keys of {@link #values}; a property with no entry here changes instantly. */
    public ObjectMap<String, WidgetOverrideTransitionVO> transitions = new ObjectMap<>(0);
    /** Only for keys of {@link #values}; what plays around the value, for properties that play. */
    public ObjectMap<String, WidgetOverrideSequenceVO> sequences = new ObjectMap<>(0);

    public WidgetStateOverridesVO() {
    }

    public WidgetStateOverridesVO(WidgetStateOverridesVO vo) {
        values.putAll(vo.values);
        for (ObjectMap.Entry<String, WidgetOverrideTransitionVO> entry : vo.transitions) {
            if (entry.value != null) transitions.put(entry.key, new WidgetOverrideTransitionVO(entry.value));
        }
        for (ObjectMap.Entry<String, WidgetOverrideSequenceVO> entry : vo.sequences) {
            if (entry.value != null) sequences.put(entry.key, new WidgetOverrideSequenceVO(entry.value));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetStateOverridesVO that = (WidgetStateOverridesVO) o;
        return values.equals(that.values) && transitions.equals(that.transitions) && sequences.equals(that.sequences);
    }

    @Override
    public int hashCode() {
        return (values.hashCode() * 31 + transitions.hashCode()) * 31 + sequences.hashCode();
    }
}
