package games.rednblack.editor.renderer.factory.component;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.data.ColorPrimitiveVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class ColorPrimitiveComponentFactory extends ComponentFactory {

    protected ComponentMapper<TextureRegionComponent> textureRegionCM;

    private final EntityTransmuter transmuter;

    public ColorPrimitiveComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(TextureRegionComponent.class)
                .add(PolygonComponent.class)
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

        PolygonComponent polygonComponent = polygonCM.get(entity);
        polygonComponent.vertices = (Vector2[][]) data;
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

        PolygonComponent polygonComponent = polygonCM.get(entity);
        component.setPolygonSprite(polygonComponent);
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        DimensionsComponent dimension = dimensionsCM.get(entity);

        PolygonComponent polygonComponent = polygonCM.get(entity);
        dimension.setFromShape(polygonComponent.vertices);
        dimension.setPolygon(polygonComponent);
    }
}
