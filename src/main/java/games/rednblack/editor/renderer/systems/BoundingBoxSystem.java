package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
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

        MainItemComponent m = null;
        if (parentNode != null && parentNode.parentEntity != -1){
            m = mainItemMapper.get(parentNode.parentEntity);
        }

        if (m != null && (!m.visible || m.culled))
                return;

        BoundingBoxComponent b = boundingBoxMapper.get(entity);

        DimensionsComponent d = dimensionsMapper.get(entity);
        TransformComponent t = transformMapper.get(entity);

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

        int checksum = calcCheckSum(entity, parentNode, t, d);
        if (checksum != b.checksum) {
            // parentLocalAABB is filled earlier by ParentLocalAABBSystem; these scene-space
            // corners need the position the layout has just decided.
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

    private int calcCheckSum(int entity, ParentNodeComponent parentNode, TransformComponent t, DimensionsComponent d) {
        PhysicsBodyComponent p = physicsMapper.get(entity);

        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);

        int checksum = Float.floatToRawIntBits(t.rotation) * 3
                + Float.floatToRawIntBits(scaleX) * 5
                + Float.floatToRawIntBits(scaleY) * 7
                + Float.floatToRawIntBits(t.x) * 11
                + Float.floatToRawIntBits(t.y) * 13
                + Float.floatToRawIntBits(t.originX) * 17
                + Float.floatToRawIntBits(t.originY) * 19
                + Float.floatToRawIntBits(d.width) * 23
                + Float.floatToRawIntBits(d.height) * 29
                + (p != null && p.fineBoundBox ? 31 : 0);

        while (parentNode != null && parentNode.parentEntity != -1) {
            TransformComponent pt = transformMapper.get(parentNode.parentEntity);
            DimensionsComponent dt = dimensionsMapper.get(parentNode.parentEntity);
            if (pt == null || dt == null)
                break;

            PhysicsBodyComponent pp = physicsMapper.get(parentNode.parentEntity);

            float pScaleX = pt.scaleX * (pt.flipX ? -1 : 1);
            float pScaleY = pt.scaleY * (pt.flipY ? -1 : 1);

            checksum += Float.floatToRawIntBits(pt.rotation) * 37
                    + Float.floatToRawIntBits(pScaleX) * 41
                    + Float.floatToRawIntBits(pScaleY) * 43
                    + Float.floatToRawIntBits(pt.x) * 47
                    + Float.floatToRawIntBits(pt.y) * 53
                    + Float.floatToRawIntBits(pt.originX) * 59
                    + Float.floatToRawIntBits(pt.originY) * 61
                    + Float.floatToRawIntBits(dt.width) * 67
                    + Float.floatToRawIntBits(dt.height) * 71
                    + (pp != null && pp.fineBoundBox ? 73 : 0);
            parentNode = parentNodeMapper.get(parentNode.parentEntity);
        }
        return checksum;
    }
}
