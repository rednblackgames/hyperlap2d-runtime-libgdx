package games.rednblack.editor.renderer.factory.component;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.data.Image9patchVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class NinePatchComponentFactory extends ComponentFactory {

    protected ComponentMapper<NinePatchComponent> ninePatchCM;

    private final EntityTransmuter transmuter;

    public NinePatchComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(ParentNodeComponent.class)
                .add(NinePatchComponent.class)
                .build();
    }

    @Override
    public int createSpecialisedEntity(int root, MainItemVO vo) {
        int entity = createGeneralEntity(vo, EntityFactory.NINE_PATCH);
        transmuter.transmute(entity);

        NinePatchComponent ninePatchComponent = ninePatchCM.get(entity);
        createNinePatchComponent(ninePatchComponent, (Image9patchVO) vo);

        adjustNodeHierarchy(root, entity);

        return entity;
    }

    protected void initializeDimensionsComponent(int entity, DimensionsComponent component, MainItemVO vo) {
        Image9patchVO sVo = (Image9patchVO) vo;
        TextureRegion region = rm.getTextureRegion(sVo.imageName);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.height = ((Image9patchVO) vo).height;
        component.width = ((Image9patchVO) vo).width;
        if (component.width == 0) {
            component.width = (float) region.getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        }

        if (component.height == 0) {
            component.height = (float) region.getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;
        }
    }

    private void createNinePatchComponent(NinePatchComponent component, Image9patchVO vo) {
        AtlasRegion atlasRegion = (AtlasRegion) rm.getTextureRegion(vo.imageName);
        int[] splits = atlasRegion.findValue("split");
        if (splits == null) {
            splits = new int[]{0, 0, 0, 0};
        }
        component.ninePatch = new NinePatch(atlasRegion, splits[0], splits[1], splits[2], splits[3]);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.ninePatch.scale(multiplier / projectInfoVO.pixelToWorld, multiplier / projectInfoVO.pixelToWorld);

        component.textureRegionName = vo.imageName;
    }

}
