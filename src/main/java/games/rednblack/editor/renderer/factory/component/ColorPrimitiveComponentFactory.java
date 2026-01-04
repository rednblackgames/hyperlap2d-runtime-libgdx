package games.rednblack.editor.renderer.factory.component;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EntityTransmuter;
import games.rednblack.editor.renderer.ecs.EntityTransmuterFactory;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.data.ColorPrimitiveVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class ColorPrimitiveComponentFactory extends ComponentFactory {

    protected ComponentMapper<TextureRegionComponent> textureRegionCM;

    private final EntityTransmuter transmuter;

    public ColorPrimitiveComponentFactory(Engine engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(TextureRegionComponent.class)
                .add(PolygonShapeComponent.class)
                .build();
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.COLOR_PRIMITIVE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        initializeSpecialComponentsFromVO(entity, null);
        Object[] params = (Object[]) data;

        PolygonShapeComponent polygonShapeComponent = polygonCM.get(entity);
        polygonShapeComponent.vertices = (Array<Vector2>) params[0];
        polygonShapeComponent.polygonizedVertices = (Vector2[][]) params[1];
    }

    @Override
    public Class<ColorPrimitiveVO> getVOType() {
        return ColorPrimitiveVO.class;
    }

    @Override
    public void initializeSpecialComponentsFromVO(int entity, MainItemVO voG) {
        ColorPrimitiveVO vo = (ColorPrimitiveVO) voG;
        TextureRegionComponent component = textureRegionCM.get(entity);
        component.regionName = "white-pixel";
        component.isRepeat = false;
        component.isPolygon = true;
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        TextureRegionComponent component = textureRegionCM.get(entity);
        engine.inject(component);
        component.region = rm.getTextureRegion(component.regionName);
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        component.ppwu = projectInfoVO.pixelToWorld;

        PolygonShapeComponent polygonShapeComponent = polygonCM.get(entity);
        component.setPolygonSprite(polygonShapeComponent);
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        DimensionsComponent dimension = dimensionsCM.get(entity);

        PolygonShapeComponent polygonShapeComponent = polygonCM.get(entity);
        if (polygonShapeComponent.vertices == null) return;
        dimension.setPolygon(polygonShapeComponent);
        dimension.width = dimension.polygon.getBoundingRectangle().width;
        dimension.height = dimension.polygon.getBoundingRectangle().height;
    }
}
