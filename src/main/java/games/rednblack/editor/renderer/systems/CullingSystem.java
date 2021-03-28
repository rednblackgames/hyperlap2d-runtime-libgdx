package games.rednblack.editor.renderer.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;

public class CullingSystem extends IteratingSystem {

    private boolean debug = false;

    final private ComponentMapper<ViewPortComponent> viewPortMapper;
    final private ComponentMapper<NodeComponent> nodeMapper;
    final private ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    final private ComponentMapper<MainItemComponent> mainItemMapper;
    final private ComponentMapper<PhysicsBodyComponent> physicsBodyMapper;

    Rectangle view = new Rectangle();
    OrthographicCamera camera;

    ShapeRenderer shapeRenderer = new ShapeRenderer();

    public CullingSystem() {
        super(Family.all(ViewPortComponent.class).get(), 2);
        viewPortMapper = ComponentMapper.getFor(ViewPortComponent.class);
        nodeMapper = ComponentMapper.getFor(NodeComponent.class);
        boundingBoxMapper = ComponentMapper.getFor(BoundingBoxComponent.class);
        mainItemMapper = ComponentMapper.getFor(MainItemComponent.class);
        physicsBodyMapper = ComponentMapper.getFor(PhysicsBodyComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
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
        Entity[] children = node.children.begin();
        for (int i = 0, n = node.children.size; i < n; i++) {
            Entity child = children[i];
            cull(child);
        }
        node.children.end();

        if (debug)
            shapeRenderer.end();
    }

    void cull(Entity entity) {
        BoundingBoxComponent b = boundingBoxMapper.get(entity);
        if (b==null) return;
        PhysicsBodyComponent p = physicsBodyMapper.get(entity);
        if (p!= null)
            if (p.bodyType > 1) return;

        MainItemComponent m = mainItemMapper.get(entity);

        m.culled = !view.overlaps(b.rectangle);
        if (debug)
            shapeRenderer.rect(b.rectangle.x, b.rectangle.y, b.rectangle.width, b.rectangle.height);

        if (!m.culled) {
            NodeComponent node = nodeMapper.get(entity);

            if (node != null) {
                Entity[] children = node.children.begin();
                for (int i = 0, n = node.children.size; i < n; i++) {
                    Entity child = children[i];
                    cull(child);
                }
                node.children.end();
            }
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
