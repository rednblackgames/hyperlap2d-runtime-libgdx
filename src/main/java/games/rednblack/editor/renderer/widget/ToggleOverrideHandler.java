package games.rednblack.editor.renderer.widget;

/**
 * A property that is simply on or off, and whose base is on: a state only ever has something to say
 * about it by turning it off. The editor gives it a switch of its own rather than a line in the list
 * of overridden values.
 *
 * Values are the two strings {@code true} and {@code false}.
 */
public interface ToggleOverrideHandler extends StateOverrideHandler {
}
