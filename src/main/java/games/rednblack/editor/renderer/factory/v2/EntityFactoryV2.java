package games.rednblack.editor.renderer.factory.v2;

import com.artemis.ComponentMapper;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntMap;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

import java.util.ArrayList;
import java.util.Collections;

public class EntityFactoryV2 {
    public static final int UNKNOWN_TYPE = -1;
    public static final int COMPOSITE_TYPE = 1;
    public static final int COLOR_PRIMITIVE = 2;
    public static final int IMAGE_TYPE = 3;
    public static final int LABEL_TYPE = 4;
    public static final int SPRITE_TYPE = 5;
    public static final int PARTICLE_TYPE = 6;
    public static final int LIGHT_TYPE = 7;
    public static final int NINE_PATCH = 8;
    public static final int SPINE_TYPE = 9;
    public static final int TALOS_TYPE = 10;

    protected ComponentMapper<MainItemComponent> mapper;
    protected ComponentMapper<NodeComponent> node;
    protected ComponentMapper<ParentNodeComponent> parent;

    private final IntMap<ComponentFactoryV2> factoriesMap = new IntMap<>();
    private final IntMap<ComponentFactoryV2> externalFactories = new IntMap<>();

    // TODO: Do we still need it? Like, in Artemis all enties are already identified by a Unique ID
    private final IntIntMap entities;

    public RayHandler rayHandler;
    public World world;
    public IResourceRetriever rm = null;
    public com.artemis.World engine;

    /**
     * Do call injectDependencies manually when using this constructor!
     */
    public EntityFactoryV2(IntIntMap entities) {
        this.entities = entities;
    }

    public void injectExternalItemType(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        this.engine = engine;
        this.engine.inject(this);
        this.rayHandler = rayHandler;
        this.world = world;
        this.rm = rm;

        factoriesMap.put(COMPOSITE_TYPE, new CompositeComponentFactoryV2(engine, rayHandler, world, rm));
        factoriesMap.put(LIGHT_TYPE, new LightComponentFactoryV2(engine, rayHandler, world, rm));
        factoriesMap.put(PARTICLE_TYPE, new ParticleEffectComponentFactoryV2(engine, rayHandler, world, rm));
        factoriesMap.put(IMAGE_TYPE, new SimpleImageComponentFactoryV2(engine, rayHandler, world, rm));
        factoriesMap.put(SPRITE_TYPE, new SpriteComponentFactoryV2(engine, rayHandler, world, rm));
        factoriesMap.put(LABEL_TYPE, new LabelComponentFactoryV2(engine, rayHandler, world, rm));
        factoriesMap.put(NINE_PATCH, new NinePatchComponentFactoryV2(engine, rayHandler, world, rm));
        factoriesMap.put(COLOR_PRIMITIVE, new ColorPrimitiveComponentFactoryV2(engine, rayHandler, world, rm));

        for (ComponentFactoryV2 factory : externalFactories.values()) {
            factory.injectDependencies(engine, rayHandler, world, rm);
        }
        factoriesMap.putAll(externalFactories);
    }

    public void addExternalFactory(IExternalItemType itemType) {
        if (itemType.getComponentFactoryV2() != null)
            externalFactories.put(itemType.getTypeId(), itemType.getComponentFactoryV2());
    }

    public int createEntity(int root, int entityType, ComponentFactoryV2.InitialData initialData) {
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

    public int postProcessEntity(int entity) {
        MainItemComponent mainItemComponent = mapper.get(entity);

        if (mainItemComponent.uniqueId == -1) mainItemComponent.uniqueId = getFreeId();
        entities.put(mainItemComponent.uniqueId, entity);

        return mainItemComponent.uniqueId;
    }

    private int getFreeId() {
        if (entities.size == 0) return 1;

        // TODO: Is it performant enough?
        // Just so you know why this TODO exists and what is really going on in here, i dont know why we need to sort the elements. That's why, to be on the safe side, i wrote some code to get it sorted
        IntIntMap.Keys keys = entities.keys();
        ArrayList<Integer> ids = new ArrayList<>();
        while (keys.hasNext) ids.add(keys.next());

        Collections.sort(ids);
        for (int i = 1; i < ids.size(); i++) {
            if (ids.get(i) - ids.get(i - 1) > 1) {
                return ids.get(i - 1) + 1;
            }
        }
        return ids.get(ids.size() - 1) + 1;
    }

    public int getEntityByUniqueId(int id) {
        return entities.get(id, -1);
    }

    public void clean() {
        entities.clear();
    }
}
