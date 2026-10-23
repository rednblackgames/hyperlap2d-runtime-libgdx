package games.rednblack.editor.renderer.systems.action.data;

import games.rednblack.editor.renderer.components.LayoutComponent;

/**
 * Moves one constraint of a layout to a new margin: the entity keeps being placed by its layout,
 * and whatever is anchored to it follows.
 */
public class LayoutMarginToData extends TemporalData {
    public LayoutComponent.ConstraintSide side;
    public float start;
    public float end;

    public void setSide(LayoutComponent.ConstraintSide side) {
        this.side = side;
    }

    public void setEnd(float end) {
        this.end = end;
    }

    @Override
    public void reset() {
        super.reset();

        side = null;
        start = 0;
        end = 0;
    }
}
