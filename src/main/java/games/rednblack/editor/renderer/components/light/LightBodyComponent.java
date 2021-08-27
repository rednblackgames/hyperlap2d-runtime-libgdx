package games.rednblack.editor.renderer.components.light;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.box2dLight.ChainLight;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.RemovableObject;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.utils.PolygonUtils;

public class LightBodyComponent extends RefreshableComponent implements RemovableObject {

    protected ComponentMapper<TransformComponent> transformCM;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    protected ComponentMapper<PolygonComponent> polygonCM;


    protected boolean needsRefresh = false;

    public float[] color = new float[]{1f, 1f, 1f, 1f};
    public int rays = 4;
    public float distance = 30;
    public int rayDirection = 1;
    public float softnessLength = 1f;
    public float height = 0f;
    public boolean isStatic = false;
    public boolean isXRay = false;
    public boolean isSoft = true;
    public boolean isActive = true;
    public float intensity = 1f;

    public ChainLight lightObject;
    private RayHandler rayHandler;

    public LightBodyComponent() {

    }

    @Override
    public void onRemove() {
        if (lightObject != null) {
            lightObject.remove();
            lightObject = null;
        }
    }

    @Override
    public void reset() {
        color[0] = 1f;
        color[1] = 1f;
        color[2] = 1f;
        color[3] = 1f;

        rays = 4;
        distance = 30;
        rayDirection = 1;
        softnessLength = 1f;
        height = 0f;
        isStatic = false;
        isXRay = false;
        isSoft = true;
        isActive = true;

        needsRefresh = false;

        lightObject = null;
    }

    public void setRayHandler(RayHandler rayHandler) {
        this.rayHandler = rayHandler;
    }

    @Override
    public void scheduleRefresh() {
        needsRefresh = true;
    }

    @Override
    public void executeRefresh(int entity) {
        if (needsRefresh) {
            refresh(entity);
            needsRefresh = false;
        }
    }

    protected void refresh(int entity) {
        if (lightObject != null) {
            lightObject.remove();
            lightObject = null;
        }

        PolygonComponent polygonComponent = polygonCM.get(entity);
        PhysicsBodyComponent physicsComponent = physicsBodyCM.get(entity);
        TransformComponent transformComponent = transformCM.get(entity);

        if (polygonComponent != null && physicsComponent != null && polygonComponent.vertices != null) {
            Vector2[] verticesArray = PolygonUtils.mergeTouchingPolygonsToOne(polygonComponent.vertices);

            float[] chain = new float[verticesArray.length * 2];

            for (int i = 0, j = 0; i < verticesArray.length; i++) {
                Vector2 point = verticesArray[i];
                chain[j++] = point.x - transformComponent.originX;
                chain[j++] = point.y - transformComponent.originY;
            }

            Color lightColor = new Color(color[0], color[1], color[2], color[3]);
            lightObject = new ChainLight(rayHandler, rays, lightColor, distance, rayDirection, chain);
            lightObject.attachToBody(physicsComponent.body);
        }
    }
}
