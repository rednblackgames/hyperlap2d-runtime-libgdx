package games.rednblack.editor.renderer.factory;

import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.data.*;
import games.rednblack.editor.renderer.factory.component.*;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class EntityFactory {
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

    protected final HashMap<Integer, ComponentFactory> factoriesMap = new HashMap<>();
    private final HashMap<Integer, ComponentFactory> externalFactories = new HashMap<>();

    // TODO: Do we still need it? Like, in Artemis all enties are already identified by a Unique ID
//    private final HashMap<Integer, Entitiy> entities = new HashMap<>();
    public final IntIntMap entities = new IntIntMap();

    public RayHandler rayHandler;
    public World world;
    public IResourceRetriever rm = null;
    public com.artemis.World engine;

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
    }

    public void addExternalFactory(IExternalItemType itemType) {
        externalFactories.put(itemType.getTypeId(), itemType.getComponentFactory());
    }

    public int createEntity(int root, SimpleImageVO vo) {
        int entity = factoriesMap.get(IMAGE_TYPE).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createEntity(int root, Image9patchVO vo) {
        int entity = factoriesMap.get(NINE_PATCH).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createEntity(int root, LabelVO vo) {
        int entity = factoriesMap.get(LABEL_TYPE).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createEntity(int root, ParticleEffectVO vo) {
        int entity = factoriesMap.get(PARTICLE_TYPE).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createEntity(int root, TalosVO vo) {
        ComponentFactory factory = externalFactories.get(TALOS_TYPE);
        if (factory != null) {
            int entity = factory.createSpecialisedEntity(root, vo);
            postProcessEntity(entity);
            return entity;
        }
        return -1;
    }

    public int createEntity(int root, LightVO vo) {
        int entity = factoriesMap.get(LIGHT_TYPE).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createEntity(int root, SpineVO vo) {
        ComponentFactory factory = externalFactories.get(SPINE_TYPE);
        if (factory != null) {
            int entity = factory.createSpecialisedEntity(root, vo);
            postProcessEntity(entity);
            return entity;
        }
        return -1;
    }

    public int createEntity(int root, SpriteAnimationVO vo) {
        int entity = factoriesMap.get(SPRITE_TYPE).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createEntity(int root, CompositeItemVO vo) {
        int entity = factoriesMap.get(COMPOSITE_TYPE).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createEntity(int root, ColorPrimitiveVO vo) {
        int entity = factoriesMap.get(COLOR_PRIMITIVE).createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public int createRootEntity(CompositeVO compositeVo, Viewport viewport) {

        CompositeItemVO vo = new CompositeItemVO();
        vo.composite = compositeVo;
        vo.automaticResize = false;


        int entity = factoriesMap.get(COMPOSITE_TYPE).createSpecialisedEntity(-1, vo);

        // TODO; remove this
        EntityEdit edit = engine.edit(entity);
        TransformComponent transform = edit.create(TransformComponent.class);

        ViewPortComponent viewPortComponent = edit.create(ViewPortComponent.class);
        viewPortComponent.viewPort = viewport;

        viewPortComponent.viewPort.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        postProcessEntity(entity);

        return entity;
    }

    public int postProcessEntity(int entity) {
        MainItemComponent mainItemComponent = mapper.get(entity);

        if (mainItemComponent.uniqueId == -1) mainItemComponent.uniqueId = getFreeId();
        entities.put(mainItemComponent.uniqueId, entity);

        return mainItemComponent.uniqueId;
    }

    private int getFreeId() {
        // TODO: will entities always remain non null?
//        if (entities == null || entities.size == 0) return 1;
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

    public int updateMap(int entity) {
        MainItemComponent mainItemComponent = mapper.get(entity);

        entities.put(mainItemComponent.uniqueId, entity);

        return mainItemComponent.uniqueId;
    }

    public void initAllChildren(int root, CompositeVO vo) {
        for (int i = 0; i < vo.sImages.size(); i++) {
            createEntity(root, vo.sImages.get(i));
        }

        for (int i = 0; i < vo.sImage9patchs.size(); i++) {
            createEntity(root, vo.sImage9patchs.get(i));
        }

        for (int i = 0; i < vo.sLabels.size(); i++) {
            createEntity(root, vo.sLabels.get(i));
        }

        for (int i = 0; i < vo.sParticleEffects.size(); i++) {
            createEntity(root, vo.sParticleEffects.get(i));
        }

        for (int i = 0; i < vo.sTalosVFX.size(); i++) {
            createEntity(root, vo.sTalosVFX.get(i));
        }

        for (int i = 0; i < vo.sLights.size(); i++) {
            createEntity(root, vo.sLights.get(i));
        }

        for (int i = 0; i < vo.sSpineAnimations.size(); i++) {
            createEntity(root, vo.sSpineAnimations.get(i));
        }

        for (int i = 0; i < vo.sSpriteAnimations.size(); i++) {
            createEntity(root, vo.sSpriteAnimations.get(i));
        }

        for (int i = 0; i < vo.sColorPrimitives.size(); i++) {
            createEntity(root, vo.sColorPrimitives.get(i));
        }

        for (int i = 0; i < vo.sComposites.size(); i++) {
            CompositeItemVO compositeItemVO = vo.sComposites.get(i);
            int composite = createEntity(root, compositeItemVO);
            initAllChildren(composite, compositeItemVO.composite);
        }

    }

    public int getEntityByUniqueId(int id) {
        return entities.get(id, -1);
    }

    public void clean() {
        entities.clear();
    }
}
