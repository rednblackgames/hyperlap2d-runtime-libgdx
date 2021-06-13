package games.rednblack.editor.renderer;

import com.artemis.*;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.box2dLight.DirectionalLight;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.box2dLight.RayHandlerOptions;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.data.*;
import games.rednblack.editor.renderer.factory.ActionFactory;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.resources.ResourceManager;
import games.rednblack.editor.renderer.scripts.IScript;
import games.rednblack.editor.renderer.systems.*;
import games.rednblack.editor.renderer.systems.action.ActionSystem;
import games.rednblack.editor.renderer.systems.action.Actions;
import games.rednblack.editor.renderer.systems.action.data.ActionData;
import games.rednblack.editor.renderer.systems.render.FrameBufferManager;
import games.rednblack.editor.renderer.systems.render.HyperLap2dRenderer;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.CpuPolygonSpriteBatch;
import games.rednblack.editor.renderer.utils.DefaultShaders;

/**
 * SceneLoader is important part of runtime that utilizes provided
 * IResourceRetriever (or creates default one shipped with runtime) in order to
 * load entire scene data into viewable actors provides the functionality to get
 * root actor of scene and load scenes.
 */
public class SceneLoader {
    public static final int BATCH_VERTICES_SIZE = 2000;

    private String curResolution = "orig";
    private SceneVO sceneVO;
    private IResourceRetriever rm = null;

    // It will be instantiated after config is completed
    private com.artemis.World engine = null;

    // This is the config's builder
    private WorldConfigurationBuilder config;

    private RayHandler rayHandler;
    private World world;
    private int rootEntity;
    private DirectionalLight sceneDirectionalLight;

    private EntityFactory entityFactory;
    private ActionFactory actionFactory;

    private int pixelsPerWU = 1;

    private HyperLap2dRenderer renderer;

    public SceneLoader(World world, RayHandler rayHandler, boolean cullingEnabled, int entityPoolInitialSize, int entityPoolMaxSize, int componentPoolInitialSize, int componentPoolMaxSize) {
        this.world = world;
        this.rayHandler = rayHandler;

        ResourceManager rm = new ResourceManager();
        rm.initAllResources();
        this.rm = rm;

        initSceneLoader(cullingEnabled, entityPoolInitialSize, entityPoolMaxSize, componentPoolInitialSize, componentPoolMaxSize);
    }

    public SceneLoader(IResourceRetriever rm, World world, RayHandler rayHandler, boolean cullingEnabled, int entityPoolInitialSize, int entityPoolMaxSize, int componentPoolInitialSize, int componentPoolMaxSize) {
        this.world = world;
        this.rayHandler = rayHandler;
        this.rm = rm;

        initSceneLoader(cullingEnabled, entityPoolInitialSize, entityPoolMaxSize, componentPoolInitialSize, componentPoolMaxSize);
    }

    public SceneLoader() {
        this(null, null, true, 10, 100, 10, 100);
    }

    public SceneLoader(IResourceRetriever rm) {
        this(rm, null, null, true, 10, 100, 10, 100);
    }

    /**
     * this method is called when rm has loaded all data
     */
    private void initSceneLoader(boolean cullingEnabled, int entityPoolInitialSize, int entityPoolMaxSize, int componentPoolInitialSize, int componentPoolMaxSize) {

        if (world == null) {
            world = new World(new Vector2(0, -10), true);
        }

        this.config = new WorldConfigurationBuilder();

        if (rayHandler == null) {
            RayHandlerOptions rayHandlerOptions = new RayHandlerOptions();
            rayHandlerOptions.setGammaCorrection(false);
            rayHandlerOptions.setDiffuse(true);

            rayHandler = new RayHandler(world, rayHandlerOptions);
            rayHandler.setAmbientLight(1f, 1f, 1f, 1f);
            rayHandler.setCulling(true);
            rayHandler.setBlur(true);
            rayHandler.setBlurNum(3);
            rayHandler.setShadows(true);
        }

        addSystems(cullingEnabled);

        // TODO: Currently no idea about what to do with all these parameters, and how to add them to Artemis World
//        this.engine = new PooledEngine(entityPoolInitialSize, entityPoolMaxSize, componentPoolInitialSize, componentPoolMaxSize);
        this.engine = new com.artemis.World(config.build());

        ComponentRetriever.initialize(engine);
        addEntityRemoveListener();

        entityFactory = new EntityFactory(engine, rayHandler, world, rm);
    }

