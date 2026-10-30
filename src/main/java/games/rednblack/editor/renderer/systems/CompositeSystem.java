package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.FloatArray;
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
    protected ComponentMapper<AutoLayoutComponent> autoLayoutMapper;
    protected ComponentMapper<LayoutComponent> layoutMapper;

    private DimensionsComponent dimensionsComponent;
    private NodeComponent nodeComponent;

    private final Vector2 p1 = new Vector2();
    private final Vector2 p2 = new Vector2();
    private final Vector2 p3 = new Vector2();
    private final Vector2 p4 = new Vector2();

    private final FloatArray bounds = new FloatArray();             // each child's box: left, bottom, right, top
    private final BooleanArray lowFollows = new BooleanArray();
    private final BooleanArray highFollows = new BooleanArray();

    @Override
    public void process(int entity) {
        dimensionsComponent = dimensionsMapper.get(entity);
        nodeComponent = nodeMapper.get(entity);
        TransformComponent transformComponent = transformMapper.get(entity);

        CompositeTransformComponent compositeTransformComponent = compositeMapper.get(entity);
        ViewPortComponent viewPortComponent = viewPortComponentMapper.get(entity);
        if (compositeTransformComponent != null) {

            // an auto layout decides its own size, and would have its children moved back otherwise
            if (compositeTransformComponent.automaticResize && viewPortComponent == null && !autoLayoutMapper.has(entity)) {
                int checksum = calcCheckSum(transformComponent);
                if (checksum != compositeTransformComponent.checksum) {
                    compositeTransformComponent.checksum = checksum;
                    recalculateSize();
                }
            }
        }
    }

    private int calcCheckSum(TransformComponent t) {
        DimensionsComponent d = dimensionsComponent;

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
                + Float.floatToRawIntBits(d.height) * 29;
        Integer[] children = nodeComponent.children.begin();
        for (int i = 0, n = nodeComponent.children.size; i < n; i++) {
            Integer child = children[i];
            TransformComponent pt = transformMapper.get(child);
            DimensionsComponent dt = dimensionsMapper.get(child);
            if (pt == null || dt == null)
                continue;
            float pScaleX = pt.scaleX * (pt.flipX ? -1 : 1);
            float pScaleY = pt.scaleY * (pt.flipY ? -1 : 1);
            checksum += Float.floatToRawIntBits(pt.rotation) * 31
                    + Float.floatToRawIntBits(pScaleX) * 37
                    + Float.floatToRawIntBits(pScaleY) * 41
                    + Float.floatToRawIntBits(pt.x) * 43
                    + Float.floatToRawIntBits(pt.y) * 47
                    + Float.floatToRawIntBits(pt.originX) * 53
                    + Float.floatToRawIntBits(pt.originY) * 59
                    + Float.floatToRawIntBits(dt.width) * 61
                    + Float.floatToRawIntBits(dt.height) * 67;
        }
        nodeComponent.children.end();
        return checksum;
    }

    /**
     * As large as the children. On an axis where children are laid out against the composite, the
     * composite's box is what places them: it keeps its origin, and the edges that follow its size
     * (pinned to its right or top, or to a sibling's edge that is) are left out of it, or the two
     * would push each other out forever, as a background stretched past the edges with a negative
     * margin does.
     */
    private void recalculateSize() {
        SnapshotArray<Integer> entities = nodeComponent.children;
        int n = entities.size;
        Integer[] children = entities.begin();
        bounds.setSize(n * 4);
        float[] box = bounds.items;
        for (int i = 0; i < n; i++) {
            TransformComponent transformComponent = transformMapper.get(children[i]);
            DimensionsComponent childDimCom = dimensionsMapper.get(children[i]);
            float x = transformComponent.x;
            float y = transformComponent.y;

            Matrix3 transMat = TransformMathUtils.transform(transformComponent);

            p1.set(x, y).mul(transMat);
            p2.set(x + childDimCom.width, y).mul(transMat);
            p3.set(x + childDimCom.width, y + childDimCom.height).mul(transMat);
            p4.set(x, y + childDimCom.height).mul(transMat);
            box[i * 4] = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
            box[i * 4 + 1] = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
            box[i * 4 + 2] = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
            box[i * 4 + 3] = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));
        }

        boolean anchoredX = followers(children, n, true);
        float lowerX = Float.MAX_VALUE, upperX = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (anchoredX) {
                if (!highFollows.get(i)) upperX = Math.max(upperX, box[i * 4 + 2]);
            } else {
                lowerX = Math.min(lowerX, box[i * 4]);
                upperX = Math.max(upperX, box[i * 4 + 2]);
            }
        }
        boolean anchoredY = followers(children, n, false);
        float lowerY = Float.MAX_VALUE, upperY = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (anchoredY) {
                if (!highFollows.get(i)) upperY = Math.max(upperY, box[i * 4 + 3]);
            } else {
                lowerY = Math.min(lowerY, box[i * 4 + 1]);
                upperY = Math.max(upperY, box[i * 4 + 3]);
            }
        }
        if (n == 0) lowerX = upperX = lowerY = upperY = 0;
        if (anchoredX) lowerX = 0;
        if (anchoredY) lowerY = 0;

        if (lowerX != 0 || lowerY != 0) {
            for (int i = 0; i < n; i++) {
                TransformComponent transformComponent = transformMapper.get(children[i]);
                transformComponent.x -= lowerX;
                transformComponent.y -= lowerY;
            }
        }
        entities.end();

        // every edge follows the composite: nothing to measure, the size stays as it is
        if (upperX != -Float.MAX_VALUE) dimensionsComponent.width = Math.max((upperX - lowerX), 0);
        if (upperY != -Float.MAX_VALUE) dimensionsComponent.height = Math.max((upperY - lowerY), 0);
        dimensionsComponent.boundBox.set(0, 0, dimensionsComponent.width, dimensionsComponent.height);
    }

    /**
     * Which children's edges follow the composite's size on one axis, into lowFollows and
     * highFollows (left and right, or bottom and top). An edge follows when it is pinned to the
     * composite's far side, or to a sibling's edge that follows: worked out until nothing changes,
     * as a chain may run in any order.
     * @return whether any child is laid out against the composite on that axis
     */
    private boolean followers(Integer[] children, int n, boolean horizontal) {
        lowFollows.clear();
        highFollows.clear();
        for (int i = 0; i < n; i++) {
            lowFollows.add(false);
            highFollows.add(false);
        }
        boolean anchored = false;
        for (int i = 0; i < n; i++) {
            LayoutComponent layout = layoutMapper.get(children[i]);
            if (layout == null) continue;
            if (toParent(horizontal ? layout.left : layout.bottom) || toParent(horizontal ? layout.right : layout.top)) anchored = true;
        }
        if (!anchored) return false;

        boolean changed = true;
        for (int pass = 0; changed && pass <= n; pass++) {
            changed = false;
            for (int i = 0; i < n; i++) {
                LayoutComponent layout = layoutMapper.get(children[i]);
                if (layout == null) continue;
                LayoutComponent.ConstraintData low = horizontal ? layout.left : layout.bottom;
                LayoutComponent.ConstraintData high = horizontal ? layout.right : layout.top;
                boolean match = horizontal ? layout.matchConstraintWidth : layout.matchConstraintHeight;
                boolean lowAnchor = follows(low, children, n), highAnchor = follows(high, children, n);
                boolean lowEdge, highEdge;
                if (low != null && high != null && match) {      // stretched: each edge on its own anchor
                    lowEdge = lowAnchor;
                    highEdge = highAnchor;
                } else {                                         // placed: both edges move with any anchor
                    lowEdge = highEdge = lowAnchor || highAnchor;
                }
                if (lowEdge && !lowFollows.get(i)) { lowFollows.set(i, true); changed = true; }
                if (highEdge && !highFollows.get(i)) { highFollows.set(i, true); changed = true; }
            }
        }
        return true;
    }

    private static boolean toParent(LayoutComponent.ConstraintData data) {
        return data != null && data.resolved && data.targetEntity == -1;
    }

    /** Whether the edge this constraint points at moves with the composite's size. */
    private boolean follows(LayoutComponent.ConstraintData data, Integer[] children, int n) {
        if (data == null || !data.resolved || data.targetSide == null) return false;
        boolean far = data.targetSide == LayoutComponent.ConstraintSide.RIGHT || data.targetSide == LayoutComponent.ConstraintSide.TOP;
        if (data.targetEntity == -1) return far;
        for (int i = 0; i < n; i++) {
            if (children[i] == data.targetEntity) return far ? highFollows.get(i) : lowFollows.get(i);
        }
        return false;
    }
}
