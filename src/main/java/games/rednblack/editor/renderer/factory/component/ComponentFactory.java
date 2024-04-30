package games.rednblack.editor.renderer.factory.component;

import com.artemis.Archetype;
import com.artemis.ArchetypeBuilder;
import com.artemis.ComponentMapper;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.components.shape.CircleShapeComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public abstract class ComponentFactory {
    protected ComponentMapper<BoundingBoxComponent> boundingBoxCM;
    protected ComponentMapper<DimensionsComponent> dimensionsCM;
    protected ComponentMapper<LightBodyComponent> lightBodyCM;
    protected ComponentMapper<MainItemComponent> mainItemCM;
    protected ComponentMapper<NodeComponent> nodeCM;
    protected ComponentMapper<ParentNodeComponent> parentNodeCM;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    protected ComponentMapper<PolygonShapeComponent> polygonCM;
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
    public ComponentFactory() {
    }

    public ComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
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

    public void adjustNodeHierarchy(int root, int entity) {
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
        z.setLayerName(initialData.layerName);
        //Set factory specific initial data
        setInitialData(entity, initialData.data);

        adjustNodeHierarchy(root, entity);

        initialize(entity);

        return entity;
    }

    public abstract void transmuteEntity(int entity);

    public abstract int getEntityType();

    public abstract void setInitialData(int entity, Object data);

    public abstract Class<? extends MainItemVO> getVOType();

    public int createEntity(int root, MainItemVO vo) {
        //Create Artemis Entity
        int entity = engine.create(entityArchetype);
        mainItemCM.get(entity).entityType = getEntityType();

        //Specialize the entity for the current factory
        transmuteEntity(entity);

        initializeComponentsFromVO(entity, vo);

        initializeSpecialComponentsFromVO(entity, vo);

        adjustNodeHierarchy(root, entity);

        initialize(entity);

        return entity;
    }

    protected void initializeComponentsFromVO(int entity, MainItemVO vo) {
        MainItemComponent mainItemComponent = mainItemCM.get(entity);
        mainItemComponent.uniqueId = vo.uniqueId;
        mainItemComponent.customVariables.putAll(vo.customVariables);
        mainItemComponent.itemIdentifier = vo.itemIdentifier;
        mainItemComponent.libraryLink = vo.itemName;
        if (vo.tags != null) {
            for (String tag : vo.tags)
                mainItemComponent.tags.add(tag);
        }

        TransformComponent transformComponent = transformCM.get(entity);
        transformComponent.rotation = vo.rotation;
        transformComponent.scaleX = vo.scaleX;
        transformComponent.scaleY = vo.scaleY;
        transformComponent.x = vo.x;
        transformComponent.y = vo.y;
        transformComponent.originX = vo.originX;
        transformComponent.originY = vo.originY;
        transformComponent.flipX = vo.flipX;
        transformComponent.flipY = vo.flipY;

        TintComponent tintComponent = tintCM.get(entity);
        tintComponent.color.set(vo.tint[0], vo.tint[1], vo.tint[2], vo.tint[3]);

        ZIndexComponent zIndexComponent = zIndexCM.get(entity);
        if (vo.layerName == null || vo.layerName.isEmpty()) vo.layerName = "Default";

        zIndexComponent.setLayerName(vo.layerName);
        zIndexComponent.setZIndex(vo.zIndex);
        zIndexComponent.needReOrder = false;

        if (vo.shape != null) {
            PolygonShapeComponent polygonShapeComponent = polygonCM.create(entity);
            polygonShapeComponent.polygonizedVertices = new Vector2[vo.shape.polygonizedVertices.length][];
            for (int i = 0; i < vo.shape.polygonizedVertices.length; i++) {
                polygonShapeComponent.polygonizedVertices[i] = new Vector2[vo.shape.polygonizedVertices[i].length];
                System.arraycopy(vo.shape.polygonizedVertices[i], 0, polygonShapeComponent.polygonizedVertices[i], 0, vo.shape.polygonizedVertices[i].length);
            }
            polygonShapeComponent.vertices = new Array<>(true, vo.shape.vertices.size, Vector2.class);
            polygonShapeComponent.vertices.addAll(vo.shape.vertices);
            polygonShapeComponent.openEnded = vo.shape.openEnded;
        }

        if (vo.circle != null) {
            CircleShapeComponent circleShapeComponent = circleShapeCM.create(entity);
            circleShapeComponent.radius = vo.circle.radius;
        }

        if (vo.physics != null) {
            PhysicsBodyComponent physicsBodyComponent = physicsBodyCM.create(entity);
            physicsBodyComponent.allowSleep = vo.physics.allowSleep;
            physicsBodyComponent.sensor = vo.physics.sensor;
            physicsBodyComponent.fineBoundBox = vo.physics.fineBoundBox;
            physicsBodyComponent.awake = vo.physics.awake;
            physicsBodyComponent.bodyType = vo.physics.bodyType;
            physicsBodyComponent.bullet = vo.physics.bullet;
            physicsBodyComponent.centerOfMass = vo.physics.centerOfMass;
            physicsBodyComponent.damping = vo.physics.damping;
            physicsBodyComponent.density = vo.physics.density;
            physicsBodyComponent.friction = vo.physics.friction;
            physicsBodyComponent.gravityScale = vo.physics.gravityScale;
            physicsBodyComponent.mass = vo.physics.mass;
            physicsBodyComponent.restitution = vo.physics.restitution;
            physicsBodyComponent.rotationalInertia = vo.physics.rotationalInertia;
            physicsBodyComponent.angularDamping = vo.physics.angularDamping;
            physicsBodyComponent.fixedRotation = vo.physics.fixedRotation;
            physicsBodyComponent.height = vo.physics.height;
            physicsBodyComponent.shapeType = vo.physics.shapeType;
        }

        if (vo.sensor != null) {
            SensorComponent sensorComponent = sensorCM.create(entity);
            sensorComponent.bottom = vo.sensor.bottom;
            sensorComponent.left = vo.sensor.left;
            sensorComponent.right = vo.sensor.right;
            sensorComponent.top = vo.sensor.top;

            sensorComponent.bottomSpanPercent = vo.sensor.bottomSpanPercent;
            sensorComponent.leftSpanPercent = vo.sensor.leftSpanPercent;
            sensorComponent.rightSpanPercent = vo.sensor.rightSpanPercent;
            sensorComponent.topSpanPercent = vo.sensor.topSpanPercent;

            sensorComponent.bottomHeightPercent = vo.sensor.bottomHeightPercent;
            sensorComponent.leftWidthPercent = vo.sensor.leftWidthPercent;
            sensorComponent.rightWidthPercent = vo.sensor.rightWidthPercent;
            sensorComponent.topHeightPercent = vo.sensor.topHeightPercent;
        }

        if (vo.light != null) {
            LightBodyComponent lightBodyComponent = lightBodyCM.create(entity);
            lightBodyComponent.rays = vo.light.rays;
            lightBodyComponent.color = vo.light.color;
            lightBodyComponent.distance = vo.light.distance;
            lightBodyComponent.intensity = vo.light.intensity;
            lightBodyComponent.rayDirection = vo.light.rayDirection;
            lightBodyComponent.softnessLength = vo.light.softnessLength;
            lightBodyComponent.height = vo.light.height;
            lightBodyComponent.falloff.set(vo.light.falloff);
            lightBodyComponent.isXRay = vo.light.isXRay;
            lightBodyComponent.isStatic = vo.light.isStatic;
            lightBodyComponent.isSoft = vo.light.isSoft;
            lightBodyComponent.isActive = vo.light.isActive;
        }

        if (vo.shader.shaderName != null && !vo.shader.shaderName.isEmpty()) {
            ShaderComponent shaderComponent = shaderCM.create(entity);
            shaderComponent.shaderName = vo.shader.shaderName;
            shaderComponent.customUniforms.putAll(vo.shader.shaderUniforms);
            shaderComponent.renderingLayer = vo.renderingLayer;
        }
    }

    public abstract void initializeSpecialComponentsFromVO(int entity, MainItemVO vo);

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
