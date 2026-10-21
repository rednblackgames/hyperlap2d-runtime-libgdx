package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Pool;
import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.AutoLayoutItemComponent;
import games.rednblack.editor.renderer.components.AutoLayoutOwnSizeComponent;
import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.utils.AutoLayoutSolver;

/**
 * Places the children of the composites that have an {@link AutoLayoutComponent}.
 *
 * A frame goes in three steps, since a composite's size can come from what it holds (it fits its
 * content) or from above (its constraints), and its children depend on it:
 * <ol>
 * <li>{@link AutoLayoutMeasureSystem}, before {@link LayoutSystem}: the composites that fit their
 * content take its size, the deepest first so a composite sees the measure of those it holds.</li>
 * <li>{@link LayoutSystem}: constraints, which see those sizes. It leaves alone the children of an
 * auto layout, which places them itself.</li>
 * <li>This system, after it: children are placed and sized, the outermost composites first so a
 * composite placed or stretched by its parent is laid out with its new size. When anything moved,
 * the constraints are resolved once more, so what is pinned inside a resized child follows in the
 * same frame.</li>
 * </ol>
 * Children are only written when they change, so a layout at rest costs a pass over its children.
 */
@All({AutoLayoutComponent.class, NodeComponent.class, DimensionsComponent.class})
public class AutoLayoutSystem extends BaseEntitySystem {

    private static final float EPSILON = 0.001f;
    private static final int MAX_DEPTH = 64;

    protected ComponentMapper<AutoLayoutComponent> autoLayoutMapper;
    protected ComponentMapper<AutoLayoutItemComponent> itemMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<ParentNodeComponent> parentMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;
    protected ComponentMapper<AutoLayoutOwnSizeComponent> ownSizeMapper;

    private final IntArray order = new IntArray();
    private final IntIntMap depths = new IntIntMap();
    private final Array<AutoLayoutSolver.Item> items = new Array<>(false, 16, AutoLayoutSolver.Item[]::new);
    private final IntArray itemEntities = new IntArray();
    private final Pool<AutoLayoutSolver.Item> itemPool = new Pool<AutoLayoutSolver.Item>() {
        @Override
        protected AutoLayoutSolver.Item newObject() {
            return new AutoLayoutSolver.Item();
        }
    };
    private final Vector2 measured = new Vector2();
    private final Rectangle footprint = new Rectangle();
    /** Set when a child got its own size back, which moves things the same as a resize. */
    private boolean sizesGivenBack;

    /** Sizes the composites that fit their content; called by {@link AutoLayoutMeasureSystem}. */
    public void measure() {
        sortByDepth(false);
        for (int i = 0; i < order.size; i++) {
            int entity = order.get(i);
            AutoLayoutComponent layout = autoLayoutMapper.get(entity);
            collectItems(entity, layout);
            AutoLayoutSolver.measure(layout, items, measured);
            freeItems();

            layout.measuredWidth = measured.x;
            layout.measuredHeight = measured.y;

            DimensionsComponent dimensions = dimensionsMapper.get(entity);
            boolean resized = false;
            if (layout.fitWidth && !sizedByParent(entity, true) && differs(dimensions.width, measured.x)) {
                dimensions.width = measured.x;
                resized = true;
            }
            if (layout.fitHeight && !sizedByParent(entity, false) && differs(dimensions.height, measured.y)) {
                dimensions.height = measured.y;
                resized = true;
            }
            if (resized) {
                refreshFootprint(entity);
                syncBoundBox(dimensions);
            }
        }
    }

    /** A composite's bound box is what the editor outlines it with: it follows the size the layout gives. */
    private static void syncBoundBox(DimensionsComponent dimensions) {
        if (dimensions.boundBox != null) dimensions.boundBox.set(0, 0, dimensions.width, dimensions.height);
    }

    @Override
    protected void processSystem() {
        sortByDepth(true);
        boolean changed = sizesGivenBack;
        sizesGivenBack = false;
        for (int i = 0; i < order.size; i++) {
            changed |= arrange(order.get(i));
        }

        if (changed) {
            LayoutSystem layoutSystem = engine.getSystem(LayoutSystem.class);
            if (layoutSystem != null) layoutSystem.process();
        }
    }

    /** @return whether a child was moved or resized */
    private boolean arrange(int entity) {
        AutoLayoutComponent layout = autoLayoutMapper.get(entity);
        DimensionsComponent dimensions = dimensionsMapper.get(entity);

        collectItems(entity, layout);
        AutoLayoutSolver.arrange(layout, dimensions.width, dimensions.height, items);

        boolean changed = false;
        for (int i = 0; i < items.size; i++) {
            changed |= place(itemEntities.get(i), items.get(i));
        }
        freeItems();
        return changed;
    }

