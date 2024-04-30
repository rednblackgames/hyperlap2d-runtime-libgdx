package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.physics.PhysicsBodyLoader;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

@All(BoundingBoxComponent.class)
public class BoundingBoxSystem extends IteratingSystem {

    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<PhysicsBodyComponent> physicsMapper;

    @Override
    protected void process(int entity) {
        ParentNodeComponent parentNode = parentNodeMapper.get(entity);
        BoundingBoxComponent b = boundingBoxMapper.get(entity);

        MainItemComponent m = null;
        if (parentNode != null && parentNode.parentEntity != -1){
            m = mainItemMapper.get(parentNode.parentEntity);
        }

        DimensionsComponent d = dimensionsMapper.get(entity);
        TransformComponent t = transformMapper.get(entity);

        if (m != null && (!m.visible || m.culled))
                return;

        float originalX = t.x;
        float originalY = t.y;
        float originalWidth = d.width;
        float originalHeight = d.height;

        if (d.polygon != null) {
            Rectangle rectangle = d.polygon.getBoundingRectangle();
            d.width = rectangle.width;
            d.height = rectangle.height;
            t.x += rectangle.x;
            t.y += rectangle.y;
        }

        float checksum = calcCheckSum(entity, parentNode, t, d);
        if (checksum != b.checksum) {
            b.points[0].set(0, 0);
            b.points[1].set(d.width, 0);
            b.points[2].set(d.width, d.height);
            b.points[3].set(0, d.height);

            TransformMathUtils.localToSceneCoordinates(entity, b.points, transformMapper, parentNodeMapper);

            b.checksum = checksum;

            PhysicsBodyComponent p = physicsMapper.get(entity);
            if (p != null && p.body != null && p.fineBoundBox) {
                PhysicsBodyLoader.calculateFixtureBoundingBoxes(b, p);
            } else {
                b.createBoundingRect();
            }
        }

        if (d.polygon != null) {
            d.width = originalWidth;
            d.height = originalHeight;
            t.x = originalX;
            t.y = originalY;
        }
    }

    private float calcCheckSum(int entity, ParentNodeComponent parentNode, TransformComponent t, DimensionsComponent d) {
        PhysicsBodyComponent p = physicsMapper.get(entity);

        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);
        float fineBB = p != null && p.fineBoundBox ? 1 : 0;

        float checksum = t.rotation + scaleX + scaleY + t.x + t.y + t.originX + t.originY + d.width + d.height + fineBB;
        while (parentNode != null && parentNode.parentEntity != -1) {
            TransformComponent pt = transformMapper.get(parentNode.parentEntity);
            DimensionsComponent dt = dimensionsMapper.get(parentNode.parentEntity);
            if (pt == null || dt == null)
                break;

            PhysicsBodyComponent pp = physicsMapper.get(parentNode.parentEntity);

            float pScaleX = pt.scaleX * (pt.flipX ? -1 : 1);
            float pScaleY = pt.scaleY * (pt.flipY ? -1 : 1);
            float pFineBB = pp != null && pp.fineBoundBox ? 1 : 0;

            checksum += pt.rotation + pScaleX + pScaleY + pt.x + pt.y + pt.originX + pt.originY + dt.width + dt.height + pFineBB;
            parentNode = parentNodeMapper.get(parentNode.parentEntity);
        }
        return checksum;
    }
}
