package games.rednblack.editor.renderer.factory;

import com.artemis.ComponentMapper;
import com.artemis.EntityEdit;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.data.*;
import games.rednblack.editor.renderer.factory.component.*;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

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

        compositeComponentFactory = new CompositeComponentFactory(engine, rayHandler, world, rm);
        lightComponentFactory = new LightComponentFactory(engine, rayHandler, world, rm);
        particleEffectComponentFactory = new ParticleEffectComponentFactory(engine, rayHandler, world, rm);
        simpleImageComponentFactory = new SimpleImageComponentFactory(engine, rayHandler, world, rm);
        spriteComponentFactory = new SpriteComponentFactory(engine, rayHandler, world, rm);
        labelComponentFactory = new LabelComponentFactory(engine, rayHandler, world, rm);
        ninePatchComponentFactory = new NinePatchComponentFactory(engine, rayHandler, world, rm);
        colorPrimitiveFactory = new ColorPrimitiveComponentFactory(engine, rayHandler, world, rm);

        for (ComponentFactory factory : externalFactories.values()) {
            factory.injectDependencies(engine, rayHandler, world, rm);
        }
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

    public void initializeEntity(int root, SimpleImageVO vo) {
        postProcessEntity(simpleImageComponentFactory.createSpecialisedEntity(root, vo));
    }

    public void initializeEntity(int root, Image9patchVO vo) {
        postProcessEntity(ninePatchComponentFactory.createSpecialisedEntity(root, vo));
    }

    public void initializeEntity(int root, LabelVO vo) {
        postProcessEntity(labelComponentFactory.createSpecialisedEntity(root, vo));

    }

    public void initializeEntity(int root, ParticleEffectVO vo) {
        postProcessEntity(particleEffectComponentFactory.createSpecialisedEntity(root, vo));
    }

    public void initializeEntity(int root, TalosVO vo) {
        ComponentFactory factory = externalFactories.get(TALOS_TYPE);
        if (factory != null) {
            postProcessEntity(factory.createSpecialisedEntity(root, vo));
        }
    }

    public void initializeEntity(int root, LightVO vo) {
        postProcessEntity(lightComponentFactory.createSpecialisedEntity(root, vo));
    }

    public void initializeEntity(int root, SpineVO vo) {
        ComponentFactory factory = externalFactories.get(SPINE_TYPE);
        if (factory != null) {
            postProcessEntity(factory.createSpecialisedEntity(root, vo));
        }
    }

    public void initializeEntity(int root, SpriteAnimationVO vo) {
        postProcessEntity(spriteComponentFactory.createSpecialisedEntity(root, vo));
    }

    public int initializeEntity(int root, CompositeItemVO vo) {
        int entity = compositeComponentFactory.createSpecialisedEntity(root, vo);
        postProcessEntity(entity);
        return entity;
    }

    public void initializeEntity(int root, ColorPrimitiveVO vo) {
        postProcessEntity(colorPrimitiveFactory.createSpecialisedEntity(root, vo));
    }

    public int createRootEntity(CompositeVO compositeVo, Viewport viewport) {

        CompositeItemVO vo = new CompositeItemVO();
        vo.composite = compositeVo;
        vo.automaticResize = false;


        int entity = compositeComponentFactory.createSpecialisedEntity(-1, vo);

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
            initializeEntity(root, vo.sImages.get(i));
        }

        for (int i = 0; i < vo.sImage9patchs.size(); i++) {
            initializeEntity(root, vo.sImage9patchs.get(i));
        }

        for (int i = 0; i < vo.sLabels.size(); i++) {
            initializeEntity(root, vo.sLabels.get(i));
        }

        for (int i = 0; i < vo.sParticleEffects.size(); i++) {
            initializeEntity(root, vo.sParticleEffects.get(i));
        }

        for (int i = 0; i < vo.sTalosVFX.size(); i++) {
            initializeEntity(root, vo.sTalosVFX.get(i));
        }

        for (int i = 0; i < vo.sLights.size(); i++) {
            initializeEntity(root, vo.sLights.get(i));
        }

        for (int i = 0; i < vo.sSpineAnimations.size(); i++) {
            initializeEntity(root, vo.sSpineAnimations.get(i));
        }

        for (int i = 0; i < vo.sSpriteAnimations.size(); i++) {
            initializeEntity(root, vo.sSpriteAnimations.get(i));
        }

        for (int i = 0; i < vo.sColorPrimitives.size(); i++) {
            initializeEntity(root, vo.sColorPrimitives.get(i));
        }

        for (int i = 0; i < vo.sComposites.size(); i++) {
            CompositeItemVO compositeItemVO = vo.sComposites.get(i);
            int composite = initializeEntity(root, compositeItemVO);
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
