package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.Objects;

public class LayoutConstraintVO {

    public ConstraintDataVO left = null;
    public ConstraintDataVO right = null;
    public ConstraintDataVO top = null;
    public ConstraintDataVO bottom = null;
    public float horizontalBias = 0.5f;
    public float verticalBias = 0.5f;
    public boolean matchConstraintWidth = false;
    public boolean matchConstraintHeight = false;

    public static class ConstraintDataVO {
        public String targetUniqueId = null; // null = parent
        public LayoutComponent.ConstraintSide targetSide;
        public float margin = 0f;

        public ConstraintDataVO() {
        }

        public ConstraintDataVO(ConstraintDataVO vo) {
            if (vo == null) return;
            targetUniqueId = vo.targetUniqueId;
            targetSide = vo.targetSide;
            margin = vo.margin;
        }

        public void loadFromComponent(LayoutComponent.ConstraintData data, Engine engine) {
            if (data.targetEntity == -1) {
                targetUniqueId = null;
            } else {
                MainItemComponent mic = ComponentRetriever.get(data.targetEntity, MainItemComponent.class, engine);
                targetUniqueId = mic != null ? mic.uniqueId : null;
            }
            targetSide = data.targetSide;
            margin = data.margin;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConstraintDataVO that = (ConstraintDataVO) o;
            return Float.compare(that.margin, margin) == 0 &&
                    Objects.equals(targetUniqueId, that.targetUniqueId) &&
                    targetSide == that.targetSide;
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetUniqueId, targetSide, margin);
        }
    }

    public LayoutConstraintVO() {
    }

    public LayoutConstraintVO(LayoutConstraintVO vo) {
        if (vo == null) return;
        if (vo.left != null) left = new ConstraintDataVO(vo.left);
        if (vo.right != null) right = new ConstraintDataVO(vo.right);
        if (vo.top != null) top = new ConstraintDataVO(vo.top);
        if (vo.bottom != null) bottom = new ConstraintDataVO(vo.bottom);
        horizontalBias = vo.horizontalBias;
        verticalBias = vo.verticalBias;
        matchConstraintWidth = vo.matchConstraintWidth;
        matchConstraintHeight = vo.matchConstraintHeight;
    }

    public void loadFromComponent(LayoutComponent comp, Engine engine) {
        if (comp.left != null) {
            left = new ConstraintDataVO();
            left.loadFromComponent(comp.left, engine);
        }
        if (comp.right != null) {
            right = new ConstraintDataVO();
            right.loadFromComponent(comp.right, engine);
        }
        if (comp.top != null) {
            top = new ConstraintDataVO();
            top.loadFromComponent(comp.top, engine);
        }
        if (comp.bottom != null) {
            bottom = new ConstraintDataVO();
            bottom.loadFromComponent(comp.bottom, engine);
        }
        horizontalBias = comp.horizontalBias;
        verticalBias = comp.verticalBias;
        matchConstraintWidth = comp.matchConstraintWidth;
        matchConstraintHeight = comp.matchConstraintHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayoutConstraintVO that = (LayoutConstraintVO) o;
        return Float.compare(that.horizontalBias, horizontalBias) == 0 &&
                Float.compare(that.verticalBias, verticalBias) == 0 &&
                matchConstraintWidth == that.matchConstraintWidth &&
                matchConstraintHeight == that.matchConstraintHeight &&
                Objects.equals(left, that.left) &&
                Objects.equals(right, that.right) &&
                Objects.equals(top, that.top) &&
                Objects.equals(bottom, that.bottom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, top, bottom, horizontalBias, verticalBias, matchConstraintWidth, matchConstraintHeight);
    }
}
