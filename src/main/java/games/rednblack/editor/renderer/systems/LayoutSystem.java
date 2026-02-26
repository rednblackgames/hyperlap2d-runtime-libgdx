package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Pool;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;

/**
 * Processes layout constraints using topological ordering to ensure
 * dependencies are resolved before dependents.
 *
 * Circular dependencies are detected via Kahn's algorithm; affected
 * entities fall back to parent-only constraints for the cycle edges,
 * guaranteeing stable positioning without cross-frame oscillation.
 *
 * A per-entity checksum (similar to {@link BoundingBoxSystem}) skips
 * recalculation when none of the inputs have changed.
 *
 * Constraints are resolved relative to parent-local axis-aligned
 * bounding boxes (AABB) from {@link BoundingBoxComponent#parentLocalAABB}
 * so that rotation and scale are properly accounted for.
 *
 * Constraint resolution (uniqueId -> entityId) and topological sort are
 * performed once on entity insert/remove rather than every frame.
 */
@All(LayoutComponent.class)
public class LayoutSystem extends BaseEntitySystem {

    protected ComponentMapper<LayoutComponent> layoutMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;

    // Topological sort structures – reused to avoid allocations
    private final IntArray sortedEntities = new IntArray();
    private final IntArray queue = new IntArray();
    private final IntIntMap inDegree = new IntIntMap();
    private final IntMap<IntArray> reverseDeps = new IntMap<>();
    private final IntSet activeSet = new IntSet();
    private final IntSet inCycle = new IntSet();
    private final IntSet depTargets = new IntSet();
    private final Pool<IntArray> intArrayPool = new Pool<IntArray>() {
        @Override
        protected IntArray newObject() {
            return new IntArray(4);
        }

        @Override
        protected void reset(IntArray object) {
            object.clear();
        }
    };

    // Only rebuild topo order and resolve on insert/remove
    private boolean dirty = true;
    private final IntSet unresolvedEntities = new IntSet();

    // ----------------------------------------------------------------
    // Entity lifecycle callbacks
    // ----------------------------------------------------------------

    @Override
    protected void inserted(int entityId) {
        LayoutComponent layout = layoutMapper.get(entityId);
        resolveConstraints(layout, entityId);
        if (hasUnresolved(layout)) {
            unresolvedEntities.add(entityId);
        }

        // New entity might be a target for previously unresolved constraints
        retryUnresolved();

        dirty = true;
    }

    @Override
    protected void removed(int entityId) {
        unresolvedEntities.remove(entityId);

        // Use reverseDeps for O(1) lookup instead of scanning all entities
        IntArray dependents = reverseDeps.get(entityId);
        if (dependents != null) {
            for (int i = 0; i < dependents.size; i++) {
                int dep = dependents.get(i);
                LayoutComponent layout = layoutMapper.get(dep);
                if (layout == null) continue;
                if (layout.left != null && layout.left.targetEntity == entityId) layout.left = null;
                if (layout.right != null && layout.right.targetEntity == entityId) layout.right = null;
                if (layout.top != null && layout.top.targetEntity == entityId) layout.top = null;
                if (layout.bottom != null && layout.bottom.targetEntity == entityId) layout.bottom = null;
            }
        }

        dirty = true;
    }

    // ----------------------------------------------------------------
    // Main processing
    // ----------------------------------------------------------------

    @Override
    protected void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        int size = actives.size();

        if (size == 0) return;

        // Retry any unresolved constraints (may occur on load order)
        if (!unresolvedEntities.isEmpty()) {
            retryUnresolved();
            dirty = true;
        }

        // Rebuild topological order only when entity composition changed
        if (dirty) {
            buildTopologicalOrder(ids, size);
            dirty = false;
        }

        // Process in dependency order with checksum-based skip
        for (int i = 0; i < sortedEntities.size; i++) {
            processEntity(sortedEntities.get(i), false);
        }

