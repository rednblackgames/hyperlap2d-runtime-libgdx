package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.EntityId;

public class LayoutComponent extends PooledComponent {

    public ConstraintData left = null;
    public ConstraintData right = null;
    public ConstraintData top = null;
    public ConstraintData bottom = null;

    public float horizontalBias = 0.5f;
    public float verticalBias = 0.5f;

    public transient float checksum;

    public static class ConstraintData {
        @EntityId public int targetEntity = -1; // -1 = parent
        public ConstraintSide targetSide;
        public float margin = 0f;

        public transient String targetUniqueId = null; // for lazy resolution
        public transient boolean resolved = false;
    }

    public enum ConstraintSide {
        LEFT, RIGHT, TOP, BOTTOM
    }

    @Override
    public void reset() {
        left = null;
        right = null;
        top = null;
        bottom = null;
        horizontalBias = 0.5f;
        verticalBias = 0.5f;
        checksum = 0;
    }
}
