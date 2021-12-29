package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.One;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.box2dLight.ConeLight;
import games.rednblack.editor.renderer.box2dLight.Light;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

@One({LightObjectComponent.class, LightBodyComponent.class})
public class LightSystem extends IteratingSystem {
    protected ComponentMapper<LightObjectComponent> lightObjectComponentMapper;
    protected ComponentMapper<TransformComponent> transformComponentMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeComponentMapper;
    protected ComponentMapper<LightBodyComponent> lightBodyComponentMapper;
    protected ComponentMapper<PolygonShapeComponent> polygonComponentMapper;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyComponentMapper;

    private RayHandler rayHandler;

    private final Vector2 localCoord = new Vector2();

    @Override
    protected void process(int entityId) {

        if (lightBodyComponentMapper.get(entityId) != null) {
            processLightBody(entityId);
            return;
        }

        LightObjectComponent lightObjectComponent = lightObjectComponentMapper.get(entityId);

        Light light = lightObjectComponent.lightObject;
        if (light == null) return;

        if (light.getRayNum() != lightObjectComponent.rays) {
            light = lightObjectComponent.rebuildRays(rayHandler);
        }

        TransformMathUtils.localToSceneCoordinates(entityId, localCoord.set(0, 0), transformComponentMapper, parentNodeComponentMapper);
        ParentNodeComponent parentNodeComponent = parentNodeComponentMapper.get(entityId);

        TransformComponent transform = transformComponentMapper.get(entityId);
        float relativeRotation = transform.rotation;

        int parentEntity = parentNodeComponent.parentEntity;
        TransformComponent parentTransformComponent;

        while (parentEntity != -1) {
            parentTransformComponent = transformComponentMapper.get(parentEntity);
            relativeRotation += parentTransformComponent.rotation;
            parentNodeComponent = parentNodeComponentMapper.get(parentEntity);
            if (parentNodeComponent == null) {
                break;
            }
            parentEntity = parentNodeComponent.parentEntity;
        }

        light.setPosition(localCoord.x, localCoord.y);
        light.setSoftnessLength(lightObjectComponent.softnessLength);
        light.setActive(lightObjectComponent.isActive);
        light.setSoft(lightObjectComponent.isSoft);
        light.setHeight(lightObjectComponent.height);
        light.setIntensity(lightObjectComponent.intensity);

        if (lightObjectComponent.type == LightObjectComponent.LightType.POINT) {
            lightObjectComponent.lightObject.setColor(Color.CLEAR);
            lightObjectComponent.lightObject.setDistance(lightObjectComponent.distance);
            lightObjectComponent.lightObject.setStaticLight(lightObjectComponent.isStatic);
            lightObjectComponent.lightObject.setXray(lightObjectComponent.isXRay);
        } else {
            lightObjectComponent.lightObject.setColor(Color.CLEAR);
            lightObjectComponent.lightObject.setDistance(lightObjectComponent.distance);
            lightObjectComponent.lightObject.setStaticLight(lightObjectComponent.isStatic);
            lightObjectComponent.lightObject.setDirection(lightObjectComponent.directionDegree + relativeRotation);
            ((ConeLight) lightObjectComponent.lightObject).setConeDegree(lightObjectComponent.coneDegree);
            lightObjectComponent.lightObject.setXray(lightObjectComponent.isXRay);
        }
    }

    private void processLightBody(int entityId) {
        LightBodyComponent lightBodyComponent = lightBodyComponentMapper.get(entityId);
        PolygonShapeComponent polygonShapeComponent = polygonComponentMapper.get(entityId);
        PhysicsBodyComponent physicsComponent = physicsBodyComponentMapper.get(entityId);

        lightBodyComponent.setRayHandler(rayHandler);

        if ((polygonShapeComponent == null || physicsComponent == null) && lightBodyComponent.lightObject != null) {
            lightBodyComponent.lightObject.remove();
            lightBodyComponent.lightObject = null;
            return;
        }

        if (lightBodyComponent.lightObject == null && polygonShapeComponent != null && physicsComponent != null) {
            lightBodyComponent.scheduleRefresh();
        }

        if (lightBodyComponent.lightObject != null &&
                (lightBodyComponent.lightObject.getRayNum() != lightBodyComponent.rays)) {
            lightBodyComponent.scheduleRefresh();
        }

        lightBodyComponent.executeRefresh(entityId);

        if (lightBodyComponent.lightObject != null) {
            TransformMathUtils.localToSceneCoordinates(entityId, localCoord.set(0, 0), transformComponentMapper, parentNodeComponentMapper);

            lightBodyComponent.lightObject.setPosition(localCoord.x, localCoord.y);
            lightBodyComponent.lightObject.setSoftnessLength(lightBodyComponent.softnessLength);
            lightBodyComponent.lightObject.setHeight(lightBodyComponent.height);
            lightBodyComponent.lightObject.setIntensity(lightBodyComponent.intensity);
            lightBodyComponent.lightObject.setDistance(lightBodyComponent.distance);
            lightBodyComponent.lightObject.setActive(lightBodyComponent.isActive);
            lightBodyComponent.lightObject.setSoft(lightBodyComponent.isSoft);
            lightBodyComponent.lightObject.setStaticLight(false);//TODO Figure out why static lights does not change position
            lightBodyComponent.lightObject.setXray(lightBodyComponent.isXRay);
            lightBodyComponent.lightObject.setColor(lightBodyComponent.color[0], lightBodyComponent.color[1], lightBodyComponent.color[2], lightBodyComponent.color[3]);
            lightBodyComponent.lightObject.update();
        }
    }

    public void setRayHandler(RayHandler rayHandler) {
        this.rayHandler = rayHandler;
    }
}
