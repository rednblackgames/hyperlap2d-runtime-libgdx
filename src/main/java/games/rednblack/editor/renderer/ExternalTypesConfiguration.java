package games.rednblack.editor.renderer;

import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.commons.IExternalItemType;

public class ExternalTypesConfiguration extends Array<IExternalItemType> {

    public ExternalTypesConfiguration() {
        super(false, 16);
    }

    public void addExternalItemType(IExternalItemType itemType) {
        add(itemType);
    }
}
