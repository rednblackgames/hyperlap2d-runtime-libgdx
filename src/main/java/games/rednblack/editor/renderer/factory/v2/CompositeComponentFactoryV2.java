package games.rednblack.editor.renderer.factory.v2;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.LayerMapComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.data.LayerItemVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class CompositeComponentFactoryV2 extends ComponentFactoryV2 {

    protected ComponentMapper<LayerMapComponent> layerMapCM;

    private final EntityTransmuter transmuter;

    public CompositeComponentFactoryV2(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(NodeComponent.class)
                .add(CompositeTransformComponent.class)
                .add(LayerMapComponent.class)
                .build();
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {

    }

    @Override
    protected void postEntityInitialization(int entity) {
        super.postEntityInitialization(entity);

        DimensionsComponent dimensions = dimensionsCM.get(entity);
        if (dimensions.boundBox == null)
            dimensions.boundBox = new Rectangle(0, 0, dimensions.width, dimensions.height);
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.COMPOSITE_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        LayerMapComponent layerMap = layerMapCM.get(entity);
        if (layerMap.getLayers().size() == 0)
            layerMap.addLayer(createDefaultLayer());
    }

    public static LayerItemVO createDefaultLayer() {
        LayerItemVO layerItemVO = new LayerItemVO();
        layerItemVO.layerName = "Default";
        layerItemVO.isVisible = true;
        return layerItemVO;
    }
}
