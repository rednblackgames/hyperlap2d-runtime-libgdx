package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.*;

@All(ViewPortComponent.class)
public class CullingSystem extends IteratingSystem {

    private boolean debug = false;

    protected ComponentMapper<ViewPortComponent> viewPortMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;

    Rectangle view = new Rectangle();
    OrthographicCamera camera;

    ShapeRenderer shapeRenderer = new ShapeRenderer();
    float ppwu = 1;

    @Override
    protected void process(int entity) {
        ViewPortComponent viewPort = viewPortMapper.get(entity);
        this.camera = (OrthographicCamera) viewPort.viewPort.getCamera();
        view.width = (camera.viewportWidth * camera.zoom);
        view.height = (camera.viewportHeight * camera.zoom);
        view.x = camera.position.x - (view.width * 0.5f);
        view.y = camera.position.y - (view.height * 0.5f);

        MainItemComponent m = mainItemMapper.get(entity);
        m.culled = false;

        if(debug) {
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
        if (b==null) return;

        MainItemComponent m = mainItemMapper.get(entity);

        m.culled = !view.overlaps(b.rectangle);
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

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setPpwu(float ppwu) {
        this.ppwu = ppwu;
    }
}
