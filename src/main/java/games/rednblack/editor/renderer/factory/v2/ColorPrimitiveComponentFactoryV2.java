package games.rednblack.editor.renderer.factory.v2;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class ColorPrimitiveComponentFactoryV2 extends ComponentFactoryV2 {

    protected ComponentMapper<TextureRegionComponent> textureRegionCM;

    private final EntityTransmuter transmuter;

    public ColorPrimitiveComponentFactoryV2(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
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
        return EntityFactoryV2.COLOR_PRIMITIVE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        TextureRegionComponent component = textureRegionCM.get(entity);
        component.regionName = "white-pixel";
        component.isRepeat = false;
        component.isPolygon = true;

        PolygonComponent polygonComponent = polygonCM.get(entity);
        polygonComponent.vertices = (Vector2[][]) data;
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
