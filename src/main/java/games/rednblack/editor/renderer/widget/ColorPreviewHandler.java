package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.graphics.Color;

/**
 * A property whose value is a colour, at least on some items, so the editor can show it as one
 * rather than as the string it is stored as. Each handler reads its own way of writing colours.
 */
public interface ColorPreviewHandler extends StateOverrideHandler {

    /** @return true if on this item the value is a colour, in which case it is written into out */
    boolean toColor(int entity, String value, Color out);
}
