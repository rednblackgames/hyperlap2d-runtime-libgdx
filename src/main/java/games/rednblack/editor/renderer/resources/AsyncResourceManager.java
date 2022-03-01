package games.rednblack.editor.renderer.resources;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.ObjectSet;
import games.rednblack.editor.renderer.ExternalTypesConfiguration;
import games.rednblack.editor.renderer.commons.IExternalItemType;

import java.util.HashMap;

public class AsyncResourceManager extends ResourceManager {

    public AsyncResourceManager() {
        super();
    }

    public AsyncResourceManager(ExternalTypesConfiguration externalTypesConfiguration) {
        super(externalTypesConfiguration);
    }

    public void addAtlasPack(String name, TextureAtlas pack) {
        this.atlasesPack.put(name, pack);
    }

    @Override
    public void loadExternalTypes() {
        throw new RuntimeException("Use loadExternalTypesAsync() or loadExternalTypesSync()");
    }

    public void loadExternalTypesAsync() {
        for (int assetType : externalItemsToLoad.keys()) {
            IExternalItemType externalItemType = externalItemTypes.get(assetType);
            ObjectSet<String> assetsToLoad = externalItemsToLoad.get(assetType);
            HashMap<String, Object> assets = getExternalItems(assetType);

            externalItemType.loadExternalTypesAsync(this, assetsToLoad, assets);
        }
    }

    public void loadExternalTypesSync() {
        for (int assetType : externalItemsToLoad.keys()) {
            IExternalItemType externalItemType = externalItemTypes.get(assetType);
            ObjectSet<String> assetsToLoad = externalItemsToLoad.get(assetType);
            HashMap<String, Object> assets = getExternalItems(assetType);

            externalItemType.loadExternalTypesSync(this, assetsToLoad, assets);
        }
    }
}