    private boolean place(int child, AutoLayoutSolver.Item item) {
        TransformComponent transform = transformMapper.get(child);
        DimensionsComponent dimensions = dimensionsMapper.get(child);
        boolean changed = false;

        // Only an upright child can be given another size: a turned one would no longer fill its box.
        if (transform.rotation == 0) {
            float scaleX = Math.abs(transform.scaleX);
            float scaleY = Math.abs(transform.scaleY);
            boolean resized = false;
            if (scaleX > 0 && differs(item.outWidth, item.width)) {
                dimensions.width = Math.max(0, item.outWidth / scaleX);
                resized = true;
            }
            if (scaleY > 0 && differs(item.outHeight, item.height)) {
                dimensions.height = Math.max(0, item.outHeight / scaleY);
                resized = true;
            }
            if (resized) {
                refreshFootprint(child);
                if (autoLayoutMapper.has(child) || nodeMapper.has(child)) syncBoundBox(dimensions);
                changed = true;
            }
        }

        footprintOf(child, transform, dimensions);
        float x = item.x - footprint.x;
        float y = item.y - footprint.y;
        // a child the editor is inside keeps its place for when it comes out
        if (differs(transform.placedX(), x)) {
            transform.placeX(x);
            changed = true;
        }
        if (differs(transform.placedY(), y)) {
            transform.placeY(y);
            changed = true;
        }
        return changed;
    }

    /** Children the layout places, in the composite's order, as rectangles for the solver. */
    private void collectItems(int entity, AutoLayoutComponent layout) {
        NodeComponent node = nodeMapper.get(entity);
        Integer[] children = node.children.begin();
        for (int i = 0, n = node.children.size; i < n; i++) {
            int child = children[i];
            if (!isPlaced(child, layout)) {
                keepOwnSize(child, false, false);
                continue;
            }

            TransformComponent transform = transformMapper.get(child);
            boolean upright = transform.rotation == 0;
            AutoLayoutItemComponent settings = itemMapper.get(child);
            // a turned child keeps its size, so it cannot take a share of the room either
            float grow = settings != null && upright ? settings.grow : 0;
            AutoLayoutComponent.Align alignSelf = settings != null ? settings.alignSelf : null;
            boolean sizesWidth = upright && AutoLayoutSolver.sizesWidth(layout, grow, alignSelf);
            boolean sizesHeight = upright && AutoLayoutSolver.sizesHeight(layout, grow, alignSelf);
            keepOwnSize(child, sizesWidth, sizesHeight);

            footprintOf(child, transform, dimensionsMapper.get(child));
            AutoLayoutSolver.Item item = itemPool.obtain().set(footprint.width, footprint.height);
            item.grow = grow;
            item.alignSelf = alignSelf;

            // What a child the layout sizes needs: its measure when it fits its content, else its own size
            if (upright) {
                AutoLayoutComponent childLayout = autoLayoutMapper.get(child);
                AutoLayoutOwnSizeComponent own = ownSizeMapper.get(child);
                float scaleX = Math.abs(transform.scaleX), scaleY = Math.abs(transform.scaleY);
                if (sizesWidth) {
                    item.contentWidth = (childLayout != null && childLayout.fitWidth ? childLayout.measuredWidth : own.width) * scaleX;
                }
                if (sizesHeight) {
                    item.contentHeight = (childLayout != null && childLayout.fitHeight ? childLayout.measuredHeight : own.height) * scaleY;
                }
            } else {
                item.contentWidth = footprint.width;
                item.contentHeight = footprint.height;
            }

            items.add(item);
            itemEntities.add(child);
        }
        node.children.end();
    }

