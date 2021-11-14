package games.rednblack.editor.renderer.factory.v2;

import com.artemis.Archetype;
import com.artemis.ArchetypeBuilder;
import com.artemis.ComponentMapper;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Pool;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public abstract class ComponentFactoryV2 {
    protected ComponentMapper<BoundingBoxComponent> boundingBoxCM;
    protected ComponentMapper<DimensionsComponent> dimensionsCM;
    protected ComponentMapper<LightBodyComponent> lightBodyCM;
    protected ComponentMapper<MainItemComponent> mainItemCM;
    protected ComponentMapper<NodeComponent> nodeCM;
    protected ComponentMapper<ParentNodeComponent> parentNodeCM;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    protected ComponentMapper<PolygonComponent> polygonCM;
    protected ComponentMapper<CircleShapeComponent> circleShapeCM;
    protected ComponentMapper<ScriptComponent> scriptCM;
    protected ComponentMapper<SensorComponent> sensorCM;
    protected ComponentMapper<ShaderComponent> shaderCM;
    protected ComponentMapper<TintComponent> tintCM;
    protected ComponentMapper<TransformComponent> transformCM;
    protected ComponentMapper<ZIndexComponent> zIndexCM;

    protected IResourceRetriever rm;
    protected RayHandler rayHandler;
    protected World world;
    protected com.artemis.World engine;

    private Archetype entityArchetype;

    /**
     * Do call injectDependencies manually when using this constructor!
     */
    public ComponentFactoryV2() {
    }

    public ComponentFactoryV2(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        injectDependencies(engine, rayHandler, world, rm);
    }

    public void injectDependencies(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        this.engine = engine;
        this.engine.inject(this);
        this.rayHandler = rayHandler;
        this.world = world;
        this.rm = rm;

        this.entityArchetype = new ArchetypeBuilder()
                .add(DimensionsComponent.class)
                .add(BoundingBoxComponent.class)
                .add(MainItemComponent.class)
                .add(TransformComponent.class)
                .add(ParentNodeComponent.class)

                .add(TintComponent.class)
                .add(ZIndexComponent.class)
                .add(ScriptComponent.class)

                .build(engine);
    }

    protected void adjustNodeHierarchy(int root, int entity) {
        if (root == -1) {
            parentNodeCM.remove(entity);
            return;
        }
        // Add this component to its parents children references
        nodeCM.get(root).addChild(entity);
        // Set the entity's parent reference to it's parent
        if (!parentNodeCM.has(entity)) parentNodeCM.create(entity);
        parentNodeCM.get(entity).parentEntity = root;
    }

    public void setResourceManager(IResourceRetriever rm) {
        this.rm = rm;
    }

    public void initialize(int entity) {
        initializeTransientComponents(entity);

        initializeDimensionsComponent(entity);

        postEntityInitialization(entity);
    }

    protected void initializeTransientComponents(int entity) {
        if (scriptCM.has(entity)) {
            scriptCM.get(entity).engine = engine;
        }

        if (physicsBodyCM.has(entity)) {
            engine.inject(physicsBodyCM.get(entity));
        }

        if (sensorCM.has(entity)) {
            engine.inject(sensorCM.get(entity));
        }

        if (lightBodyCM.has(entity)) {
            engine.inject(lightBodyCM.get(entity));
        }

        if (shaderCM.has(entity)) {
            ShaderComponent component = shaderCM.get(entity);
            component.setShader(component.shaderName, rm.getShaderProgram(component.shaderName));
        }
    }

    protected abstract void initializeDimensionsComponent(int entity);

    protected void postEntityInitialization(int entity) {
        TransformComponent transform = transformCM.get(entity);
        DimensionsComponent dimension = dimensionsCM.get(entity);

        if (Float.isNaN(transform.originX)) transform.originX = dimension.width / 2f;

        if (Float.isNaN(transform.originY)) transform.originY = dimension.height / 2f;
    }

    public int createEntity(int root, InitialData initialData) {
        //Create Artemis Entity
        int entity = engine.create(entityArchetype);
        mainItemCM.get(entity).entityType = getEntityType();

        //Specialize the entity for the current factory
        transmuteEntity(entity);

        //Set initial data for components
        TransformComponent t = transformCM.get(entity);
        t.x = initialData.x;
        t.y = initialData.y;
        ZIndexComponent z = zIndexCM.get(entity);
        if (initialData.layerName == null || initialData.layerName.isEmpty()) initialData.layerName = "Default";
        z.layerName = initialData.layerName;
        //Set factory specific initial data
        setInitialData(entity, initialData.data);

        adjustNodeHierarchy(root, entity);

        initialize(entity);

        return entity;
    }

    public abstract void transmuteEntity(int entity);

    public abstract int getEntityType();

    public abstract void setInitialData(int entity, Object data);

    public static class InitialData implements Pool.Poolable {
        public float x, y;
        public String layerName;
        public Object data;

        @Override
        public void reset() {
            x = 0;
            y = 0;
            layerName = "";
            data = null;
        }
    }
}
