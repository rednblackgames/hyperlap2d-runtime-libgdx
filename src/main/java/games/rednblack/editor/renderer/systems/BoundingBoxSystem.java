package games.rednblack.editor.renderer.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.*;

public class BoundingBoxSystem extends IteratingSystem {

    final private ComponentMapper<DimensionsComponent> dimensionsMapper;
    final private ComponentMapper<ParentNodeComponent> parentNodeMapper;
    final private ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    final private ComponentMapper<MainItemComponent> mainItemMapper;
    final private ComponentMapper<TransformComponent> transformMapper;

    private final Vector2 tmpVec = new Vector2();

    public BoundingBoxSystem() {
        super(Family.all(BoundingBoxComponent.class).get());
        dimensionsMapper = ComponentMapper.getFor(DimensionsComponent.class);
        parentNodeMapper = ComponentMapper.getFor(ParentNodeComponent.class);
        boundingBoxMapper = ComponentMapper.getFor(BoundingBoxComponent.class);
        mainItemMapper = ComponentMapper.getFor(MainItemComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
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

        if (calcCheckSum(entity) != b.checksum) {
            float scaleX = t.scaleX * (t.flipX ? -1 : 1);
            float scaleY = t.scaleY * (t.flipY ? -1 : 1);

            if (t.rotation == 0) {
                float scaleOffsetX = t.originX * scaleX - t.originX;
                float scaleOffsetY = t.originY * scaleY - t.originY;

                b.points[0].set(t.x -scaleOffsetX,t.y -scaleOffsetY);
                b.points[1].set(t.x -scaleOffsetX + d.width*scaleX,t.y -scaleOffsetY);
                b.points[2].set(t.x -scaleOffsetX + d.width*scaleX,t.y -scaleOffsetY + d.height*scaleY);
                b.points[3].set(t.x -scaleOffsetX ,t.y -scaleOffsetY + d.height*scaleY);
            } else {
                float pivotX = t.originX * scaleX;
                float pivotY = t.originY * scaleY;
                calcFor(b, t, d, pivotX, pivotY);
            }

            while (parentNode != null) {
                TransformComponent parentTransform = transformMapper.get(parentNode.parentEntity);
                if (parentTransform == null)
                    break;
                if (parentTransform.rotation != 0) {
                    for(int i = 0; i < 4; i++)
                        b.points[i].rotateDeg(parentTransform.rotation);
                }

                float pScaleX = parentTransform.scaleX * (parentTransform.flipX ? -1 : 1);
                float pScaleY = parentTransform.scaleY * (parentTransform.flipY ? -1 : 1);

                float originX = parentTransform.originX * pScaleX;
                float originY = parentTransform.originY * pScaleY;

                float scaleOffsetX = originX - parentTransform.originX;
                float scaleOffsetY = originY - parentTransform.originY;

                tmpVec.set(originX, originY);
                tmpVec.rotateDeg(parentTransform.rotation);

                for(int i = 0; i < 4; i++) {
                    b.points[i].add(originX - tmpVec.x, originY - tmpVec.y);

                    b.points[i].x = b.points[i].x * pScaleX + parentTransform.x - scaleOffsetX;
                    b.points[i].y = b.points[i].y * pScaleY + parentTransform.y - scaleOffsetY;
                }
                parentNode =  parentNodeMapper.get(parentNode.parentEntity);
            }
            b.checksum = calcCheckSum(entity);
            b.createBoundingRect();
        }

        if (d.polygon != null) {
            d.width = originalWidth;
            d.height = originalHeight;
            t.x = originalX;
            t.y = originalY;
        }
    }

    private float calcCheckSum(Entity entity) {
        ParentNodeComponent parentNode = parentNodeMapper.get(entity);
        TransformComponent t = transformMapper.get(entity);
        DimensionsComponent d = dimensionsMapper.get(entity);

        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);

        float checksum = 0;
        checksum = t.rotation + scaleX + scaleY + t.x + t.y + t.originX + t.originY + d.width + d.height;
        while (parentNode != null) {
            TransformComponent pt = transformMapper.get(parentNode.parentEntity);
            DimensionsComponent dt = dimensionsMapper.get(parentNode.parentEntity);
            if (pt == null || dt == null)
                break;
            float pScaleX = t.scaleX * (t.flipX ? -1 : 1);
            float pScaleY = t.scaleY * (t.flipY ? -1 : 1);
            checksum += pt.rotation + pScaleX + pScaleY + pt.x + pt.y + pt.originX + pt.originY + dt.width + dt.height;
            parentNode = parentNodeMapper.get(parentNode.parentEntity);
        }
        return checksum;
    }

    private void calcFor(BoundingBoxComponent box, TransformComponent transform, DimensionsComponent dimension, float pivotX, float pivotY) {
        float scaleX = transform.scaleX * (transform.flipX ? -1 : 1);
        float scaleY = transform.scaleY * (transform.flipY ? -1 : 1);

        float width = dimension.width*scaleX;
        float height = dimension.height*scaleY;

        box.points[0].set(-pivotX,-pivotY);
        box.points[1].set(width-pivotX, -pivotY);
        box.points[2].set(-pivotX,height-pivotY);
        box.points[3].set(width-pivotX,height-pivotY);

        float scaleOffsetX;
        float scaleOffsetY;

        if (pivotX == 0 && pivotY == 0) {
            scaleOffsetX = 0;
            scaleOffsetY = 0;
        } else {
            scaleOffsetX = (width - dimension.width) * (transform.originX / dimension.width);
            scaleOffsetY = (height - dimension.height) * (transform.originY / dimension.height);
        }

        for(int i = 0; i < 4; i++)
            box.points[i].rotateDeg(transform.rotation);

        for(int i = 0; i < 4; i++) {
            box.points[i].x = box.points[i].x + transform.x - scaleOffsetX + pivotX;
            box.points[i].y = box.points[i].y + transform.y - scaleOffsetY + pivotY;
        }
    }
}
