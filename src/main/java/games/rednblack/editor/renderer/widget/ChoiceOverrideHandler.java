package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.Array;

/**
 * A property whose values are a known list rather than anything the user may type, such as the
 * animations a skeleton holds. The editor offers them instead of asking for a name.
 */
public interface ChoiceOverrideHandler extends StateOverrideHandler {

    /** @return the values this property may take on this entity, empty if they cannot be listed */
    Array<String> getChoices(int entity);
}
