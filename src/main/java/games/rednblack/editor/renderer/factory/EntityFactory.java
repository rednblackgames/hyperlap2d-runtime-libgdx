package games.rednblack.editor.renderer.factory;

import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.AsyncTask;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.data.CompositeItemVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.factory.component.*;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.systems.strategy.HyperLap2dInvocationStrategy;
import games.rednblack.editor.renderer.utils.AsyncEntityFactoryCallback;
import games.rednblack.editor.renderer.utils.HyperJson;

public class EntityFactory {
    private static final char[] idSubset = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] idBuffer = new char[8];

    public static final int UNKNOWN_TYPE = -1;
    public static final int COMPOSITE_TYPE = 1;
    public static final int COLOR_PRIMITIVE = 2;
    public static final int IMAGE_TYPE = 3;
    public static final int LABEL_TYPE = 4;
    public static final int SPRITE_TYPE = 5;
    public static final int PARTICLE_TYPE = 6;
    public static final int LIGHT_TYPE = 7;
    public static final int NINE_PATCH = 8;

    protected ComponentMapper<MainItemComponent> mapper;
    protected ComponentMapper<NodeComponent> node;
    protected ComponentMapper<ParentNodeComponent> parent;
    protected ComponentMapper<ViewPortComponent> viewportCM;

    private final IntMap<ComponentFactory> factoriesMap = new IntMap<>();
    private final IntMap<ComponentFactory> externalFactories = new IntMap<>();
    private final ObjectMap<Class<? extends MainItemVO>, ComponentFactory> factoriesVOMap = new ObjectMap<>();
    private final IntMap<Class<? extends MainItemVO>> voMap = new IntMap<>();

    private final ObjectMap<String, EntityTransmuter> tagTransmuter = new ObjectMap<>();

    private final ObjectIntMap<String> entities = new ObjectIntMap<>();

    public RayHandler rayHandler;
    public World world;
    public IResourceRetriever rm = null;
    public com.artemis.World engine;

    private final AsyncExecutor asyncExecutor = new AsyncExecutor(1);

    /**
     * Do call injectDependencies manually when using this constructor!
     */
    public EntityFactory() {
    }

    public void injectExternalItemType(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        this.engine = engine;
        this.engine.inject(this);
        this.rayHandler = rayHandler;
        this.world = world;
        this.rm = rm;

        factoriesMap.put(COMPOSITE_TYPE, new CompositeComponentFactory(engine, rayHandler, world, rm));
        factoriesMap.put(LIGHT_TYPE, new LightComponentFactory(engine, rayHandler, world, rm));
        factoriesMap.put(PARTICLE_TYPE, new ParticleEffectComponentFactory(engine, rayHandler, world, rm));
        factoriesMap.put(IMAGE_TYPE, new SimpleImageComponentFactory(engine, rayHandler, world, rm));
        factoriesMap.put(SPRITE_TYPE, new SpriteComponentFactory(engine, rayHandler, world, rm));
        factoriesMap.put(LABEL_TYPE, new LabelComponentFactory(engine, rayHandler, world, rm));
        factoriesMap.put(NINE_PATCH, new NinePatchComponentFactory(engine, rayHandler, world, rm));
        factoriesMap.put(COLOR_PRIMITIVE, new ColorPrimitiveComponentFactory(engine, rayHandler, world, rm));

        for (ComponentFactory factory : externalFactories.values()) {
            factory.injectDependencies(engine, rayHandler, world, rm);
        }
        factoriesMap.putAll(externalFactories);

        for (ComponentFactory factory : factoriesMap.values()) {
            Class<? extends MainItemVO> voType = factory.getVOType();
            factoriesVOMap.put(voType, factory);
            voMap.put(factory.getEntityType(), voType);
        }
    }

    public void buildTagTransmuters(ObjectMap<String, ObjectSet<Class<? extends Component>>> tags) {
        for (String tag : tags.keys()) {
            EntityTransmuterFactory factory = new EntityTransmuterFactory(engine);
            ObjectSet<Class<? extends Component>> components = tags.get(tag);
            for (Class<? extends Component> component : components) {
                factory.add(component);
            }
            tagTransmuter.put(tag, factory.build());
        }
    }

    public void addExternalFactory(IExternalItemType itemType) {
        if (itemType.getComponentFactory() != null)
            externalFactories.put(itemType.getTypeId(), itemType.getComponentFactory());
    }

    public int createEntity(int root, int entityType, ComponentFactory.InitialData initialData) {
        int entity = factoriesMap.get(entityType).createEntity(root, initialData);
        postProcessEntity(entity);
        return entity;
    }

    public void loadEntities(int root, IntBag entities) {
        factoriesMap.get(COMPOSITE_TYPE).adjustNodeHierarchy(root, entities.get(0));
        initEntities(entities);
    }

    public void initEntities(IntBag entities) {
        for (int i = 0; i < entities.size(); i++) {
            int entity = entities.get(i);

            factoriesMap.get(mapper.get(entity).entityType).initialize(entity);

            postProcessEntity(entity);
        }
    }

    public <T extends MainItemVO> int createEntity(int root, T vo) {
        int entity = factoriesVOMap.get(vo.getClass()).createEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createRootEntity(CompositeItemVO vo, Viewport viewport, int ppwu) {
        vo.automaticResize = false;

        int entity = createEntity(-1, vo);

        ViewPortComponent viewPortComponent = viewportCM.create(entity);
        viewPortComponent.viewPort = viewport;
        viewPortComponent.pixelsPerWU = ppwu;
        viewPortComponent.viewPort.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        postProcessEntity(entity);
        return entity;
    }

    public void createEntitiesAsync(final int root, final int entityType, final Array<ComponentFactory.InitialData> initialData, final AsyncEntityFactoryCallback callback) {
        AsyncTask<Void> task = new AsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                for (int i = 0; i < initialData.size; i++) {
                    ComponentFactory.InitialData data = initialData.get(i);
                    synchronized (HyperLap2dInvocationStrategy.updateEntities) {
                        int e = createEntity(root, entityType, data);
                        if (callback != null) callback.onEntityCreated(e, data);
                    }
                }
                return null;
            }
        };
        asyncExecutor.submit(task);
    }

    public void initAllChildrenAsync(final int root, final CompositeItemVO vo) {
        AsyncTask<Void> task = new AsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                createAllChildrenAsync(root, vo);
                return null;
            }
        };
        asyncExecutor.submit(task);
    }

    private void createAllChildrenAsync(int root, CompositeItemVO vo) {
        for (String key : new ObjectMap.Keys<>(vo.content)) {
            if (key.equals(CompositeItemVO.class.getName())) continue;

            Array<MainItemVO> vos = vo.content.get(key);
            for (MainItemVO mainItemVO : new Array.ArrayIterator<>(vos, true)) {
                synchronized (HyperLap2dInvocationStrategy.updateEntities) {
                    createEntity(root, mainItemVO);
                }
            }
        }

        Array<MainItemVO> compositeVOs = vo.content.get(CompositeItemVO.class.getName());
        if (compositeVOs != null) {
            for (MainItemVO mainItemVO : new Array.ArrayIterator<>(compositeVOs, true)) {
                CompositeItemVO compositeItemVO = (CompositeItemVO) mainItemVO;
                int composite = -1;
                synchronized (HyperLap2dInvocationStrategy.updateEntities) {
                    composite = createEntity(root, compositeItemVO);
                }
                createAllChildrenAsync(composite, compositeItemVO);
            }
        }
    }

    public void initAllChildren(int root, CompositeItemVO vo) {
        for (String key : vo.content.keys()) {
            if (key.equals(CompositeItemVO.class.getName())) continue;

            Array<MainItemVO> vos = vo.content.get(key);
            for (MainItemVO mainItemVO : vos) {
                createEntity(root, mainItemVO);
            }
        }

        Array<MainItemVO> compositeVOs = vo.content.get(CompositeItemVO.class.getName());
        if (compositeVOs != null) {
            for (MainItemVO mainItemVO : compositeVOs) {
                CompositeItemVO compositeItemVO = (CompositeItemVO) mainItemVO;
                int composite = createEntity(root, compositeItemVO);
                initAllChildren(composite, compositeItemVO);
            }
        }
    }

    public void postProcessEntity(int entity) {
        MainItemComponent mainItemComponent = mapper.get(entity);

        if (mainItemComponent.uniqueId == null) mainItemComponent.uniqueId = generateRandomId(idBuffer);
        entities.put(mainItemComponent.uniqueId, entity);

        for (String tag : mainItemComponent.tags) {
            EntityTransmuter transmuter = tagTransmuter.get(tag);
            if (transmuter != null) transmuter.transmute(entity);
        }
    }

    private String generateRandomId(char[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            int index = MathUtils.random.nextInt(idSubset.length);
            buffer[i] = idSubset[index];
        }
        return new String(buffer);
    }

    public int getEntityByUniqueId(String id) {
        return entities.get(id, -1);
    }

    public void removeEntity(String id) {
        entities.remove(id, -1);
    }

    public void clean() {
        entities.clear();
    }

    public MainItemVO instantiateEmptyVO(int entityType) throws ReflectionException {
        return ClassReflection.newInstance(voMap.get(entityType));
    }

    public MainItemVO instantiateVOFromJson(String json, int entityType) {
        return HyperJson.getJson().fromJson(voMap.get(entityType), json);
    }

    public void dispose() {
        asyncExecutor.dispose();
    }
}
