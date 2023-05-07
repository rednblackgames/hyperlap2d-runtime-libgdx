/*
 * ******************************************************************************
 *  * Copyright 2015 See AUTHORS file.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package games.rednblack.editor.renderer.utils;

import com.artemis.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.components.shape.CircleShapeComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;

/**
 * Component Retriever is a singleton single instance class that initialises list of
 * all component mappers on first access, and provides a retrieval methods to get {@link Component}
 * with provided class from provided {@link Entity} object
 *
 * @author azakhary on 5/19/2015.
 */
public class ComponentRetriever {

    /**
     * single static instance of this class
     */
    private static ComponentRetriever instance;

    /**
     * Unique map of mappers that can be accessed by component class
     */
    private final ObjectMap<World, ObjectMap<Class<? extends Component>, BaseComponentMapper<? extends Component>>> engineMappers = new ObjectMap<>();

    /**
     * Private constructor
     */
    private ComponentRetriever() {

    }

    /**
     * This is called only during first initialisation and populates map of mappers of all known Component mappers
     */
    private void init(World engine) {
        ObjectMap<Class<? extends Component>, BaseComponentMapper<? extends Component>> mappers = instance.engineMappers.get(engine);
        if (mappers == null) {
            mappers = new ObjectMap<>();
            self().engineMappers.put(engine, mappers);
        }

        mappers.put(LightObjectComponent.class, ComponentMapper.getFor(LightObjectComponent.class, engine));

        mappers.put(ParticleComponent.class, ComponentMapper.getFor(ParticleComponent.class, engine));

        mappers.put(LabelComponent.class, ComponentMapper.getFor(LabelComponent.class, engine));

        mappers.put(PolygonShapeComponent.class, ComponentMapper.getFor(PolygonShapeComponent.class, engine));
        mappers.put(CircleShapeComponent.class, ComponentMapper.getFor(CircleShapeComponent.class, engine));
        mappers.put(PhysicsBodyComponent.class, ComponentMapper.getFor(PhysicsBodyComponent.class, engine));
        mappers.put(SensorComponent.class, ComponentMapper.getFor(SensorComponent.class, engine));
        mappers.put(LightBodyComponent.class, ComponentMapper.getFor(LightBodyComponent.class, engine));

        mappers.put(SpriteAnimationComponent.class, ComponentMapper.getFor(SpriteAnimationComponent.class, engine));
        mappers.put(SpriteAnimationStateComponent.class, ComponentMapper.getFor(SpriteAnimationStateComponent.class, engine));

        mappers.put(BoundingBoxComponent.class, ComponentMapper.getFor(BoundingBoxComponent.class, engine));
        mappers.put(CompositeTransformComponent.class, ComponentMapper.getFor(CompositeTransformComponent.class, engine));
        mappers.put(DimensionsComponent.class, ComponentMapper.getFor(DimensionsComponent.class, engine));
        mappers.put(LayerMapComponent.class, ComponentMapper.getFor(LayerMapComponent.class, engine));
        mappers.put(MainItemComponent.class, ComponentMapper.getFor(MainItemComponent.class, engine));
        mappers.put(NinePatchComponent.class, ComponentMapper.getFor(NinePatchComponent.class, engine));
        mappers.put(NodeComponent.class, ComponentMapper.getFor(NodeComponent.class, engine));
        mappers.put(ParentNodeComponent.class, ComponentMapper.getFor(ParentNodeComponent.class, engine));
        mappers.put(TextureRegionComponent.class, ComponentMapper.getFor(TextureRegionComponent.class, engine));
        mappers.put(TintComponent.class, ComponentMapper.getFor(TintComponent.class, engine));
        mappers.put(TransformComponent.class, ComponentMapper.getFor(TransformComponent.class, engine));
        mappers.put(ViewPortComponent.class, ComponentMapper.getFor(ViewPortComponent.class, engine));
        mappers.put(ZIndexComponent.class, ComponentMapper.getFor(ZIndexComponent.class, engine));
        mappers.put(ScriptComponent.class, ComponentMapper.getFor(ScriptComponent.class, engine));
        mappers.put(ChainedEntitiesComponent.class, ComponentMapper.getFor(ChainedEntitiesComponent.class, engine));

        mappers.put(ShaderComponent.class, ComponentMapper.getFor(ShaderComponent.class, engine));

        mappers.put(ActionComponent.class, ComponentMapper.getFor(ActionComponent.class, engine));
        mappers.put(ButtonComponent.class, ComponentMapper.getFor(ButtonComponent.class, engine));

        mappers.put(NormalMapRendering.class, ComponentMapper.getFor(NormalMapRendering.class, engine));
    }

    public static void initialize(World engine) {
        if (instance == null) {
            instance = new ComponentRetriever();
        }

        self().init(engine);
    }

    /**
     * Short version of getInstance singleton variation, but with private access,
     * as there is no reason to get instance of this class, but only use it's public methods
     *
     * @return ComponentRetriever only instance
     */
    private static synchronized ComponentRetriever self() {
        return instance;
    }

    /**
     * @return returns Map of mappers, for internal use only
     */
    private ObjectMap<Class<? extends Component>, BaseComponentMapper<? extends Component>> getMappers(World engine) {
        return self().engineMappers.get(engine);
    }

    public static <T extends Component> BaseComponentMapper<T> getMapper(Class<T> type, World engine) {
        return (BaseComponentMapper<T>) self().getMappers(engine).get(type);
    }

    public static Array<Component> getComponents(int entity, Array<Component> components, World engine) {
        for (BaseComponentMapper<? extends Component> mapper : self().getMappers(engine).values()) {
            if (mapper.get(entity) != null) components.add(mapper.get(entity));
        }
        return components;
    }

    /**
     * This is to add a new mapper type externally, in case of for example implementing the plugin system,
     * where components might be initialized on the fly
     *
     * @param type
     */
    public static void addMapper(Class<? extends Component> type) {
        for (World engine : self().engineMappers.keys())
            self().getMappers(engine).put(type, ComponentMapper.getFor(type, engine));
    }

    /**
     * Returns the specified {@link Component} associated with the given entity.
     * Returns null if the component was not added before.
     *
     * @see ComponentMapper#get(int)
     */
    public static <T extends Component> T get(int entity, Class<T> type, World engine) {
        return getMapper(type, engine).get(entity);
    }

    /**
     * Returns the specified {@link Component} associated with the given entity.
     * If the {@link Component} is not present, creates a new {@link Component}.
     *
     * @see ComponentMapper#create(int)
     */
    public static <T extends Component> T create(int entity, Class<T> type, World engine) {
        return getMapper(type, engine).create(entity);
    }

    /**
     * Removes the specified {@link Component} associated with the given entity.
     *
     * @see ComponentMapper#remove(int)
     */
    public static <T extends Component> void remove(int entity, Class<T> type, World engine) {
        getMapper(type, engine).remove(entity);
    }

    /**
     * Checks if the specified {@link Component} is present in the given entity.
     *
     * @see ComponentMapper#has(int)
     */
    public static <T extends Component> boolean has(int entity, Class<T> type, World engine) {
        return getMapper(type, engine).has(entity);
    }

    /**
     * Checks if the specified entity is present in the engine.
     *
     */
    public static boolean isActive(int entity, World engine) {
        return engine.getEntityManager().isActive(entity);
    }
}
