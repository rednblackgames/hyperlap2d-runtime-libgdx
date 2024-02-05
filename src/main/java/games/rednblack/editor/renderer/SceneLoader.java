package games.rednblack.editor.renderer;

import com.artemis.*;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.box2dLight.DirectionalLight;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
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
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;
import games.rednblack.editor.renderer.systems.PhysicsSystem;
import games.rednblack.editor.renderer.systems.action.Actions;
import games.rednblack.editor.renderer.systems.action.data.ActionData;
import games.rednblack.editor.renderer.systems.render.FrameBufferManager;
import games.rednblack.editor.renderer.systems.render.HyperLap2dRenderer;
import games.rednblack.editor.renderer.systems.strategy.HyperLap2dInvocationStrategy;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.DefaultShaders;
import games.rednblack.editor.renderer.utils.SceneLoaderFieldResolver;

/**
 * SceneLoader is important part of runtime that utilizes provided
 * IResourceRetriever (or creates default one shipped with runtime) in order to
 * load entire scene data into viewable actors provides the functionality to get
 * root actor of scene and load scenes.
 * <p>
 * Usage note:
 * First, create an instance with suitable parameters.
 * Second, inject all the external types using injectExternalItemType
 * Third, create the engine, initialise mappers and external types by calling createEngine
 * Fourth, load a scene using loadScene
 */
public class SceneLoader {
    // Initialised when a SceneLoader is instantiated
    private String curResolution = "orig";
    private World world;
    private RayHandler rayHandler;
    private IResourceRetriever rm;
    private HyperLap2dRenderer renderer;
    private EntityFactory entityFactory;
    private final IntMap<IExternalItemType> externalItemTypes = new IntMap<>();

    // Initialised when injectExternalItemType is called

    // Initialised when createEngine is called
    private com.artemis.World engine = null;
    private ComponentMapper<LightBodyComponent> lightBodyCM;
    private ComponentMapper<LightObjectComponent> lightObjectCM;
    private ComponentMapper<MainItemComponent> mainItemCM;
    private ComponentMapper<NodeComponent> nodeCM;
    private ComponentMapper<ParentNodeComponent> parentNodeCM;
    private ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    private ComponentMapper<ScriptComponent> scriptCM;

    // Initialised when loadScene is called
    private int pixelsPerWU = 1;
    private SceneVO sceneVO;
    private int rootEntity;
    private DirectionalLight sceneDirectionalLight;
    private ActionFactory actionFactory;

    public SceneLoader(SceneConfiguration configuration) {

        this.world = configuration.getWorld();
        this.rayHandler = configuration.getRayHandler();

        this.rm = configuration.getResourceRetriever();

        initSceneLoader(configuration);
    }

    /**
     * this method is called when rm has loaded all data
     */
    private void initSceneLoader(SceneConfiguration configuration) {
        entityFactory = new EntityFactory();

        renderer = configuration.getSystem(HyperLap2dRenderer.class);

        WorldConfigurationBuilder config = new WorldConfigurationBuilder();

        for (SceneConfiguration.SystemData<?> data : configuration.getSystems()) {
            config.with(data.priority, data.system);
        }
        config.register(configuration.getInvocationStrategy());
        config.register(new SceneLoaderFieldResolver(this));
        WorldConfiguration build = config.build();
        build.expectedEntityCount(configuration.getExpectedEntityCount());
        build.setAlwaysDelayComponentRemoval(true);

        this.engine = new com.artemis.World(build);

        engine.inject(this);
        ComponentRetriever.initialize(engine);

        addEntityRemoveListener();

        if (configuration.getExternalItemTypes() != null) {
            for (IExternalItemType itemType : configuration.getExternalItemTypes()) {
                itemType.injectMappers();
                entityFactory.addExternalFactory(itemType);
                renderer.addDrawableType(itemType);
                externalItemTypes.put(itemType.getTypeId(), itemType);
            }
        }
        renderer.injectMappers(engine);

        entityFactory.injectExternalItemType(engine, rayHandler, world, rm);
        entityFactory.buildTagTransmuters(configuration.getTagTransmuters());
    }