    public void setResolution(String resolutionName) {
        ResolutionEntryVO resolution = getRm().getProjectVO().getResolution(resolutionName);
        if (resolution != null) {
            curResolution = resolutionName;
        }
    }

    // TODO: Do i give in config or engine to injectDependencies
    public void injectExternalItemType(IExternalItemType itemType) {
        itemType.injectDependencies(engine, rayHandler, world, rm);
        itemType.injectMappers();
        entityFactory.addExternalFactory(itemType);

        addSystem(itemType.getSystem());

        renderer.addDrawableType(itemType);
    }

    // TODO: It's a nasty fix; I know: don't even know, if it'll work... Gotta test it and find a fix!
    // TODO: Have some way to add in new Systems
    public void addSystem(BaseSystem system) {
    }

    private void addSystems(boolean cullingEnabled) {
        ParticleSystem particleSystem = new ParticleSystem();
        LightSystem lightSystem = new LightSystem(rayHandler);
        SpriteAnimationSystem animationSystem = new SpriteAnimationSystem();
        LayerSystem layerSystem = new LayerSystem();
        PhysicsSystem physicsSystem = new PhysicsSystem(world);
        CompositeSystem compositeSystem = new CompositeSystem();
        LabelSystem labelSystem = new LabelSystem();
        TypingLabelSystem typingLabelSystem = new TypingLabelSystem();
        ScriptSystem scriptSystem = new ScriptSystem();
        ActionSystem actionSystem = new ActionSystem();
        BoundingBoxSystem boundingBoxSystem = new BoundingBoxSystem();
        CullingSystem cullingSystem = new CullingSystem();
        renderer = new HyperLap2dRenderer(new CpuPolygonSpriteBatch(BATCH_VERTICES_SIZE, createDefaultShader()));
        renderer.setRayHandler(rayHandler);

        config.with(animationSystem);
        config.with(particleSystem);
        config.with(layerSystem);
        config.with(physicsSystem);
        config.with(lightSystem);
        config.with(typingLabelSystem);
        config.with(compositeSystem);
        config.with(labelSystem);
        config.with(scriptSystem);
        config.with(actionSystem);

        if (cullingEnabled) {
            config.with(boundingBoxSystem);
            config.with(cullingSystem);
        }

        config.with(renderer);

        // additional
        config.with(new ButtonSystem());
    }

    private void addEntityRemoveListener() {

        engine.getAspectSubscriptionManager()
                .get(Aspect.all())
                .addSubscriptionListener(new EntitySubscription.SubscriptionListener() {

                    @Override
                    public void inserted(IntBag entities) {
                        for (int i = 0, s = entities.size(); i < s; i++) {
                            entityAdded(entities.get(i));
                        }
                    }

                    @Override
                    public void removed(IntBag entities) {
                        for (int i = 0, s = entities.size(); i < s; i++) {
                            entityRemoved(entities.get(i));
                        }
                    }

                    public void entityAdded(int entity) {
                        // call init for a system
                        ScriptComponent scriptComponent = ComponentRetriever.get(entity, ScriptComponent.class);
                        if (scriptComponent != null) {
                            for (IScript script : scriptComponent.scripts) {
                                script.init(entity);
                            }
                        }
                    }

                    public void entityRemoved(int entity) {
                        ParentNodeComponent parentComponent = ComponentRetriever.get(entity, ParentNodeComponent.class);

                        if (parentComponent == null) {
                            return;
                        }

                        int parentEntity = parentComponent.parentEntity;
                        NodeComponent parentNodeComponent = ComponentRetriever.get(parentEntity, NodeComponent.class);
                        if (parentNodeComponent != null)
                            parentNodeComponent.removeChild(entity);

                        // check if composite and remove all children
                        NodeComponent nodeComponent = ComponentRetriever.get(entity, NodeComponent.class);
                        if (nodeComponent != null) {
                            // it is composite
                            for (int node : nodeComponent.children) {
                                if (engine.getEntity(node).isActive())
                                    engine.delete(node);
                            }
                        }

                        // TODO: remove this comment
                        /*if (nodeComponent != null) {
                            // it is composite
                            for (int node : nodeComponent.children) {
                                if (!node.isRemoving() && !node.isScheduledForRemoval())
                                    engine.removeEntity(node);
                            }
                        }*/

                        //check for physics
                        PhysicsBodyComponent physicsBodyComponent = ComponentRetriever.get(entity, PhysicsBodyComponent.class);
                        if (physicsBodyComponent != null && physicsBodyComponent.body != null) {
                            world.destroyBody(physicsBodyComponent.body);
                            physicsBodyComponent.body = null;
                        }

                        // check if it is light
                        LightObjectComponent lightObjectComponent = ComponentRetriever.get(entity, LightObjectComponent.class);
                        if (lightObjectComponent != null) {
                            lightObjectComponent.lightObject.remove(true);
                        }

                        LightBodyComponent lightBodyComponent = ComponentRetriever.get(entity, LightBodyComponent.class);
                        if (lightBodyComponent != null && lightBodyComponent.lightObject != null) {
                            lightBodyComponent.lightObject.remove(true);
                        }

                        ScriptComponent scriptComponent = ComponentRetriever.get(entity, ScriptComponent.class);
                        if (scriptComponent != null) {
                            for (IScript script : scriptComponent.scripts) {
                                script.dispose();
                            }
                        }

                        renderer.removeSpecialEntity(entity);
                    }
                });
    }

