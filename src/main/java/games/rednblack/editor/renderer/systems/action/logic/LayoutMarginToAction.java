package games.rednblack.editor.renderer.systems.action.logic;

import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.systems.action.data.LayoutMarginToData;

/**
 * Takes the margin of one of the layout's constraints from where it is to where it is asked to be.
 * The layout system places the entity from there, as it does with any margin it is given. An entity
 * without that constraint is left as it is.
 */
public class LayoutMarginToAction<T extends LayoutMarginToData> extends TemporalAction<T> {

    @Override
    public void update(float percent, int entity, T actionData) {
        LayoutComponent.ConstraintData constraint = constraint(entity, actionData.side);
        if (constraint == null) return;

        constraint.margin = actionData.start + (actionData.end - actionData.start) * percent;
    }

    @Override
    public void begin(int entity, T actionData) {
        LayoutComponent.ConstraintData constraint = constraint(entity, actionData.side);
        if (constraint == null) return;

        actionData.start = constraint.margin;
    }

    @Override
    public void end(int entity, T actionData) {

    }

    private LayoutComponent.ConstraintData constraint(int entity, LayoutComponent.ConstraintSide side) {
        LayoutComponent layout = layoutMapper.get(entity);
        if (layout == null || side == null) return null;
        switch (side) {
            case LEFT: return layout.left;
            case RIGHT: return layout.right;
            case TOP: return layout.top;
            default: return layout.bottom;
        }
    }
}