    /**
     * Keeps the child's own size on the axes the layout sizes, and gives it back on those it no
     * longer does, so a child stops being stretched the moment it is not asked to be.
     */
    private void keepOwnSize(int child, boolean sizesWidth, boolean sizesHeight) {
        DimensionsComponent dimensions = dimensionsMapper.get(child);
        if (dimensions == null) return;
        AutoLayoutOwnSizeComponent own = ownSizeMapper.get(child);
        if (own == null) {
            if (!sizesWidth && !sizesHeight) return;
            own = ownSizeMapper.create(child);
        }

        boolean gaveBack = false;
        if (sizesWidth) {
            if (Float.isNaN(own.width)) own.width = dimensions.width;
        } else if (!Float.isNaN(own.width)) {
            dimensions.width = own.width;
            own.width = Float.NaN;
            gaveBack = true;
        }
        if (sizesHeight) {
            if (Float.isNaN(own.height)) own.height = dimensions.height;
        } else if (!Float.isNaN(own.height)) {
            dimensions.height = own.height;
            own.height = Float.NaN;
            gaveBack = true;
        }

        if (gaveBack) {
            refreshFootprint(child);
            if (nodeMapper.has(child)) syncBoundBox(dimensions);
            sizesGivenBack = true;
        }
    }

    /** A composite that loses its layout gives its children their own sizes back. */
    @Override
    protected void removed(int entity) {
        NodeComponent node = nodeMapper.get(entity);
        if (node == null) return;
        Integer[] children = node.children.begin();
        for (int i = 0, n = node.children.size; i < n; i++) {
            keepOwnSize(children[i], false, false);
        }
        node.children.end();
    }

    private boolean isPlaced(int child, AutoLayoutComponent layout) {
        if (transformMapper.get(child) == null || dimensionsMapper.get(child) == null) return false;
        AutoLayoutItemComponent settings = itemMapper.get(child);
        if (settings != null && settings.ignore) return false;
        if (layout.skipInvisible) {
            MainItemComponent mainItem = mainItemMapper.get(child);
            if (mainItem != null && !mainItem.visible) return false;
        }
        return true;
    }

    /** Whether the entity's parent lays it out and gives it its size on that axis. */
    private boolean sizedByParent(int entity, boolean horizontal) {
        ParentNodeComponent parent = parentMapper.get(entity);
        if (parent == null || parent.parentEntity == -1) return false;
        AutoLayoutComponent parentLayout = autoLayoutMapper.get(parent.parentEntity);
        if (parentLayout == null || !isPlaced(entity, parentLayout)) return false;
        if (transformMapper.get(entity).rotation != 0) return false;

        AutoLayoutItemComponent settings = itemMapper.get(entity);
        float grow = settings == null ? 0 : settings.grow;
        AutoLayoutComponent.Align alignSelf = settings == null ? null : settings.alignSelf;
        return horizontal ? AutoLayoutSolver.sizesWidth(parentLayout, grow, alignSelf)
                : AutoLayoutSolver.sizesHeight(parentLayout, grow, alignSelf);
    }

    /** Where the child is in its parent, as an offset from its position, and how large. */
    private void footprintOf(int child, TransformComponent transform, DimensionsComponent dimensions) {
        BoundingBoxComponent box = boundingBoxMapper.get(child);
        if (box != null) footprint.set(box.parentLocalAABB);
        else footprint.set(0, 0, dimensions.width, dimensions.height);
    }

    private void refreshFootprint(int entity) {
        BoundingBoxComponent box = boundingBoxMapper.get(entity);
        if (box == null) return;
        DimensionsComponent dimensions = dimensionsMapper.get(entity);
        ParentLocalAABBSystem.computeParentLocalAABB(transformMapper.get(entity), dimensions.width, dimensions.height, box);
    }

    private void freeItems() {
        itemPool.freeAll(items);
        items.clear();
        itemEntities.clear();
    }

    /** The composites, outermost first or deepest first. */
    private void sortByDepth(boolean outermostFirst) {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        order.clear();
        depths.clear();
        for (int i = 0, n = actives.size(); i < n; i++) {
            int entity = ids[i];
            depths.put(entity, depthOf(entity));
            order.add(entity);
        }

        // insertion sort: there are few of them, and they are mostly in order already
        for (int i = 1; i < order.size; i++) {
            int entity = order.get(i);
            int depth = depths.get(entity, 0);
            int j = i - 1;
            while (j >= 0 && (outermostFirst ? depths.get(order.get(j), 0) > depth : depths.get(order.get(j), 0) < depth)) {
                order.set(j + 1, order.get(j));
                j--;
            }
            order.set(j + 1, entity);
        }
    }

    private int depthOf(int entity) {
        int depth = 0;
        ParentNodeComponent parent = parentMapper.get(entity);
        while (parent != null && parent.parentEntity != -1 && depth < MAX_DEPTH) {
            depth++;
            parent = parentMapper.get(parent.parentEntity);
        }
        return depth;
    }

    private static boolean differs(float a, float b) {
        return Math.abs(a - b) > EPSILON;
    }
}
