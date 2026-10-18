package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;

/**
 * A child's own size, kept while its parent's auto layout gives it another one (it grows, or it is
 * stretched), so it gets it back as soon as the layout stops sizing it. NaN on an axis the layout
 * does not size. Saving writes this size, not the one the layout gave.
 */
@Transient
public class AutoLayoutOwnSizeComponent extends PooledComponent {

    public float width = Float.NaN;
    public float height = Float.NaN;

    /** @return the size the entity has of its own: the one kept, else the one it has */
    public static float ownWidth(AutoLayoutOwnSizeComponent own, DimensionsComponent dimensions) {
        return own != null && !Float.isNaN(own.width) ? own.width : dimensions.width;
    }

    public static float ownHeight(AutoLayoutOwnSizeComponent own, DimensionsComponent dimensions) {
        return own != null && !Float.isNaN(own.height) ? own.height : dimensions.height;
    }

    @Override
    public void reset() {
        width = Float.NaN;
        height = Float.NaN;
    }
}
