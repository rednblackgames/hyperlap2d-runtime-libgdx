package games.rednblack.editor.renderer.input;

/**
 * Told when a press lands outside an open overlay, which is how a drop down list knows to close.
 *
 * The press itself goes on to whatever is under it, the way scene2d does it: clicking another button
 * while a list is open closes the list and presses the button.
 */
public interface OverlayListener {

    /**
     * @param overlay the overlay the press missed
     * @param event   the press, in the coordinates of the scene
     */
    void touchedOutside(int overlay, UIEvent event);
}
