package games.rednblack.editor.renderer.factory.v2;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class NinePatchComponentFactoryV2 extends ComponentFactoryV2 {
    protected ComponentMapper<NinePatchComponent> ninePatchCM;

    private final EntityTransmuter transmuter;

    public NinePatchComponentFactoryV2(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(NinePatchComponent.class)
                .build();
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactoryV2.NINE_PATCH;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        ninePatchCM.get(entity).textureRegionName = (String) data;
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        NinePatchComponent ninePatchComponent = ninePatchCM.get(entity);
        createNinePatchComponent(ninePatchComponent, ninePatchComponent.textureRegionName);
    }

    private void createNinePatchComponent(NinePatchComponent component, String imageName) {
        TextureAtlas.AtlasRegion atlasRegion = (TextureAtlas.AtlasRegion) rm.getTextureRegion(imageName);
        int[] splits = atlasRegion.findValue("split");
        if (splits == null) {
            splits = new int[]{0, 0, 0, 0};
        }
        component.ninePatch = new NinePatch(atlasRegion, splits[0], splits[1], splits[2], splits[3]);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.ninePatch.scale(multiplier / projectInfoVO.pixelToWorld, multiplier / projectInfoVO.pixelToWorld);
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        NinePatchComponent ninePatchComponent = ninePatchCM.get(entity);
        TextureRegion region = rm.getTextureRegion(ninePatchComponent.textureRegionName);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        DimensionsComponent component = dimensionsCM.get(entity);
        if (component.width == 0) {
            component.width = (float) region.getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        }

        if (component.height == 0) {
            component.height = (float) region.getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;
        }
    }
}
