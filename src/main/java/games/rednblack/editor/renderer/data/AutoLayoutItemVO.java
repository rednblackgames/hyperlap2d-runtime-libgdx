package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.AutoLayoutItemComponent;

import java.util.Objects;

public class AutoLayoutItemVO {
    public float grow;
    public AutoLayoutComponent.Align alignSelf;
    public boolean ignore;

    public AutoLayoutItemVO() {
    }

    public AutoLayoutItemVO(AutoLayoutItemVO vo) {
        if (vo == null) return;
        grow = vo.grow;
        alignSelf = vo.alignSelf;
        ignore = vo.ignore;
    }

    public void loadFromComponent(AutoLayoutItemComponent component) {
        grow = component.grow;
        alignSelf = component.alignSelf;
        ignore = component.ignore;
    }

    public void applyToComponent(AutoLayoutItemComponent component) {
        component.grow = Math.max(0, grow);
        component.alignSelf = alignSelf;
        component.ignore = ignore;
    }

    public boolean isDefault() {
        return grow == 0 && alignSelf == null && !ignore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoLayoutItemVO that = (AutoLayoutItemVO) o;
        return Float.compare(grow, that.grow) == 0 && ignore == that.ignore && alignSelf == that.alignSelf;
    }

    @Override
    public int hashCode() {
        return Objects.hash(grow, alignSelf, ignore);
    }
}
