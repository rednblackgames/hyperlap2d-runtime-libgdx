package games.rednblack.editor.renderer.factory.component;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EntityTransmuter;
import games.rednblack.editor.renderer.ecs.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.data.SimpleImageVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ABAtlasRegion;

public class SimpleImageComponentFactory extends ComponentFactory {

    protected ComponentMapper<TextureRegionComponent> textureRegionCM;
    protected ComponentMapper<NormalMapRendering> normalMapRenderingCM;

    private final EntityTransmuter transmuter;

    public SimpleImageComponentFactory(Engine engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(TextureRegionComponent.class)
                .add(NormalMapRendering.class)
                .build();
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.IMAGE_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        textureRegionCM.get(entity).regionName = (String) data;
    }

    @Override
    public Class<SimpleImageVO> getVOType() {
        return SimpleImageVO.class;
    }

    @Override
    public void initializeSpecialComponentsFromVO(int entity, MainItemVO voG) {
        SimpleImageVO vo = (SimpleImageVO) voG;
        TextureRegionComponent textureRegionComponent = textureRegionCM.get(entity);
        textureRegionComponent.regionName = vo.imageName;
        textureRegionComponent.isRepeat = vo.isRepeat;
        textureRegionComponent.isPolygon = vo.isPolygon;
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        TextureRegionComponent component = textureRegionCM.get(entity);
        engine.inject(component);

        if (rm.hasTextureRegion(component.regionName + ".normal")) {
            TextureAtlas.AtlasRegion regionDiffuse = (TextureAtlas.AtlasRegion) rm.getTextureRegion(component.regionName);
            TextureAtlas.AtlasRegion normalRegion = (TextureAtlas.AtlasRegion) rm.getTextureRegion(component.regionName + ".normal");
            component.region = new ABAtlasRegion(regionDiffuse, normalRegion, normalMapRenderingCM.get(entity));
        } else {
            normalMapRenderingCM.remove(entity);
            component.region = rm.getTextureRegion(component.regionName);
        }
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        TextureRegionComponent component = textureRegionCM.get(entity);
        DimensionsComponent dimension = dimensionsCM.get(entity);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        dimension.width = (float) component.region.getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        dimension.height = (float) component.region.getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;

        updatePolygons(entity);
    }

    private void updatePolygons(int entity) {
        DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);
        PolygonShapeComponent polygonShapeComponent = polygonCM.get(entity);

        TextureRegionComponent textureRegionComponent = textureRegionCM.get(entity);
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        textureRegionComponent.ppwu = projectInfoVO.pixelToWorld;
        if (textureRegionComponent.isPolygon && polygonShapeComponent != null && polygonShapeComponent.vertices != null) {
            textureRegionComponent.setPolygonSprite(polygonShapeComponent);
            dimensionsComponent.setPolygon(polygonShapeComponent);
        }
    }
}
