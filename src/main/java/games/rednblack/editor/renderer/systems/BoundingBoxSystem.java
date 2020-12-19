package games.rednblack.editor.renderer.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.spriter.SpriterComponent;

public class BoundingBoxSystem extends IteratingSystem {

    final private ComponentMapper<DimensionsComponent> dimensionsMapper;
    final private ComponentMapper<ParentNodeComponent> parentNodeMapper;
    final private ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    final private ComponentMapper<MainItemComponent> mainItemMapper;
    final private ComponentMapper<SpriterComponent> spriterMapper;
    final private ComponentMapper<TransformComponent> transformMapper;

    private final Vector2 tmpVec = new Vector2();

    public BoundingBoxSystem() {
        super(Family.all(BoundingBoxComponent.class).get());
        dimensionsMapper = ComponentMapper.getFor(DimensionsComponent.class);
        parentNodeMapper = ComponentMapper.getFor(ParentNodeComponent.class);
        boundingBoxMapper = ComponentMapper.getFor(BoundingBoxComponent.class);
        mainItemMapper = ComponentMapper.getFor(MainItemComponent.class);
        spriterMapper = ComponentMapper.getFor(SpriterComponent.class);
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

        SpriterComponent s = spriterMapper.get(entity);
        DimensionsComponent d = dimensionsMapper.get(entity);
        TransformComponent t = transformMapper.get(entity);

        if (m != null && (!m.visible || m.culled))
                return;

        if (calcCheckSum(entity) != b.checksum) {
            if (s != null) {
                com.brashmonkey.spriter.Rectangle r = s.player.getBoudingRectangle(null);
                b.points[0].set(r.left, r.bottom);
                b.points[1].set(r.right, r.bottom );
                b.points[2].set(r.right, r.top);
                b.points[3].set(r.left, r.top);
            } else if (t.rotation == 0) {
                float scaleOffsetX = t.originX * t.scaleX - t.originX;
                float scaleOffsetY = t.originY * t.scaleY - t.originY;

                b.points[0].set(t.x -scaleOffsetX,t.y -scaleOffsetY);
                b.points[1].set(t.x -scaleOffsetX + d.width*t.scaleX,t.y -scaleOffsetY);
                b.points[2].set(t.x -scaleOffsetX + d.width*t.scaleX,t.y -scaleOffsetY + d.height*t.scaleY);
                b.points[3].set(t.x -scaleOffsetX ,t.y -scaleOffsetY + d.height*t.scaleY);
            } else {
                float pivotX = t.originX * t.scaleX;
                float pivotY = t.originY * t.scaleY;
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

                float originX = parentTransform.originX * parentTransform.scaleX;
                float originY = parentTransform.originY * parentTransform.scaleY;

                float scaleOffsetX = originX - parentTransform.originX;
                float scaleOffsetY = originY - parentTransform.originY;

                tmpVec.set(originX, originY);
                tmpVec.rotateDeg(parentTransform.rotation);

                for(int i = 0; i < 4; i++) {
                    b.points[i].add(originX - tmpVec.x, originY - tmpVec.y);

                    b.points[i].x = b.points[i].x * parentTransform.scaleX + parentTransform.x - scaleOffsetX;
                    b.points[i].y = b.points[i].y * parentTransform.scaleY + parentTransform.y - scaleOffsetY;
                }
                parentNode =  parentNodeMapper.get(parentNode.parentEntity);
            }
            b.checksum = calcCheckSum(entity);
            b.createBoundingRect();
        }
    }

    private float calcCheckSum(Entity entity) {
        ParentNodeComponent parentNode = parentNodeMapper.get(entity);
        TransformComponent t = transformMapper.get(entity);
        DimensionsComponent d = dimensionsMapper.get(entity);

        float checksum = 0;
        checksum = t.rotation + t.scaleX + t.scaleY + t.x + t.y + t.originX + t.originY + d.width + d.height;
        while (parentNode != null) {
            TransformComponent pt = transformMapper.get(parentNode.parentEntity);
            DimensionsComponent dt = dimensionsMapper.get(parentNode.parentEntity);
            if (pt == null || dt == null)
                break;
            checksum += pt.rotation + pt.scaleX + pt.scaleY + pt.x + pt.y + pt.originX + pt.originY + dt.width + dt.height;
            parentNode = parentNodeMapper.get(parentNode.parentEntity);
        }
        return checksum;
    }

    private void calcFor(BoundingBoxComponent box, TransformComponent transform, DimensionsComponent dimension, float pivotX, float pivotY) {
        float width = dimension.width*transform.scaleX;
        float height = dimension.height*transform.scaleY;

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
            scaleOffsetX = (width - dimension.width) / 2;
            scaleOffsetY = (height - dimension.height) / 2;
        }

        for(int i = 0; i < 4; i++)
            box.points[i].rotateDeg(transform.rotation);

        for(int i = 0; i < 4; i++) {
            box.points[i].x = box.points[i].x + transform.x - scaleOffsetX + pivotX;
            box.points[i].y = box.points[i].y + transform.y - scaleOffsetY + pivotY;
        }
    }
}
