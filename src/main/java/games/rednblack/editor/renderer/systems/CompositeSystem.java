package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.SnapshotArray;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

@All(CompositeTransformComponent.class)
public class CompositeSystem extends IteratingSystem {

    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<CompositeTransformComponent> compositeMapper;
    protected ComponentMapper<ViewPortComponent> viewPortComponentMapper;

    private DimensionsComponent dimensionsComponent;
    private NodeComponent nodeComponent;

    private final Vector2 p1 = new Vector2();
    private final Vector2 p2 = new Vector2();
    private final Vector2 p3 = new Vector2();
    private final Vector2 p4 = new Vector2();
    private final Vector2 tmpBoundPoints = new Vector2();

    private final Vector2[] tmpVectorArray = new Vector2[5];

    public CompositeSystem() {
        tmpVectorArray[0] = p1;
        tmpVectorArray[1] = p2;
        tmpVectorArray[2] = p3;
        tmpVectorArray[3] = p4;
        tmpVectorArray[4] = tmpBoundPoints;
    }

    @Override
    public void process(int entity) {
        dimensionsComponent = dimensionsMapper.get(entity);
        nodeComponent = nodeMapper.get(entity);
        TransformComponent transformComponent = transformMapper.get(entity);

        CompositeTransformComponent compositeTransformComponent = compositeMapper.get(entity);
        ViewPortComponent viewPortComponent = viewPortComponentMapper.get(entity);
        if (compositeTransformComponent != null) {

            if (compositeTransformComponent.automaticResize && viewPortComponent == null) {
                float checksum = calcCheckSum(transformComponent);
                if (checksum != compositeTransformComponent.checksum) {
                    compositeTransformComponent.checksum = checksum;
                    recalculateSize();
                }
            }

            compositeTransformComponent.clipBounds.set(transformComponent.x, transformComponent.y, dimensionsComponent.width, dimensionsComponent.height);
        }
    }

    private float calcCheckSum(TransformComponent t) {
        DimensionsComponent d = dimensionsComponent;

        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);

        float checksum = t.rotation + scaleX + scaleY + t.x + t.y + t.originX + t.originY + d.width + d.height;
        Integer[] children = nodeComponent.children.begin();
        for (int i = 0, n = nodeComponent.children.size; i < n; i++) {
            Integer child = children[i];
            TransformComponent pt = transformMapper.get(child);
            DimensionsComponent dt = dimensionsMapper.get(child);
            if (pt == null || dt == null)
                continue;
            float pScaleX = pt.scaleX * (pt.flipX ? -1 : 1);
            float pScaleY = pt.scaleY * (pt.flipY ? -1 : 1);
            checksum += pt.rotation + pScaleX + pScaleY + pt.x + pt.y + pt.originX + pt.originY + dt.width + dt.height;
        }
        nodeComponent.children.end();
        return checksum;
    }

    private void recalculateSize() {
        float lowerX = Float.MAX_VALUE;
        float lowerY = Float.MAX_VALUE;
        float upperX = Float.MIN_VALUE;
        float upperY = Float.MIN_VALUE;
        SnapshotArray<Integer> entities = nodeComponent.children;
        for (Integer entity : entities) {
            TransformComponent transformComponent = transformMapper.get(entity);
            DimensionsComponent childDimCom = dimensionsMapper.get(entity);
            float x = transformComponent.x;
            float y = transformComponent.y;
            float width = childDimCom.width;
            float height = childDimCom.height;

            Matrix3 transMat = TransformMathUtils.transform(transformComponent);

            p1.set(x, y).mul(transMat);
            p2.set(x + width, y).mul(transMat);
            p3.set(x + width, y + height).mul(transMat);
            p4.set(x, y + height).mul(transMat);

            tmpBoundPoints.set(lowerX, 0);
            lowerX = getX(MinMaxOp.MIN, tmpVectorArray);

            tmpBoundPoints.set(upperX, 0);
            upperX = getX(MinMaxOp.MAX, tmpVectorArray);

            tmpBoundPoints.set(0, lowerY);
            lowerY = getY(MinMaxOp.MIN, tmpVectorArray);

            tmpBoundPoints.set(0, upperY);
            upperY = getY(MinMaxOp.MAX, tmpVectorArray);
        }

        for (Integer entity : entities) {
            if (lowerX == 0 && lowerY == 0) break;
            TransformComponent transformComponent = transformMapper.get(entity);
            transformComponent.x -= lowerX;
            transformComponent.y -= lowerY;
        }

        dimensionsComponent.width = Math.max((upperX - lowerX), 0);
        dimensionsComponent.height = Math.max((upperY - lowerY), 0);
        lowerX = 0;
        lowerY = 0;
        dimensionsComponent.boundBox.set(lowerX, lowerY, dimensionsComponent.width, dimensionsComponent.height);
    }

    private float getX(MinMaxOp op, Vector2[] points) {
        float pointX = points[0].x;
        for (Vector2 point : points) {
            pointX = op.compare(pointX, point.x);
        }
        return pointX;
    }

    private float getY(MinMaxOp op, Vector2[] points) {
        float pointY = points[0].y;
        for (Vector2 point : points) {
            pointY = op.compare(pointY, point.y);
        }
        return pointY;
    }

    private enum MinMaxOp {
        MIN("<") {
            @Override
            public float compare(float a, float b) {
                return Math.min(a, b);
            }
        },

        MAX(">") {
            @Override
            public float compare(float a, float b) {
                return Math.max(a, b);
            }
        };

        private String minMaxOperator;

        MinMaxOp(String minMaxOperator) {
            this.minMaxOperator = minMaxOperator;
        }

        public abstract float compare(float a, float b);
    }
}