    public void setResolution(String resolutionName) {
        ResolutionEntryVO resolution = getRm().getProjectVO().getResolution(resolutionName);
        if (resolution != null) {
            curResolution = resolutionName;
        }
    }

    private void addEntityRemoveListener() {
        engine.getAspectSubscriptionManager()
                .get(Aspect.all())
                .addSubscriptionListener(new EntitySubscription.SubscriptionListener() {

                    @Override
                    public void inserted(IntBag entities) {
                    }

                    @Override
                    public void removed(IntBag entities) {
                        for (int i = 0; i < entities.size(); i++) {
                            int entity = entities.get(i);
                            ParentNodeComponent parentComponent = parentNodeCM.get(entity);

                            if (parentComponent == null) {
                                continue;
                            }

                            int parentEntity = parentComponent.parentEntity;
                            if (parentEntity != -1) {
                                NodeComponent parentNodeComponent = nodeCM.get(parentEntity);
                                if (parentNodeComponent != null)
                                    parentNodeComponent.removeChild(entity);
                            }

                            // check if composite and remove all children
                            NodeComponent nodeComponent = nodeCM.get(entity);
                            if (nodeComponent != null) {
                                // it is composite
                                for (int node : nodeComponent.children) {
                                    if (engine.getEntityManager().isActive(node))
                                        engine.delete(node);
                                }
                            }

                            renderer.removeSpecialEntity(entity);
                            MainItemComponent mainItemComponent = mainItemCM.get(entity);
                            entityFactory.removeEntity(mainItemComponent.uniqueId);
                        }
                    }
                });
    }

    public SceneVO loadScene(String sceneName) {
        return loadScene(sceneName, false);
    }

    public SceneVO loadScene(String sceneName, boolean customLight) {
        ProjectInfoVO projectVO = rm.getProjectVO();
        Viewport viewport = new ScalingViewport(Scaling.stretch, (float) projectVO.originalResolution.width / pixelsPerWU, (float) projectVO.originalResolution.height / pixelsPerWU, new OrthographicCamera());
        return loadScene(sceneName, viewport, customLight);
    }

    public SceneVO loadScene(String sceneName, Viewport viewport) {
        return loadScene(sceneName, viewport, false);
    }

    public SceneVO loadScene(String sceneName, Viewport viewport, boolean customLight) {
        return loadScene(rm.getSceneVO(sceneName), viewport, customLight);
    }

    public SceneVO loadScene(SceneVO vo, Viewport viewport, boolean customLight) {
        assert engine != null : "You need to first create an engine by calling createEngine";

        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all())
                .getEntities();

        int[] ids = entities.getData();
        for (int i = 0, s = entities.size(); s > i; i++) {
            engine.delete(ids[i]);
        }

        entityFactory.clean();
        //Update the engine to ensure that all pending operations are completed!!
        engine.setDelta(0);
        engine.process();

        pixelsPerWU = rm.getProjectVO().pixelToWorld;

        sceneVO = vo;
        world.setGravity(new Vector2(sceneVO.physicsPropertiesVO.gravityX, sceneVO.physicsPropertiesVO.gravityY));
        PhysicsSystem physicsSystem = engine.getSystem(PhysicsSystem.class);
        if (physicsSystem != null)
            physicsSystem.setPhysicsOn(sceneVO.physicsPropertiesVO.enabled);

        if (sceneVO.composite == null) {
            sceneVO.composite = new CompositeItemVO();
        }
        rootEntity = entityFactory.createRootEntity(sceneVO.composite, viewport, pixelsPerWU);

        if (sceneVO.composite != null) {
            entityFactory.initAllChildren(rootEntity, sceneVO.composite);
        }
        if (!customLight) {
            setAmbientInfo(sceneVO);
        }

        actionFactory = new ActionFactory(rm.getProjectVO().libraryActions);

        SystemInvocationStrategy strategy = engine.getInvocationStrategy();
        if (strategy instanceof HyperLap2dInvocationStrategy)
            ((HyperLap2dInvocationStrategy) strategy).updateEntitySateSync();

