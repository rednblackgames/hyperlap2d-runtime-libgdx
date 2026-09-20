package games.rednblack.editor.renderer.widget;

/**
 * Knows how to read and write one overridable property of an entity, the value travelling as a
 * string so that overrides stay plain data in the scene file.
 *
 * Handlers are injected by the engine when registered, so they can declare
 * {@code ComponentMapper} (and {@code SceneLoader}) fields like a system does.
 */
public interface StateOverrideHandler {

    /** @return true if the entity owns the property this handler drives */
    boolean supports(int entity);

    /** @return the current value of the property, in the same format {@link #apply} accepts */
    String capture(int entity);

    void apply(int entity, String value);
}
