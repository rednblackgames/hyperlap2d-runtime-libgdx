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

    protected static ComponentMapper<MainItemComponent> mapper;

    protected ComponentFactory compositeComponentFactory, lightComponentFactory, particleEffectComponentFactory,
            simpleImageComponentFactory, spriteComponentFactory, labelComponentFactory,
            ninePatchComponentFactory, colorPrimitiveFactory;

    private final HashMap<Integer, ComponentFactory> externalFactories = new HashMap<>();

    // TODO: Do we still need it? Like, in Artemis all enties are already identified by a Unique ID
//    private final HashMap<Integer, Entitiy> entities = new HashMap<>();
    private final IntIntMap entities = new IntIntMap();

    public RayHandler rayHandler;
    public World world;
    public IResourceRetriever rm = null;
    public com.artemis.World engine;

    public EntityFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        this.engine = engine;
        this.engine.inject(this);
        this.rayHandler = rayHandler;
        this.world = world;
        this.rm = rm;

        compositeComponentFactory = new CompositeComponentFactory(engine, rayHandler, world, rm);
        lightComponentFactory = new LightComponentFactory(engine, rayHandler, world, rm);
        particleEffectComponentFactory = new ParticleEffectComponentFactory(engine, rayHandler, world, rm);
        simpleImageComponentFactory = new SimpleImageComponentFactory(engine, rayHandler, world, rm);
        spriteComponentFactory = new SpriteComponentFactory(engine, rayHandler, world, rm);
        labelComponentFactory = new LabelComponentFactory(engine, rayHandler, world, rm);
        ninePatchComponentFactory = new NinePatchComponentFactory(engine, rayHandler, world, rm);
        colorPrimitiveFactory = new ColorPrimitiveComponentFactory(engine, rayHandler, world, rm);
    }

    public ComponentFactory getCompositeComponentFactory() {
        return compositeComponentFactory;
    }

    public SpriteComponentFactory getSpriteComponentFactory() {
        return (SpriteComponentFactory) spriteComponentFactory;
    }

    public void addExternalFactory(IExternalItemType itemType) {
        externalFactories.put(itemType.getTypeId(), itemType.getComponentFactory());
    }

    public void initializeEntity(int root, int entity, SimpleImageVO vo) {
        simpleImageComponentFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);
    }

    public void initializeEntity(int root, int entity, Image9patchVO vo) {
        ninePatchComponentFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);
    }

    public void initializeEntity(int root, int entity, LabelVO vo) {
        labelComponentFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);

    }

    public void initializeEntity(int root, int entity, ParticleEffectVO vo) {
        particleEffectComponentFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);
    }

    public void initializeEntity(int root, int entity, TalosVO vo) {
        ComponentFactory factory = externalFactories.get(TALOS_TYPE);
        if (factory != null) {
            factory.createComponents(root, entity, vo);
            postProcessEntity(entity);
        }
    }

    public void initializeEntity(int root, int entity, LightVO vo) {
        lightComponentFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);
    }

    public void initializeEntity(int root, int entity, SpineVO vo) {
        ComponentFactory factory = externalFactories.get(SPINE_TYPE);
        if (factory != null) {
            factory.createComponents(root, entity, vo);
            postProcessEntity(entity);
        }
    }

    public void initializeEntity(int root, int entity, SpriteAnimationVO vo) {
        spriteComponentFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);
    }

    public void initializeEntity(int root, int entity, CompositeItemVO vo) {
        compositeComponentFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);
    }

    public void initializeEntity(int root, int entity, ColorPrimitiveVO vo) {
        colorPrimitiveFactory.createComponents(root, entity, vo);
        postProcessEntity(entity);
    }

    public int createRootEntity(CompositeVO compositeVo, Viewport viewport) {

        CompositeItemVO vo = new CompositeItemVO();
        vo.composite = compositeVo;
        vo.automaticResize = false;

        int entity = engine.create();
        EntityEdit edit = engine.edit(entity);

        compositeComponentFactory.createComponents(-1, entity, vo);
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
        if (entities == null || entities.size == 0) return 1;

        // TODO: Is it performant enough?
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

    public void initAllChildren(com.artemis.World engine, int entity, CompositeVO vo) {
        for (int i = 0; i < vo.sImages.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sImages.get(i));
        }

        for (int i = 0; i < vo.sImage9patchs.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sImage9patchs.get(i));
        }

        for (int i = 0; i < vo.sLabels.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sLabels.get(i));
        }

        for (int i = 0; i < vo.sParticleEffects.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sParticleEffects.get(i));
        }

        for (int i = 0; i < vo.sTalosVFX.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sTalosVFX.get(i));
        }

        for (int i = 0; i < vo.sLights.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sLights.get(i));
        }

        for (int i = 0; i < vo.sSpineAnimations.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sSpineAnimations.get(i));
        }

        for (int i = 0; i < vo.sSpriteAnimations.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sSpriteAnimations.get(i));
        }

        for (int i = 0; i < vo.sColorPrimitives.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sColorPrimitives.get(i));
        }

        for (int i = 0; i < vo.sComposites.size(); i++) {
            int child = engine.create();
            initializeEntity(entity, child, vo.sComposites.get(i));
            initAllChildren(engine, child, vo.sComposites.get(i).composite);
        }
    }

    public int getEntityByUniqueId(int id) {
        return entities.get(id, -1);
    }

    public void clean() {
        entities.clear();
    }
}
