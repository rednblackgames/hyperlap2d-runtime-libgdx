package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

@All(BoundingBoxComponent.class)
public class BoundingBoxSystem extends IteratingSystem {

    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<SensorComponent> sensorMapper;

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

        float checksum = calcCheckSum(entity);
        if (checksum != b.checksum) {
            SensorComponent s = sensorMapper.get(entity);
            float additionalWidth = 0;
            float additionalHeight = 0;
            float x = 0;
            float y = 0;
            if (s != null) {
                if (s.top) {
                    additionalHeight = Math.max(d.height * s.topHeightPercent, additionalHeight);
                    if (s.topSpanPercent > 1) {
                        float w = (d.width * s.topSpanPercent) - d.width;
                        additionalWidth = Math.max(w, additionalWidth);
                        x = Math.min(x, -w);
                    }
                }
                if (s.bottom) {
                    y = Math.min(-d.height * s.bottomHeightPercent, y);
                    if (s.topSpanPercent > 1) {
                        float w = (d.width * s.bottomSpanPercent) - d.width;
                        additionalWidth = Math.max(w, additionalWidth);
                        x = Math.min(x, -w);
                    }
                }
                if (s.left) {
                    x = Math.min(-d.width * s.leftWidthPercent, x);
                    if (s.leftSpanPercent > 1) {
                        float h = (d.height * s.leftSpanPercent) - d.height;
                        additionalHeight = Math.max(h, additionalHeight);
                        y = Math.min(y, -h);
                    }
                }
                if (s.right) {
                    additionalWidth = Math.max(d.width * s.rightWidthPercent, additionalWidth);
                    if (s.rightSpanPercent > 1) {
                        float h = (d.height * s.rightSpanPercent) - d.height;
                        additionalHeight = Math.max(h, additionalHeight);
                        y = Math.min(y, -h);
                    }
                }
            }

            b.points[0].set(x, y);
            b.points[1].set(d.width + additionalWidth, y);
            b.points[2].set(d.width + additionalWidth, d.height + additionalHeight);
            b.points[3].set(x, d.height + additionalHeight);

            TransformMathUtils.localToSceneCoordinates(entity, b.points, transformMapper, parentNodeMapper);

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
        SensorComponent s = sensorMapper.get(entity);

        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);

        float sensorsSum = 0;
        if (s != null) {
            if (s.top) {
                sensorsSum += s.topHeightPercent + s.topSpanPercent;
            }
            if (s.bottom) {
                sensorsSum += s.bottomHeightPercent + s.bottomSpanPercent;
            }
            if (s.left) {
                sensorsSum += s.leftWidthPercent + s.leftSpanPercent;
            }
            if (s.right) {
                sensorsSum += s.rightWidthPercent + s.rightSpanPercent;
            }
        }

        float checksum = t.rotation + scaleX + scaleY + t.x + t.y + t.originX + t.originY + d.width + d.height + sensorsSum;
        while (parentNode != null && parentNode.parentEntity != -1) {
            TransformComponent pt = transformMapper.get(parentNode.parentEntity);
            DimensionsComponent dt = dimensionsMapper.get(parentNode.parentEntity);
            if (pt == null || dt == null)
                break;
            SensorComponent ds = sensorMapper.get(parentNode.parentEntity);
            float pScaleX = pt.scaleX * (pt.flipX ? -1 : 1);
            float pScaleY = pt.scaleY * (pt.flipY ? -1 : 1);

            float pSensorsSum = 0;
            if (ds != null) {
                if (ds.top) {
                    pSensorsSum += ds.topHeightPercent + ds.topSpanPercent;
                }
                if (ds.bottom) {
                    pSensorsSum += ds.bottomHeightPercent + ds.bottomSpanPercent;
                }
                if (ds.left) {
                    pSensorsSum += ds.leftWidthPercent + ds.leftSpanPercent;
                }
                if (ds.right) {
                    pSensorsSum += ds.rightWidthPercent + ds.rightSpanPercent;
                }
            }

            checksum += pt.rotation + pScaleX + pScaleY + pt.x + pt.y + pt.originX + pt.originY + dt.width + dt.height + pSensorsSum;
            parentNode = parentNodeMapper.get(parentNode.parentEntity);
        }
        return checksum;
    }
}
