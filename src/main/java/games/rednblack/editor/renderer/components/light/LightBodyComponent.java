package games.rednblack.editor.renderer.components.light;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.box2dLight.ChainLight;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.RemovableObject;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;

public class LightBodyComponent extends RefreshableComponent implements RemovableObject {

    protected transient ComponentMapper<TransformComponent> transformCM;
    protected transient ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    protected transient ComponentMapper<PolygonShapeComponent> polygonCM;

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

    public transient ChainLight lightObject;
    private transient RayHandler rayHandler;

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

        PolygonShapeComponent polygonShapeComponent = polygonCM.get(entity);
        PhysicsBodyComponent physicsComponent = physicsBodyCM.get(entity);
        TransformComponent transformComponent = transformCM.get(entity);

        if (polygonShapeComponent != null && physicsComponent != null && polygonShapeComponent.vertices != null) {
            Array<Vector2> verticesArray = polygonShapeComponent.vertices;
            //TODO Pool Vertices Array
            float[] chain = new float[verticesArray.size * 2];

            for (int i = 0, j = 0; i < verticesArray.size; i++) {
                Vector2 point = verticesArray.get(i);
                chain[j++] = point.x - transformComponent.originX;
                chain[j++] = point.y - transformComponent.originY;
            }
            //TODO Pool this color too!!
            Color lightColor = new Color(color[0], color[1], color[2], color[3]);
            //TODO Pooling ChainLight object would be nice :)
            lightObject = new ChainLight(rayHandler, rays, lightColor, distance, rayDirection, chain);
            lightObject.attachToBody(physicsComponent.body);
        }
    }
}
