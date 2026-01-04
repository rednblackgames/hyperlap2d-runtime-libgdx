package games.rednblack.editor.renderer.components.light;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.lights.ChainLight;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;

public class LightBodyComponent extends RefreshableComponent {

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
    public Vector3 falloff = new Vector3(1, 0, 5);
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
        falloff.set(1, 0, 0);
        isStatic = false;
        isXRay = false;
        isSoft = true;
        isActive = true;

        needsRefresh = false;

        if (lightObject != null) {
            lightObject.remove(true);
            lightObject = null;
        }

        rayHandler = null;
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

        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        if (polygonShapeComponent != null && physicsComponent != null && physicsComponent.body != null && polygonShapeComponent.vertices != null) {
            Array<Vector2> verticesArray = polygonShapeComponent.vertices;
            //TODO Pool Vertices Array
            float[] chain = new float[verticesArray.size * 2];

            for (int i = 0, j = 0; i < verticesArray.size; i++) {
                Vector2 point = verticesArray.get(i);
                chain[j++] = (point.x - transformComponent.originX) * scaleX;
                chain[j++] = (point.y - transformComponent.originY) * scaleY;
            }
            //TODO Pooling ChainLight object would be nice :)
            lightObject = new ChainLight(rayHandler, rays, null, distance, rayDirection, chain);
            lightObject.attachToBody(physicsComponent.body);
        }
    }
}
