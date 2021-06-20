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

import com.artemis.Archetype;
import com.artemis.ArchetypeBuilder;
import com.artemis.ComponentMapper;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

/**
 * Created by azakhary on 5/22/2015.
 */
public abstract class ComponentFactory {

    protected static ComponentMapper<BoundingBoxComponent> boundingBoxCM;
    protected static ComponentMapper<DimensionsComponent> dimensionsCM;
    protected static ComponentMapper<LightBodyComponent> lightBodyCM;
    protected static ComponentMapper<MainItemComponent> mainItemCM;
    protected static ComponentMapper<NodeComponent> nodeCM;
    protected static ComponentMapper<ParentNodeComponent> parentNodeCM;
    protected static ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    protected static ComponentMapper<PolygonComponent> polygonCM;
    protected static ComponentMapper<ScriptComponent> scriptCM;
    protected static ComponentMapper<SensorComponent> sensorCM;
    protected static ComponentMapper<ShaderComponent> shaderCM;
    protected static ComponentMapper<TintComponent> tintCM;
    protected static ComponentMapper<TransformComponent> transformCM;
    protected static ComponentMapper<ZIndexComponent> zIndexCM;

    protected IResourceRetriever rm;
    protected RayHandler rayHandler;
    protected World world;
    protected com.artemis.World engine;

    private Archetype entityArchetype;


    /**
     * Do call injectDependencies manually when using this constructor!
     */
    public ComponentFactory() {
    }

    public ComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        injectDependencies(engine, rayHandler, world, rm);
    }

    public void injectDependencies(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        this.engine = engine;
        this.engine.inject(this);
        this.rayHandler = rayHandler;
        this.world = world;
        this.rm = rm;

        this.entityArchetype = new ArchetypeBuilder()

                .add(DimensionsComponent.class)
                .add(BoundingBoxComponent.class)
                .add(MainItemComponent.class)
                .add(TransformComponent.class)

                .add(TintComponent.class)
                .add(ZIndexComponent.class)
                .add(ScriptComponent.class)
                .add(PolygonComponent.class)

                .add(PhysicsBodyComponent.class)
                .add(SensorComponent.class)
                .add(LightBodyComponent.class)
                .add(ShaderComponent.class)

                .build(engine);
    }

    /**
     * Creates an entity supplied with the necessary Specialised Components.
     */
    public abstract int createSpecialisedEntity(int root, MainItemVO vo);

    protected int createGeneralEntity(MainItemVO vo, int entityType) {
        int entity = engine.create(entityArchetype);

        DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);

        initializeDimensionsComponent(dimensionsComponent, vo);
        initializeBoundingBoxComponent(boundingBoxCM.get(entity), vo);
        initializeMainItemComponent(mainItemCM.get(entity), vo, entityType);
        initializeTransformComponent(transformCM.get(entity), vo, dimensionsComponent);

        initializeTintComponent(tintCM.get(entity), vo);
        initializeZIndexComponent(zIndexCM.get(entity), vo);
        initializeScriptComponent(scriptCM.get(entity), vo);
        initializeMeshComponent(entity, vo);

        checkPhysicsBodyComponent(entity, vo);
        initializeSensorComponent(sensorCM.get(entity), vo);
        checkLightBodyComponent(entity, vo);
        checkShaderComponent(entity, vo);

        return entity;
    }

    protected abstract void initializeDimensionsComponent(DimensionsComponent component, MainItemVO vo);

    /**
     * No initialization required, just add the component.
     */
    protected void initializeBoundingBoxComponent(BoundingBoxComponent component, MainItemVO vo) {
    }

    protected void initializeMainItemComponent(MainItemComponent component, MainItemVO vo, int entityType) {
        component.setCustomVarString(vo.customVars);
        component.uniqueId = vo.uniqueId;
        component.itemIdentifier = vo.itemIdentifier;
        component.libraryLink = vo.itemName;
        if (vo.tags != null) {
            for (String tag : vo.tags)
                component.tags.add(tag);
        }
        component.entityType = entityType;
    }

    protected void initializeTransformComponent(TransformComponent component, MainItemVO vo, DimensionsComponent dimensionsComponent) {
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
    }

    protected void initializeTintComponent(TintComponent component, MainItemVO vo) {
        component.color.set(vo.tint[0], vo.tint[1], vo.tint[2], vo.tint[3]);
    }

    protected void initializeZIndexComponent(ZIndexComponent component, MainItemVO vo) {
        if (vo.layerName == null || vo.layerName.isEmpty()) vo.layerName = "Default";

        component.layerName = vo.layerName;
        component.setZIndex(vo.zIndex);
        component.needReOrder = false;
    }

    protected void initializeScriptComponent(ScriptComponent component, MainItemVO vo) {
        component.engine = engine;
    }

    protected void initializeMeshComponent(int entity, MainItemVO vo) {
        if (vo.shape == null) {
            polygonCM.remove(entity);
            return;
        }

        PolygonComponent component = polygonCM.get(entity);
        component.vertices = new Vector2[vo.shape.polygons.length][];
        for (int i = 0; i < vo.shape.polygons.length; i++) {
            component.vertices[i] = new Vector2[vo.shape.polygons[i].length];
            System.arraycopy(vo.shape.polygons[i], 0, component.vertices[i], 0, vo.shape.polygons[i].length);
        }
    }

    protected void checkPhysicsBodyComponent(int entity, MainItemVO vo) {
        if (vo.physics == null) {
            physicsBodyCM.remove(entity);
            return;
        }
        initializePhysicsBodyPropertiesComponent(physicsBodyCM.get(entity), vo);
    }

    protected void initializePhysicsBodyPropertiesComponent(PhysicsBodyComponent component, MainItemVO vo) {
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
    }

    protected void initializeSensorComponent(SensorComponent component, MainItemVO vo) {
        if (vo.sensor == null) return;

        component.bottom = vo.sensor.bottom;
        component.left = vo.sensor.left;
        component.right = vo.sensor.right;
        component.top = vo.sensor.top;

        component.bottomSpanPercent = vo.sensor.bottomSpanPercent;
        component.leftSpanPercent = vo.sensor.leftSpanPercent;
        component.rightSpanPercent = vo.sensor.rightSpanPercent;
        component.topSpanPercent = vo.sensor.topSpanPercent;
    }

    protected void checkLightBodyComponent(int entity, MainItemVO vo) {
        if (vo.light == null) {
            lightBodyCM.remove(entity);
            return;
        }

        LightBodyComponent component = lightBodyCM.get(entity);
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
    }

    protected void checkShaderComponent(int entity, MainItemVO vo) {
        if (vo.shaderName == null || vo.shaderName.isEmpty()) {
            shaderCM.remove(entity);
            return;
        }
        ShaderComponent component = shaderCM.get(entity);
        component.setShader(vo.shaderName, rm.getShaderProgram(vo.shaderName));
        component.customUniforms.putAll(vo.shaderUniforms);
        component.renderingLayer = vo.renderingLayer;
    }

    protected void initializeParentNodeComponent(int root, int entity) {
        ParentNodeComponent component = parentNodeCM.create(entity);
        component.parentEntity = root;
    }

    protected void createNodeComponent(int root, int entity) {
        NodeComponent component = nodeCM.get(root);
        component.children.add(entity);
    }

    protected void adjustNodeHierarchy(int root, int entity) {
        // Add this component to it's parents children references
        nodeCM.get(root).children.add(entity);
        // Set the entity's parent reference to it's parent
        parentNodeCM.get(entity).parentEntity=root;
    }

    public void setResourceManager(IResourceRetriever rm) {
        this.rm = rm;
    }

}