    public SceneVO loadScene(String sceneName, Viewport viewport) {
        return loadScene(sceneName, viewport, false);
    }

    public SceneVO loadScene(String sceneName) {
        return loadScene(sceneName, false);
    }

    public SceneVO loadScene(String sceneName, boolean customLight) {
        ProjectInfoVO projectVO = rm.getProjectVO();
        Viewport viewport = new ScalingViewport(Scaling.stretch, (float) projectVO.originalResolution.width / pixelsPerWU, (float) projectVO.originalResolution.height / pixelsPerWU, new OrthographicCamera());
        return loadScene(sceneName, viewport, customLight);
    }

    public SceneVO loadScene(String sceneName, Viewport viewport, boolean customLight) {
        // this has to be done differently.
        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all())
                .getEntities();

        int[] ids = entities.getData();
        for (int i = 0, s = entities.size(); s > i; i++) {
            engine.delete(ids[i]);
        }

        entityFactory.clean();
        //Update the engine to ensure that all pending operations are completed!!
        engine.setDelta(Gdx.graphics.getDeltaTime());
        engine.process();

        pixelsPerWU = rm.getProjectVO().pixelToWorld;
        renderer.setPixelsPerWU(pixelsPerWU);

        sceneVO = rm.getSceneVO(sceneName);
        world.setGravity(new Vector2(sceneVO.physicsPropertiesVO.gravityX, sceneVO.physicsPropertiesVO.gravityY));
        PhysicsSystem physicsSystem = engine.getSystem(PhysicsSystem.class);
        if (physicsSystem != null)
            physicsSystem.setPhysicsOn(sceneVO.physicsPropertiesVO.enabled);

        if (sceneVO.composite == null) {
            sceneVO.composite = new CompositeVO();
        }
        rootEntity = entityFactory.createRootEntity(sceneVO.composite, viewport);

        if (sceneVO.composite != null) {
            entityFactory.initAllChildren(engine, rootEntity, sceneVO.composite);
        }
        if (!customLight) {
            setAmbientInfo(sceneVO);
        }

        actionFactory = new ActionFactory(rm.getProjectVO().libraryActions);

