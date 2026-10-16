package games.rednblack.editor.renderer.components.additional;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.input.OverlayListener;

/**
 * Marks an entity that is open over the rest of the scene, such as the list of a select box or a
 * window: it is drawn after everything else and outside the clipping of the composites it sits in,
 * and input finds it before anything else.
 *
 * The entity stays where it is in the tree, so it keeps following its parent. Open and close it with
 * {@link games.rednblack.editor.renderer.systems.UIInputSystem#openOverlay} and
 * {@link games.rednblack.editor.renderer.systems.UIInputSystem#closeOverlay} rather than adding this
 * by hand: the input system keeps the order they were opened in.
 *
 * Inside a composite that renders to a frame buffer the entity is drawn in place, since the buffer
 * has coordinates of its own; input still finds it first.
 */
@Transient
public class OverlayComponent extends PooledComponent {

    /** True for an overlay nothing behind can be reached through: presses, hover, the wheel and the keys. */
    public boolean modal = false;
    /** Told of a press landing outside the overlay, null for an overlay that does not care. */
    public OverlayListener listener = null;
    /** When it was opened among the others: the later, the more in front. */
    public int order = 0;

    @Override
    public void reset() {
        modal = false;
        listener = null;
        order = 0;
    }
}
