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

package games.rednblack.editor.renderer.factory.component;

import com.artemis.BaseComponentMapper;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by azakhary on 5/22/2015.
 */
public abstract class ComponentFactory {

    protected IResourceRetriever rm;
    protected RayHandler rayHandler;
    protected World world;
    protected com.artemis.World engine;

    protected BaseComponentMapper<NodeComponent> nodeComponentMapper;

    public ComponentFactory() {
        nodeComponentMapper = ComponentRetriever.getMapper(NodeComponent.class);
    }

    public ComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        this();
        injectDependencies(engine, rayHandler, world, rm);
    }

    public void injectDependencies(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        this.engine = engine;
        this.rayHandler = rayHandler;
        this.world = world;
        this.rm = rm;
    }

    public abstract void createComponents(int root, int entity, MainItemVO vo);

    protected void createCommonComponents(int entity, MainItemVO vo, int entityType) {
        DimensionsComponent dimensionsComponent = createDimensionsComponent(entity, vo);
        createBoundingBoxComponent(entity, vo);
        createMainItemComponent(entity, vo, entityType);
        createTransformComponent(entity, vo, dimensionsComponent);
        createTintComponent(entity, vo);
        createZIndexComponent(entity, vo);
        createScriptComponent(entity, vo);
        createMeshComponent(entity, vo);
        createPhysicsComponents(entity, vo);
        createSensorComponent(entity, vo);
        createLightComponents(entity, vo);
        createShaderComponent(entity, vo);
    }

    protected BoundingBoxComponent createBoundingBoxComponent(int entity, MainItemVO vo) {
        BoundingBoxComponent component = engine.edit(entity).create(BoundingBoxComponent.class);
        return component;
    }

    protected ShaderComponent createShaderComponent(int entity, MainItemVO vo) {
        if (vo.shaderName == null || vo.shaderName.isEmpty()) {
            return null;
        }
        ShaderComponent component = engine.edit(entity).create(ShaderComponent.class);
        component.setShader(vo.shaderName, rm.getShaderProgram(vo.shaderName));
        component.customUniforms.putAll(vo.shaderUniforms);
        component.renderingLayer = vo.renderingLayer;
        return component;
    }

    protected MainItemComponent createMainItemComponent(int entity, MainItemVO vo, int entityType) {
        MainItemComponent component = engine.edit(entity).create(MainItemComponent.class);
        component.setCustomVarString(vo.customVars);
        component.uniqueId = vo.uniqueId;
        component.itemIdentifier = vo.itemIdentifier;
        component.libraryLink = vo.itemName;
        if (vo.tags != null) {
            for (String tag : vo.tags)
                component.tags.add(tag);
        }
        component.entityType = entityType;

        return component;
    }

    protected TransformComponent createTransformComponent(int entity, MainItemVO vo, DimensionsComponent dimensionsComponent) {
        TransformComponent component = engine.edit(entity).create(TransformComponent.class);
        component.rotation = vo.rotation;
        component.scaleX = vo.scaleX;
        component.scaleY = vo.scaleY;
        component.x = vo.x;
        component.y = vo.y;

        if (Float.isNaN(vo.originX)) component.originX = dimensionsComponent.width / 2f;
        else component.originX = vo.originX;

        if (Float.isNaN(vo.originY)) component.originY = dimensionsComponent.height / 2f;
        else component.originY = vo.originY;

        component.flipX = vo.flipX;
        component.flipY = vo.flipY;

        return component;
    }

    protected abstract DimensionsComponent createDimensionsComponent(int entity, MainItemVO vo);

    protected TintComponent createTintComponent(int entity, MainItemVO vo) {
        TintComponent component = engine.edit(entity).create(TintComponent.class);
        component.color.set(vo.tint[0], vo.tint[1], vo.tint[2], vo.tint[3]);

        return component;
    }

    protected ZIndexComponent createZIndexComponent(int entity, MainItemVO vo) {
        ZIndexComponent component = engine.edit(entity).create(ZIndexComponent.class);

        if (vo.layerName == "" || vo.layerName == null) vo.layerName = "Default";

        component.layerName = vo.layerName;
        component.setZIndex(vo.zIndex);
        component.needReOrder = false;

        return component;
    }

    protected ScriptComponent createScriptComponent(int entity, MainItemVO vo) {
        ScriptComponent component = engine.edit(entity).create(ScriptComponent.class);
        return component;
    }

    protected ParentNodeComponent createParentNodeComponent(int root, int entity) {
        ParentNodeComponent component = engine.edit(entity).create(ParentNodeComponent.class);
        component.parentEntity = root;

        return component;
    }

    protected void createNodeComponent(int root, int entity) {
        NodeComponent component = nodeComponentMapper.get(root);
        component.children.add(entity);
    }

    protected void createPhysicsComponents(int entity, MainItemVO vo) {
        if (vo.physics == null) {
            return;
        }

        createPhysicsBodyPropertiesComponent(entity, vo);
    }

    /**
     * Creats the sensor component and adds it to the entity.
     *
     * @param entity The entity to add the component to.
     * @param vo     The data transfer object to create the component from.
     */
    protected void createSensorComponent(int entity, MainItemVO vo) {
        if (vo.sensor == null) {
            return;
        }

        SensorComponent sensorComponent = engine.edit(entity).create(SensorComponent.class);
        sensorComponent.bottom = vo.sensor.bottom;
        sensorComponent.left = vo.sensor.left;
        sensorComponent.right = vo.sensor.right;
        sensorComponent.top = vo.sensor.top;

        sensorComponent.bottomSpanPercent = vo.sensor.bottomSpanPercent;
        sensorComponent.leftSpanPercent = vo.sensor.leftSpanPercent;
        sensorComponent.rightSpanPercent = vo.sensor.rightSpanPercent;
        sensorComponent.topSpanPercent = vo.sensor.topSpanPercent;
    }

    protected PhysicsBodyComponent createPhysicsBodyPropertiesComponent(int entity, MainItemVO vo) {
        PhysicsBodyComponent component = engine.edit(entity).create(PhysicsBodyComponent.class);
        component.allowSleep = vo.physics.allowSleep;
        component.sensor = vo.physics.sensor;
        component.awake = vo.physics.awake;
        component.bodyType = vo.physics.bodyType;
        component.bullet = vo.physics.bullet;
        component.centerOfMass = vo.physics.centerOfMass;
        component.damping = vo.physics.damping;
        component.density = vo.physics.density;
        component.friction = vo.physics.friction;
        component.gravityScale = vo.physics.gravityScale;
        component.mass = vo.physics.mass;
        component.restitution = vo.physics.restitution;
        component.rotationalInertia = vo.physics.rotationalInertia;
        component.angularDamping = vo.physics.angularDamping;
        component.fixedRotation = vo.physics.fixedRotation;

        component.height = vo.physics.height;

        return component;
    }

    protected LightBodyComponent createLightComponents(int entity, MainItemVO vo) {
        if (vo.light == null) {
            return null;
        }

        LightBodyComponent component = engine.edit(entity).create(LightBodyComponent.class);
        component.rays = vo.light.rays;
        component.color = vo.light.color;
        component.distance = vo.light.distance;
        component.intensity = vo.light.intensity;
        component.rayDirection = vo.light.rayDirection;
        component.softnessLength = vo.light.softnessLength;
        component.isXRay = vo.light.isXRay;
        component.isStatic = vo.light.isStatic;
        component.isSoft = vo.light.isSoft;
        component.isActive = vo.light.isActive;

        return component;
    }

    protected PolygonComponent createMeshComponent(int entity, MainItemVO vo) {
        PolygonComponent component = engine.edit(entity).create(PolygonComponent.class);
        if (vo.shape != null) {
            component.vertices = new Vector2[vo.shape.polygons.length][];
            for (int i = 0; i < vo.shape.polygons.length; i++) {
                component.vertices[i] = new Vector2[vo.shape.polygons[i].length];
                System.arraycopy(vo.shape.polygons[i], 0, component.vertices[i], 0, vo.shape.polygons[i].length);
            }

            return component;
        }
        return null;
    }

    public void setResourceManager(IResourceRetriever rm) {
        this.rm = rm;
    }

}
