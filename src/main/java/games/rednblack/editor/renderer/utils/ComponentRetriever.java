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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;

import games.rednblack.editor.renderer.components.ActionComponent;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.LayerMapComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.components.ShaderComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.label.TypingLabelComponent;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
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
    private final Map<Class<? extends Component>, ComponentMapper<? extends Component>> mappers = new HashMap<>();

    /**
     * Private constructor
     */
    private ComponentRetriever() {

    }

    /**
     * This is called only during first initialisation and populates map of mappers of all known Component mappers
     * it might be a good idea to use Reflections library later to create this list from all classes in components package of runtime, all in favour?
     */
    private void init() {
    	mappers.put(LightObjectComponent.class, ComponentMapper.getFor(LightObjectComponent.class));
    	
    	mappers.put(ParticleComponent.class, ComponentMapper.getFor(ParticleComponent.class));

        mappers.put(LabelComponent.class, ComponentMapper.getFor(LabelComponent.class));
        mappers.put(TypingLabelComponent.class, ComponentMapper.getFor(TypingLabelComponent.class));

    	mappers.put(PolygonComponent.class, ComponentMapper.getFor(PolygonComponent.class));
    	mappers.put(PhysicsBodyComponent.class, ComponentMapper.getFor(PhysicsBodyComponent.class));
    	mappers.put(SensorComponent.class, ComponentMapper.getFor(SensorComponent.class));
        mappers.put(LightBodyComponent.class, ComponentMapper.getFor(LightBodyComponent.class));

        mappers.put(SpriteAnimationComponent.class, ComponentMapper.getFor(SpriteAnimationComponent.class));
        mappers.put(SpriteAnimationStateComponent.class, ComponentMapper.getFor(SpriteAnimationStateComponent.class));

        mappers.put(BoundingBoxComponent.class, ComponentMapper.getFor(BoundingBoxComponent.class));
        mappers.put(CompositeTransformComponent.class, ComponentMapper.getFor(CompositeTransformComponent.class));
        mappers.put(DimensionsComponent.class, ComponentMapper.getFor(DimensionsComponent.class));
        mappers.put(LayerMapComponent.class, ComponentMapper.getFor(LayerMapComponent.class));
        mappers.put(MainItemComponent.class, ComponentMapper.getFor(MainItemComponent.class));
        mappers.put(NinePatchComponent.class, ComponentMapper.getFor(NinePatchComponent.class));
        mappers.put(NodeComponent.class, ComponentMapper.getFor(NodeComponent.class));
        mappers.put(ParentNodeComponent.class, ComponentMapper.getFor(ParentNodeComponent.class));
        mappers.put(TextureRegionComponent.class, ComponentMapper.getFor(TextureRegionComponent.class));
        mappers.put(TintComponent.class, ComponentMapper.getFor(TintComponent.class));
        mappers.put(TransformComponent.class, ComponentMapper.getFor(TransformComponent.class));
        mappers.put(ViewPortComponent.class, ComponentMapper.getFor(ViewPortComponent.class));
        mappers.put(ZIndexComponent.class, ComponentMapper.getFor(ZIndexComponent.class));
        mappers.put(ScriptComponent.class, ComponentMapper.getFor(ScriptComponent.class));

        mappers.put(ShaderComponent.class, ComponentMapper.getFor(ShaderComponent.class));

        mappers.put(ActionComponent.class, ComponentMapper.getFor(ActionComponent.class));
        mappers.put(ButtonComponent.class, ComponentMapper.getFor(ButtonComponent.class));

        mappers.put(NormalMapRendering.class, ComponentMapper.getFor(NormalMapRendering.class));
    }

    /**
     * Short version of getInstance singleton variation, but with private access,
     * as there is no reason to get instance of this class, but only use it's public methods
     *
     * @return ComponentRetriever only instance
     */
    private static synchronized ComponentRetriever self() {
        if(instance == null) {
            instance = new ComponentRetriever();

            // Important to initialize during first creation, to populate mappers map
            instance.init();
        }

        return instance;
    }

    /**
     * @return returns Map of mappers, for internal use only
     */
    private Map<Class<? extends Component>, ComponentMapper<? extends Component>> getMappers() {
        return mappers;
    }

    /**
     * Retrieves Component of provided type from a provided entity
     * @param entity of type Entity to retrieve component from
     * @param type of the component
     * @param <T>
     *
     * @return Component subclass instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component> T get(Entity entity, Class<T> type) {
        return (T)self().getMappers().get(type).get(entity);
    }


    public static  Collection<Component> getComponents(Entity entity) {
        Collection<Component> components = new ArrayList<>();
        for (ComponentMapper<? extends Component> mapper : self().getMappers().values()) {
            if(mapper.get(entity) != null) components.add(mapper.get(entity));
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
        self().getMappers().put(type, ComponentMapper.getFor(type));
    }
}
