package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.*;

@All(ViewPortComponent.class)
public class CullingSystem extends IteratingSystem {

    private boolean debug = false;

    protected ComponentMapper<ViewPortComponent> viewPortMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;

    private Camera camera;

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private float ppwu = 1;

    @Override
    protected void process(int entity) {
        ViewPortComponent viewPort = viewPortMapper.get(entity);
        ppwu = viewPort.pixelsPerWU;
        this.camera = viewPort.viewPort.getCamera();

        MainItemComponent m = mainItemMapper.get(entity);
        m.culled = false;

        if (debug) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        }

        NodeComponent node = nodeMapper.get(entity);
        Integer[] children = node.children.begin();
        for (int i = 0, n = node.children.size; i < n; i++) {
            Integer child = children[i];
            cull(child);
        }
        node.children.end();

        if (debug)
            shapeRenderer.end();
    }

    void cull(Integer entity) {
        BoundingBoxComponent b = boundingBoxMapper.get(entity);
        MainItemComponent m = mainItemMapper.get(entity);
        if (b == null) {
            m.culled = false;
            return;
        }

        Frustum frustum = camera.frustum;
        m.culled = !boundsInFrustum(frustum, b.rectangle);
        if (debug) {
            shapeRenderer.rect(b.rectangle.x, b.rectangle.y, b.rectangle.width, b.rectangle.height);

            shapeRenderer.circle(b.points[0].x, b.points[0].y, 5 / ppwu, 10);
            shapeRenderer.circle(b.points[1].x, b.points[1].y, 5 / ppwu, 10);
            shapeRenderer.circle(b.points[2].x, b.points[2].y, 5 / ppwu, 10);
            shapeRenderer.circle(b.points[3].x, b.points[3].y, 5 / ppwu, 10);
        }

        if (!m.culled) {
            NodeComponent node = nodeMapper.get(entity);

            if (node != null) {
                Integer[] children = node.children.begin();
                for (int i = 0, n = node.children.size; i < n; i++) {
                    Integer child = children[i];
                    cull(child);
                }
                node.children.end();
            }
        }
    }

    public boolean boundsInFrustum(Frustum frustum, Rectangle b) {
        for (int i = 0, len2 = frustum.planes.length; i < len2; i++) {
            if (frustum.planes[i].testPoint(b.x, b.y, 0) != Plane.PlaneSide.Back) continue;
            if (frustum.planes[i].testPoint(b.x + b.width, b.y, 0) != Plane.PlaneSide.Back) continue;
            if (frustum.planes[i].testPoint(b.x + b.width, b.y + b.height, 0) != Plane.PlaneSide.Back) continue;
            if (frustum.planes[i].testPoint(b.x, b.y + b.height, 0) != Plane.PlaneSide.Back) continue;
            return false;
        }

        return true;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
