package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

@All(BoundingBoxComponent.class)
public class BoundingBoxSystem extends IteratingSystem {

    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;
    protected ComponentMapper<TransformComponent> transformMapper;

    @Override
    protected void process(int entity) {
        ParentNodeComponent parentNode = parentNodeMapper.get(entity);
        BoundingBoxComponent b = boundingBoxMapper.get(entity);

        MainItemComponent m = null;
        if (parentNode != null){
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

        float checksum = calcCheckSum(entity);
        if (checksum != b.checksum) {
            b.points[0].set(0, 0);
            b.points[1].set(d.width, 0);
            b.points[2].set(d.width, d.height);
            b.points[3].set(0, d.height);

            TransformMathUtils.localToSceneCoordinates(entity, b.points);

            b.checksum = checksum;
            b.createBoundingRect();
        }

        if (d.polygon != null) {
            d.width = originalWidth;
            d.height = originalHeight;
            t.x = originalX;
            t.y = originalY;
        }
    }

    private float calcCheckSum(int entity) {
        ParentNodeComponent parentNode = parentNodeMapper.get(entity);
        TransformComponent t = transformMapper.get(entity);
        DimensionsComponent d = dimensionsMapper.get(entity);

        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);

        float checksum = t.rotation + scaleX + scaleY + t.x + t.y + t.originX + t.originY + d.width + d.height;
        while (parentNode != null) {
            TransformComponent pt = transformMapper.get(parentNode.parentEntity);
            DimensionsComponent dt = dimensionsMapper.get(parentNode.parentEntity);
            if (pt == null || dt == null)
                break;
            float pScaleX = pt.scaleX * (pt.flipX ? -1 : 1);
            float pScaleY = pt.scaleY * (pt.flipY ? -1 : 1);
            checksum += pt.rotation + pScaleX + pScaleY + pt.x + pt.y + pt.originX + pt.originY + dt.width + dt.height;
            parentNode = parentNodeMapper.get(parentNode.parentEntity);
        }
        return checksum;
    }
}