        return sceneVO;
    }

    public SceneVO getSceneVO() {
        return sceneVO;
    }

    public int loadFromLibrary(String libraryName, String layerName, float x, float y) {
        ProjectInfoVO projectInfoVO = getRm().getProjectVO();
        CompositeItemVO compositeItemVO = projectInfoVO.libraryItems.get(libraryName);

        if (compositeItemVO != null) {
            compositeItemVO.layerName = layerName;
            compositeItemVO.x = x;
            compositeItemVO.y = y;

            int compositeEntity = entityFactory.createEntity(getRoot(), compositeItemVO);
            getEntityFactory().initAllChildren(compositeEntity, compositeItemVO);
            return compositeEntity;
        }

        return -1;
    }

    public int loadFromLibrary(String libraryName, String layerName, float x, float y, int parent) {
        ProjectInfoVO projectInfoVO = getRm().getProjectVO();
        CompositeItemVO compositeItemVO = projectInfoVO.libraryItems.get(libraryName);

        if (compositeItemVO != null) {
            compositeItemVO.layerName = layerName;
            compositeItemVO.x = x;
            compositeItemVO.y = y;

            int compositeEntity = getEntityFactory().createEntity(parent, compositeItemVO);
            getEntityFactory().initAllChildren(compositeEntity, compositeItemVO);
            return compositeEntity;
        }

        return -1;
    }

    public CompositeItemVO loadVoFromLibrary(String libraryName) {
        ProjectInfoVO projectInfoVO = getRm().getProjectVO();
        return projectInfoVO.libraryItems.get(libraryName);
    }

    public ActionData loadActionFromLibrary(String actionName) {
        return actionFactory.loadFromLibrary(actionName);
    }

    public ActionFactory getActionFactory() {
        return actionFactory;
    }

    public void addComponentByTagName(String tagName, Class<? extends Component> componentClass) {
        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all(MainItemComponent.class))
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);

            MainItemComponent mainItemComponent = mainItemCM.get(id);
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
                .get(Aspect.all(MainItemComponent.class))
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);
            MainItemComponent mainItemComponent = mainItemCM.get(id);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    Actions.addAction(id, loadActionFromLibrary(action), engine);
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
                .get(Aspect.all(MainItemComponent.class))
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);
            MainItemComponent mainItemComponent = mainItemCM.get(id);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    Actions.addAction(id, action, engine);
                }
            }
        }
    }

    /**
     * Attach a script to the entity using {@link ScriptComponent},
     * Scripts will be automatically pooled and must extends {@link BasicScript}
     *
     * @param scriptClazz script class definition
     */
    public <T extends BasicScript> void addScriptByTagName(String tagName, Class<T> scriptClazz) {
        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all(MainItemComponent.class))
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);
            MainItemComponent mainItemComponent = mainItemCM.get(id);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    ScriptComponent component = scriptCM.get(id);
                    if(component == null) {
                        component = scriptCM.create(id);
                        component.engine = engine;
                    }
                    T script = component.addScript(scriptClazz);
                }
            }
        }
    }

    /**
     * Attach a script to the entity using {@link ScriptComponent}
     * @param script script instance
     */
    public void addScriptByTagName(String tagName, IScript script) {
        IntBag entities = engine.getAspectSubscriptionManager()
                .get(Aspect.all(MainItemComponent.class))
                .getEntities();

        for (int i = 0, s = entities.size(); s > i; i++) {
            int id = entities.get(i);
            MainItemComponent mainItemComponent = mainItemCM.get(id);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    ScriptComponent component = scriptCM.get(id);
                    if (component == null) {
                        component = scriptCM.create(id);
                        component.engine = engine;
                    }
                    component.addScript(script);
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
        renderer.setSceneShader(rm.getShaderProgram(vo.shaderVO.shaderName));

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
        entityFactory.dispose();
        Actions.dispose();
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

    public Batch getBatch() {
        return renderer.getBatch();
    }

    public FrameBufferManager getFrameBufferManager() {
        return renderer.getFrameBufferManager();
    }

    public IExternalItemType getExternalItemType(int type) {
        return externalItemTypes.get(type);
    }

    public HyperLap2dRenderer getRenderer() {
        return renderer;
    }
}