        // Process cycle entities – ignore sibling deps on other
        // cycle members to prevent cross-frame oscillation
        if (inCycle.size > 0) {
            for (int i = 0; i < size; i++) {
                if (inCycle.contains(ids[i])) {
                    processEntity(ids[i], true);
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Constraint resolution helpers
    // ----------------------------------------------------------------

    private boolean hasUnresolved(LayoutComponent layout) {
        return isUnresolved(layout.left) || isUnresolved(layout.right)
            || isUnresolved(layout.top) || isUnresolved(layout.bottom);
    }

    private boolean isUnresolved(LayoutComponent.ConstraintData data) {
        return data != null && !data.resolved;
    }

    private void retryUnresolved() {
        IntSet.IntSetIterator iter = unresolvedEntities.iterator();
        while (iter.hasNext) {
            int entityId = iter.next();
            LayoutComponent layout = layoutMapper.get(entityId);
            if (layout == null) { iter.remove(); continue; }
            resolveConstraints(layout, entityId);
            if (!hasUnresolved(layout)) {
                iter.remove();
            }
        }
    }

    // ----------------------------------------------------------------
    // Topological sort (Kahn's algorithm)
    // ----------------------------------------------------------------

    private void buildTopologicalOrder(int[] ids, int size) {
        sortedEntities.clear();
        inCycle.clear();
        activeSet.clear();
        inDegree.clear();
        queue.clear();

        // Return IntArrays to pool and clear the map
        for (IntMap.Entry<IntArray> entry : reverseDeps) {
            intArrayPool.free(entry.value);
        }
        reverseDeps.clear();

        // Build active set for O(1) membership tests
        for (int i = 0; i < size; i++) {
            activeSet.add(ids[i]);
        }

        // Compute in-degrees (distinct dependency count) and reverse edges
        for (int i = 0; i < size; i++) {
            int entity = ids[i];
            LayoutComponent layout = layoutMapper.get(entity);

            depTargets.clear();
            collectTarget(layout.left);
            collectTarget(layout.right);
            collectTarget(layout.top);
            collectTarget(layout.bottom);

            inDegree.put(entity, depTargets.size);

            IntSet.IntSetIterator iter = depTargets.iterator();
            while (iter.hasNext) {
                int target = iter.next();
                IntArray deps = reverseDeps.get(target);
                if (deps == null) {
                    deps = intArrayPool.obtain();
                    reverseDeps.put(target, deps);
                }
                deps.add(entity);
            }
        }

        // Seed the queue with zero in-degree entities (parent-only)
        for (int i = 0; i < size; i++) {
            if (inDegree.get(ids[i], 0) == 0) {
                queue.add(ids[i]);
            }
        }

        // BFS
        while (queue.size > 0) {
            int entity = queue.pop();
            sortedEntities.add(entity);

            IntArray deps = reverseDeps.get(entity);
            if (deps != null) {
                for (int j = 0; j < deps.size; j++) {
                    int dependent = deps.get(j);
                    int newDeg = inDegree.get(dependent, 1) - 1;
                    inDegree.put(dependent, newDeg);
                    if (newDeg == 0) {
                        queue.add(dependent);
                    }
                }
            }
        }

        // Entities still with in-degree > 0 are in or blocked by cycles
        if (sortedEntities.size < size) {
            for (int i = 0; i < size; i++) {
                if (inDegree.get(ids[i], 0) > 0) {
                    inCycle.add(ids[i]);
                }
            }
        }
    }

    private void collectTarget(LayoutComponent.ConstraintData data) {
        if (data != null && data.targetEntity != -1 && activeSet.contains(data.targetEntity)) {
            depTargets.add(data.targetEntity);
        }
    }

    // ----------------------------------------------------------------
    // Per-entity layout calculation
    // ----------------------------------------------------------------

    private void processEntity(int entity, boolean skipCycleDeps) {
        ParentNodeComponent parentNode = parentNodeMapper.get(entity);
        if (parentNode == null) return;
        int parent = parentNode.parentEntity;
        if (parent == -1) return;

        DimensionsComponent parentDimensions = dimensionsMapper.get(parent);
        if (parentDimensions == null) return;

        LayoutComponent layout = layoutMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);
        DimensionsComponent dimensions = dimensionsMapper.get(entity);
        BoundingBoxComponent bb = boundingBoxMapper.get(entity);
        MainItemComponent mic = mainItemMapper.get(entity);
        boolean visible = mic == null || mic.visible;

        // Checksum-based skip: if none of the inputs changed, skip recalculation
        int checksum = calcChecksum(layout, transform, dimensions, parentDimensions, bb, skipCycleDeps, visible);
        if (checksum == layout.checksum) return;

        // Invisible entities collapse their AABB to zero so siblings
        // constrained to them see a zero-size anchor point.
        processHorizontal(layout, transform, dimensions, bb, parentDimensions, skipCycleDeps, visible);
        processVertical(layout, transform, dimensions, bb, parentDimensions, skipCycleDeps, visible);

        layout.checksum = calcChecksum(layout, transform, dimensions, parentDimensions, bb, skipCycleDeps, visible);
    }

    private void processHorizontal(LayoutComponent layout, TransformComponent transform,
                                    DimensionsComponent dimensions, BoundingBoxComponent bb,
                                    DimensionsComponent parentDimensions, boolean skipCycleDeps,
                                    boolean visible) {
        float aabbLeft, aabbRight, aabbWidth;
        if (!visible) {
            aabbLeft = 0; aabbRight = 0; aabbWidth = 0;
        } else {
            aabbLeft = bb != null ? bb.parentLocalAABB.x : 0;
            aabbRight = bb != null ? bb.parentLocalAABB.x + bb.parentLocalAABB.width : dimensions.width;
            aabbWidth = aabbRight - aabbLeft;
        }

        float leftAnchor = resolveAnchor(layout.left, parentDimensions, skipCycleDeps);
        float rightAnchor = resolveAnchor(layout.right, parentDimensions, skipCycleDeps);

        if (!Float.isNaN(leftAnchor) && !Float.isNaN(rightAnchor)) {
            float leftPos = leftAnchor + layout.left.margin;
            float rightPos = rightAnchor - layout.right.margin;
            float availableSpace = rightPos - leftPos - aabbWidth;
            transform.x = leftPos - aabbLeft + availableSpace * layout.horizontalBias;
        } else if (!Float.isNaN(leftAnchor)) {
            transform.x = leftAnchor + layout.left.margin - aabbLeft;
        } else if (!Float.isNaN(rightAnchor)) {
            transform.x = rightAnchor - layout.right.margin - aabbRight;
        }
    }

    private void processVertical(LayoutComponent layout, TransformComponent transform,
                                  DimensionsComponent dimensions, BoundingBoxComponent bb,
                                  DimensionsComponent parentDimensions, boolean skipCycleDeps,
                                  boolean visible) {
        float aabbBottom, aabbTop, aabbHeight;
        if (!visible) {
            aabbBottom = 0; aabbTop = 0; aabbHeight = 0;
        } else {
            aabbBottom = bb != null ? bb.parentLocalAABB.y : 0;
            aabbTop = bb != null ? bb.parentLocalAABB.y + bb.parentLocalAABB.height : dimensions.height;
            aabbHeight = aabbTop - aabbBottom;
        }

        float bottomAnchor = resolveAnchor(layout.bottom, parentDimensions, skipCycleDeps);
        float topAnchor = resolveAnchor(layout.top, parentDimensions, skipCycleDeps);

        if (!Float.isNaN(bottomAnchor) && !Float.isNaN(topAnchor)) {
            float bottomPos = bottomAnchor + layout.bottom.margin;
            float topPos = topAnchor - layout.top.margin;
            float availableSpace = topPos - bottomPos - aabbHeight;
            transform.y = bottomPos - aabbBottom + availableSpace * layout.verticalBias;
        } else if (!Float.isNaN(bottomAnchor)) {
            transform.y = bottomAnchor + layout.bottom.margin - aabbBottom;
        } else if (!Float.isNaN(topAnchor)) {
            transform.y = topAnchor - layout.top.margin - aabbTop;
        }
    }

    // ----------------------------------------------------------------
    // Anchor resolution
    // ----------------------------------------------------------------

    private float resolveAnchor(LayoutComponent.ConstraintData data, DimensionsComponent parentDimensions,
                                 boolean skipCycleDeps) {
        if (data == null) return Float.NaN;

        if (data.targetEntity == -1) {
            return resolveParentSide(data.targetSide, parentDimensions);
        } else {
            // Skip sibling edges that participate in a cycle
            if (skipCycleDeps && inCycle.contains(data.targetEntity)) return Float.NaN;
            if (!engine.getEntityManager().isActive(data.targetEntity)) return Float.NaN;

            TransformComponent siblingTransform = transformMapper.get(data.targetEntity);
            DimensionsComponent siblingDimensions = dimensionsMapper.get(data.targetEntity);
            if (siblingTransform == null || siblingDimensions == null) return Float.NaN;

            return resolveSiblingSide(data.targetSide, data.targetEntity, siblingTransform, siblingDimensions);
        }
    }

    private float resolveParentSide(LayoutComponent.ConstraintSide side, DimensionsComponent parentDimensions) {
        if (side == null) return Float.NaN;
        switch (side) {
            case LEFT:
            case BOTTOM:
                return 0f;
            case RIGHT:
                return parentDimensions.width;
            case TOP:
                return parentDimensions.height;
            default:
                return Float.NaN;
        }
    }

    private float resolveSiblingSide(LayoutComponent.ConstraintSide side, int siblingEntity,
                                      TransformComponent siblingTransform,
                                      DimensionsComponent siblingDimensions) {
        if (side == null) return Float.NaN;

        // Invisible siblings collapse to zero so dependents see a point anchor.
        MainItemComponent sibMic = mainItemMapper.get(siblingEntity);
        boolean sibVisible = sibMic == null || sibMic.visible;

        float left, bottom, right, top;
        if (!sibVisible) {
            left = 0; bottom = 0; right = 0; top = 0;
        } else {
            BoundingBoxComponent sibBB = boundingBoxMapper.get(siblingEntity);
            if (sibBB != null) {
                left = sibBB.parentLocalAABB.x;
                bottom = sibBB.parentLocalAABB.y;
                right = left + sibBB.parentLocalAABB.width;
                top = bottom + sibBB.parentLocalAABB.height;
            } else {
                left = 0; bottom = 0;
                right = siblingDimensions.width;
                top = siblingDimensions.height;
            }
        }

        switch (side) {
            case LEFT:
                return siblingTransform.x + left;
            case RIGHT:
                return siblingTransform.x + right;
            case BOTTOM:
                return siblingTransform.y + bottom;
            case TOP:
                return siblingTransform.y + top;
            default:
                return Float.NaN;
        }
    }

    // ----------------------------------------------------------------
    // Checksum – mirrors the pattern used in BoundingBoxSystem
    // ----------------------------------------------------------------

    private int calcChecksum(LayoutComponent layout, TransformComponent transform,
                              DimensionsComponent dimensions, DimensionsComponent parentDimensions,
                              BoundingBoxComponent bb, boolean skipCycleDeps, boolean visible) {
        float scaleX = transform.scaleX * (transform.flipX ? -1 : 1);
        float scaleY = transform.scaleY * (transform.flipY ? -1 : 1);
        int cs = (visible ? 0 : 137)
                + Float.floatToRawIntBits(layout.horizontalBias) * 3
                + Float.floatToRawIntBits(layout.verticalBias) * 7
                + Float.floatToRawIntBits(dimensions.width) * 11
                + Float.floatToRawIntBits(dimensions.height) * 13
                + Float.floatToRawIntBits(parentDimensions.width) * 17
                + Float.floatToRawIntBits(parentDimensions.height) * 19
                + Float.floatToRawIntBits(transform.x) * 71
                + Float.floatToRawIntBits(transform.y) * 73
                + Float.floatToRawIntBits(transform.rotation) * 83
                + Float.floatToRawIntBits(scaleX) * 89
                + Float.floatToRawIntBits(scaleY) * 97
                + Float.floatToRawIntBits(transform.originX) * 101
                + Float.floatToRawIntBits(transform.originY) * 103;
        if (bb != null) cs += bb.checksum * 79;

        cs += constraintChecksum(layout.left, skipCycleDeps) * 23;
        cs += constraintChecksum(layout.right, skipCycleDeps) * 29;
        cs += constraintChecksum(layout.top, skipCycleDeps) * 31;
        cs += constraintChecksum(layout.bottom, skipCycleDeps) * 37;

        return cs;
    }

    private int constraintChecksum(LayoutComponent.ConstraintData data, boolean skipCycleDeps) {
        if (data == null) return 0;

        int cs = Float.floatToRawIntBits(data.margin) * 41 + data.targetEntity * 43;
        if (data.targetSide != null) cs += data.targetSide.ordinal() * 47;

        // Include sibling spatial data so any position/size/rotation change propagates.
        // The BoundingBoxComponent checksum captures transform changes across the
        // entire parent chain (rotation, scale, origin, etc.) that raw x/y alone miss.
        if (data.targetEntity != -1 && !(skipCycleDeps && inCycle.contains(data.targetEntity))) {
            MainItemComponent sibMic = mainItemMapper.get(data.targetEntity);
            if (sibMic != null && !sibMic.visible) cs += 139;
            TransformComponent st = transformMapper.get(data.targetEntity);
            DimensionsComponent sd = dimensionsMapper.get(data.targetEntity);
            if (st != null && sd != null) {
                float stScaleX = st.scaleX * (st.flipX ? -1 : 1);
                float stScaleY = st.scaleY * (st.flipY ? -1 : 1);
                cs += Float.floatToRawIntBits(st.x) * 53
                    + Float.floatToRawIntBits(st.y) * 59
                    + Float.floatToRawIntBits(sd.width) * 61
                    + Float.floatToRawIntBits(sd.height) * 67
                    + Float.floatToRawIntBits(st.rotation) * 107
                    + Float.floatToRawIntBits(stScaleX) * 109
                    + Float.floatToRawIntBits(stScaleY) * 113
                    + Float.floatToRawIntBits(st.originX) * 127
                    + Float.floatToRawIntBits(st.originY) * 131;
            }
            BoundingBoxComponent bb = boundingBoxMapper.get(data.targetEntity);
            if (bb != null) {
                cs += bb.checksum * 71;
            }
        }

        return cs;
    }

    // ----------------------------------------------------------------
    // Lazy constraint resolution (uniqueId -> entityId)
    // ----------------------------------------------------------------

    private void resolveConstraints(LayoutComponent layout, int entity) {
        resolveConstraintData(layout.left, entity);
        resolveConstraintData(layout.right, entity);
        resolveConstraintData(layout.top, entity);
        resolveConstraintData(layout.bottom, entity);
    }

    private void resolveConstraintData(LayoutComponent.ConstraintData data, int entity) {
        if (data == null || data.resolved) return;

        if (data.targetUniqueId == null) {
            data.targetEntity = -1;
            data.resolved = true;
        } else {
            int resolved = findEntityByUniqueId(data.targetUniqueId, entity);
            if (resolved != -1) {
                data.targetEntity = resolved;
                data.resolved = true;
            }
        }
    }

    private int findEntityByUniqueId(String uniqueId, int requestingEntity) {
        ParentNodeComponent pnc = parentNodeMapper.get(requestingEntity);
        if (pnc == null) return -1;
        int parent = pnc.parentEntity;
        if (parent == -1) return -1;
        NodeComponent nc = nodeMapper.get(parent);
        if (nc == null) return -1;

        for (int child : nc.children) {
            MainItemComponent mic = mainItemMapper.get(child);
            if (mic != null && uniqueId.equals(mic.uniqueId)) {
                return child;
            }
        }
        return -1;
    }
}
