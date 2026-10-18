package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;

/** What a child asks of the auto layout of its parent. A child without one asks for nothing special. */
public class AutoLayoutItemComponent extends PooledComponent {

    /**
     * Share of the room left in a row or a column, against the other children that grow. A growing
     * child does not keep its own length: it takes its share, so it can shrink back as well.
     */
    public float grow;
    /** How it sits across a row or a column, null for the way the layout says. */
    public AutoLayoutComponent.Align alignSelf;
    /** Stays where it is put and takes no room in the layout. */
    public boolean ignore;

    public boolean isDefault() {
        return grow == 0 && alignSelf == null && !ignore;
    }

    @Override
    public void reset() {
        grow = 0;
        alignSelf = null;
        ignore = false;
    }
}