        return sceneVO;
    }

    public SceneVO getSceneVO() {
        return sceneVO;
    }

    public int loadFromLibrary(String libraryName) {
        ProjectInfoVO projectInfoVO = getRm().getProjectVO();
        CompositeItemVO compositeItemVO = projectInfoVO.libraryItems.get(libraryName);

        if (compositeItemVO != null) {
            int ent = engine.create();
            entityFactory.initializeEntity(-1, ent, compositeItemVO);
            return ent;
        }

        return -1;
    }

    public CompositeItemVO loadVoFromLibrary(String libraryName) {
        ProjectInfoVO projectInfoVO = getRm().getProjectVO();
        CompositeItemVO compositeItemVO = projectInfoVO.libraryItems.get(libraryName);

        return compositeItemVO;
    }

    public ActionData loadActionFromLibrary(String actionName) {
        return actionFactory.loadFromLibrary(actionName);
    }

    public ActionFactory getActionFactory() {
        return actionFactory;
    }

    public void addComponentByTagName(String tagName, Class<? extends Component> componentClass) {
        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all())
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);

            MainItemComponent mainItemComponent = ComponentRetriever.get(id, MainItemComponent.class);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    engine.edit(id).create(componentClass);
                }
            }
        }
    }

    /*
     * Add an actions from library actions for any entity with specified tag
     *
     */
    public void addActionByTagName(String tagName, String action) {
        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all())
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);
            MainItemComponent mainItemComponent = ComponentRetriever.get(id, MainItemComponent.class);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    Actions.addAction(engine, id, loadActionFromLibrary(action));
                }
            }
        }
    }

    /*
     * Add an actions for any entity with specified tag
     *
     */
    public void addActionByTagName(String tagName, ActionData action) {
        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all())
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);
            MainItemComponent mainItemComponent = ComponentRetriever.get(id, MainItemComponent.class);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    Actions.addAction(engine, id, action);
                }
            }
        }
    }

    /**
     * Sets ambient light to the one specified in scene from editor
     *
     * @param vo - Scene data file to invalidate
     */

    public void setAmbientInfo(SceneVO vo) {
        setAmbientInfo(vo, false);
    }

    public void setAmbientInfo(SceneVO vo, boolean override) {
        if (sceneDirectionalLight != null) {
            sceneDirectionalLight.remove();
            sceneDirectionalLight = null;
        }
        boolean isDiffuse = !vo.lightsPropertiesVO.lightType.equals("BRIGHT");
        renderer.setUseLights(vo.lightsPropertiesVO.enabled);

        if (override || !vo.lightsPropertiesVO.enabled) {
            isDiffuse = true;
            if (isDiffuse != RayHandler.isDiffuseLight()) {
                rayHandler.setDiffuseLight(isDiffuse);
            }
            rayHandler.setAmbientLight(1f, 1f, 1f, 1f);
            return;
        }

        if (isDiffuse != RayHandler.isDiffuseLight()) {
            rayHandler.setDiffuseLight(isDiffuse);
        }

        rayHandler.setPseudo3dLight(vo.lightsPropertiesVO.pseudo3d);

        if (vo.lightsPropertiesVO.ambientColor != null) {
            Color clr = new Color(vo.lightsPropertiesVO.ambientColor[0], vo.lightsPropertiesVO.ambientColor[1],
                    vo.lightsPropertiesVO.ambientColor[2], vo.lightsPropertiesVO.ambientColor[3]);

            if (vo.lightsPropertiesVO.lightType.equals("DIRECTIONAL")) {
                Color lightColor = new Color(vo.lightsPropertiesVO.directionalColor[0], vo.lightsPropertiesVO.directionalColor[1],
                        vo.lightsPropertiesVO.directionalColor[2], vo.lightsPropertiesVO.directionalColor[3]);
                sceneDirectionalLight = new DirectionalLight(rayHandler, vo.lightsPropertiesVO.directionalRays,
                        lightColor, vo.lightsPropertiesVO.directionalDegree);
                sceneDirectionalLight.setHeight(vo.lightsPropertiesVO.directionalHeight);
            }
            rayHandler.setAmbientLight(clr);
            rayHandler.setBlurNum(vo.lightsPropertiesVO.blurNum);
        }
    }

    public void resize(int width, int height) {
        rayHandler.resizeFBO(width, height);
        renderer.resize(width, height);
    }

    public void dispose() {
        renderer.dispose();
        rayHandler.dispose();
        world.dispose();
    }

    public EntityFactory getEntityFactory() {
        return entityFactory;
    }

    public IResourceRetriever getRm() {
        return rm;
    }

    public com.artemis.World getEngine() {
        return engine;
    }

    public RayHandler getRayHandler() {
        return rayHandler;
    }

    public World getWorld() {
        return world;
    }

    public int getPixelsPerWU() {
        return pixelsPerWU;
    }

    public int getRoot() {
        return rootEntity;
    }

    public Entity getRootEntity() {
        return engine.getEntity(rootEntity);
    }

    /**
     * Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.
     */
    static public ShaderProgram createDefaultShader() {
        ShaderProgram shader = new ShaderProgram(DefaultShaders.DEFAULT_VERTEX_SHADER, DefaultShaders.DEFAULT_FRAGMENT_SHADER);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }

    public Batch getBatch() {
        return renderer.getBatch();
    }

    public FrameBufferManager getFrameBufferManager() {
        return renderer.getFrameBufferManager();
    }
}
