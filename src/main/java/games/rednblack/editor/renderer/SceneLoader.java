package games.rednblack.editor.renderer;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
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
import games.rednblack.editor.renderer.components.*;
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

    private String curResolution = "orig";
    private SceneVO sceneVO;
    private IResourceRetriever rm = null;

    private PooledEngine engine = null;
    private RayHandler rayHandler;
    private World world;
    private Entity rootEntity;
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
        this.engine = new PooledEngine(entityPoolInitialSize, entityPoolMaxSize, componentPoolInitialSize, componentPoolMaxSize);

        if (world == null) {
            world = new World(new Vector2(0, -10), true);
        }

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
        entityFactory = new EntityFactory(engine, rayHandler, world, rm);
    }

    public void setResolution(String resolutionName) {
        ResolutionEntryVO resolution = getRm().getProjectVO().getResolution(resolutionName);
        if (resolution != null) {
            curResolution = resolutionName;
        }
    }

    public void injectExternalItemType(IExternalItemType itemType) {
        itemType.injectDependencies(engine, rayHandler, world, rm);
        itemType.injectMappers();
        entityFactory.addExternalFactory(itemType);
        engine.addSystem(itemType.getSystem());
        renderer.addDrawableType(itemType);
    }

    private void addSystems(boolean cullingEnabled) {
        ParticleSystem particleSystem = new ParticleSystem();
        LightSystem lightSystem = new LightSystem();
        lightSystem.setRayHandler(rayHandler);
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
        renderer = new HyperLap2dRenderer(new CpuPolygonSpriteBatch(2000, createDefaultShader()));
        renderer.setRayHandler(rayHandler);

        engine.addSystem(animationSystem);
        engine.addSystem(particleSystem);
        engine.addSystem(layerSystem);
        engine.addSystem(physicsSystem);
        engine.addSystem(lightSystem);
        engine.addSystem(typingLabelSystem);
        engine.addSystem(compositeSystem);
        engine.addSystem(labelSystem);
        engine.addSystem(scriptSystem);
        engine.addSystem(actionSystem);

        if (cullingEnabled) {
            engine.addSystem(boundingBoxSystem);
            engine.addSystem(cullingSystem);
        }

        engine.addSystem(renderer);

        // additional
        engine.addSystem(new ButtonSystem());

        addEntityRemoveListener();
    }

    private void addEntityRemoveListener() {
        engine.addEntityListener(new EntityListener() {
            @Override
            public void entityAdded(Entity entity) {
                // TODO: Gev knows what to do. (do this for all entities)

                // mae sure we assign correct z-index here
				/*
				ZindexComponent zindexComponent = ComponentRetriever.get(entity, ZindexComponent.class);
				ParentNodeComponent parentNodeComponent = ComponentRetriever.get(entity, ParentNodeComponent.class);
				if (parentNodeComponent != null) {
					NodeComponent nodeComponent = parentNodeComponent.parentEntity.getComponent(NodeComponent.class);
					zindexComponent.setZIndex(nodeComponent.children.size);
					zindexComponent.needReOrder = false;
				}*/

                // call init for a system
                ScriptComponent scriptComponent = entity.getComponent(ScriptComponent.class);
                if (scriptComponent != null) {
                    for (IScript script : scriptComponent.scripts) {
                        script.init(entity);
                    }
                }
            }

            @Override
            public void entityRemoved(Entity entity) {
                ParentNodeComponent parentComponent = ComponentRetriever.get(entity, ParentNodeComponent.class);

                if (parentComponent == null) {
                    return;
                }

                Entity parentEntity = parentComponent.parentEntity;
                NodeComponent parentNodeComponent = ComponentRetriever.get(parentEntity, NodeComponent.class);
                if (parentNodeComponent != null)
                    parentNodeComponent.removeChild(entity);

                // check if composite and remove all children
                NodeComponent nodeComponent = ComponentRetriever.get(entity, NodeComponent.class);
                if (nodeComponent != null) {
                    // it is composite
                    for (Entity node : nodeComponent.children) {
                        engine.removeEntity(node);
                    }
                }

                //check for physics
                PhysicsBodyComponent physicsBodyComponent = ComponentRetriever.get(entity, PhysicsBodyComponent.class);
                if (physicsBodyComponent != null && physicsBodyComponent.body != null) {
                    world.destroyBody(physicsBodyComponent.body);
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

                ScriptComponent scriptComponent = entity.getComponent(ScriptComponent.class);
                if (scriptComponent != null) {
                    for (IScript script : scriptComponent.scripts) {
                        script.dispose();
                    }
                }
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
        engine.removeAllEntities();
        entityFactory.clean();
        //Update the engine to ensure that all pending operations are completed!!
        engine.update(Gdx.graphics.getDeltaTime());

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
        engine.addEntity(rootEntity);

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

    public Entity loadFromLibrary(String libraryName) {
        ProjectInfoVO projectInfoVO = getRm().getProjectVO();
        CompositeItemVO compositeItemVO = projectInfoVO.libraryItems.get(libraryName);

        if (compositeItemVO != null) {
            return entityFactory.createEntity(null, compositeItemVO);
        }

        return null;
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
        ImmutableArray<Entity> entities = engine.getEntities();
        for (Entity entity : entities) {
            MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    entity.add(engine.createComponent(componentClass));
                }
            }
        }
    }

    /*
    * Add an actions from library actions for any entity with specified tag
    *
    */
    public void addActionByTagName(String tagName, String action) {
        ImmutableArray<Entity> entities = engine.getEntities();
        for (Entity entity : entities) {
            MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    Actions.addAction(engine, entity, loadActionFromLibrary(action));
                }
            }
        }
    }

    /*
     * Add an actions for any entity with specified tag
     *
     */
    public void addActionByTagName(String tagName, ActionData action) {
        ImmutableArray<Entity> entities = engine.getEntities();
        for (Entity entity : entities) {
            MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class);
            for (String tag : mainItemComponent.tags) {
                if (tag.equals(tagName)) {
                    Actions.addAction(engine, entity, action);
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

        if (vo.lightsPropertiesVO.ambientColor != null) {
            Color clr = new Color(vo.lightsPropertiesVO.ambientColor[0], vo.lightsPropertiesVO.ambientColor[1],
                    vo.lightsPropertiesVO.ambientColor[2], vo.lightsPropertiesVO.ambientColor[3]);

            if (vo.lightsPropertiesVO.lightType.equals("DIRECTIONAL")) {
                Color lightColor = new Color(vo.lightsPropertiesVO.directionalColor[0], vo.lightsPropertiesVO.directionalColor[1],
                        vo.lightsPropertiesVO.directionalColor[2], vo.lightsPropertiesVO.directionalColor[3]);
                sceneDirectionalLight = new DirectionalLight(rayHandler, vo.lightsPropertiesVO.directionalRays,
                        lightColor, vo.lightsPropertiesVO.directionalDegree);
            }
            rayHandler.setAmbientLight(clr);
            rayHandler.setBlurNum(vo.lightsPropertiesVO.blurNum);
        }
    }

    public void resize(int width, int height) {
        rayHandler.resizeFBO(width / 4, height / 4);
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

    public PooledEngine getEngine() {
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

    public Entity getRoot() {
        return rootEntity;
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
