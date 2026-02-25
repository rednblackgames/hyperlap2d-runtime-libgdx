package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
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

    // Topological sort structures – reused each frame to avoid allocations
    private final IntArray sortedEntities = new IntArray();
    private final IntArray queue = new IntArray();
    private final IntIntMap inDegree = new IntIntMap();
    private final IntMap<IntArray> reverseDeps = new IntMap<>();
    private final IntSet activeSet = new IntSet();
    private final IntSet inCycle = new IntSet();
    private final IntSet depTargets = new IntSet();

    @Override
    protected void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        int size = actives.size();

        if (size == 0) return;

        // Phase 1: Resolve lazy constraint targets (uniqueId -> entityId)
        for (int i = 0; i < size; i++) {
            resolveConstraints(layoutMapper.get(ids[i]), ids[i]);
        }

        // Phase 2: Build dependency graph and topological sort
        buildTopologicalOrder(ids, size);

        // Phase 3: Process in dependency order with checksum-based skip
        for (int i = 0; i < sortedEntities.size; i++) {
            processEntity(sortedEntities.get(i), false);
        }

        // Phase 4: Process cycle entities – ignore sibling deps on other
        // cycle members to prevent cross-frame oscillation
        if (sortedEntities.size < size) {
            for (int i = 0; i < size; i++) {
                if (inCycle.contains(ids[i])) {
                    processEntity(ids[i], true);
                }
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

        // Clear reused IntArrays in the reverse-dependency map
        for (IntMap.Entry<IntArray> entry : reverseDeps) {
            entry.value.clear();
        }

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
                    deps = new IntArray(4);
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

        // Checksum-based skip: if none of the inputs changed, skip recalculation
        float checksum = calcChecksum(layout, transform, dimensions, parentDimensions, bb, skipCycleDeps);
        if (checksum == layout.checksum) return;

        processHorizontal(layout, transform, dimensions, parentDimensions, skipCycleDeps);
        processVertical(layout, transform, dimensions, parentDimensions, skipCycleDeps);

        // Recompute AFTER processing so the stored checksum includes the
        // updated x/y.  This way next frame's pre-check matches immediately
        // instead of lagging one frame behind the BoundingBoxSystem update.
        layout.checksum = calcChecksum(layout, transform, dimensions, parentDimensions, bb, skipCycleDeps);
    }

    private void processHorizontal(LayoutComponent layout, TransformComponent transform,
                                    DimensionsComponent dimensions, DimensionsComponent parentDimensions,
                                    boolean skipCycleDeps) {
        float leftAnchor = resolveAnchor(layout.left, parentDimensions, skipCycleDeps);
        float rightAnchor = resolveAnchor(layout.right, parentDimensions, skipCycleDeps);

        if (!Float.isNaN(leftAnchor) && !Float.isNaN(rightAnchor)) {
            float leftPos = leftAnchor + layout.left.margin;
            float rightPos = rightAnchor - layout.right.margin;
            float availableSpace = rightPos - leftPos - dimensions.width;
            transform.x = leftPos + availableSpace * layout.horizontalBias;
        } else if (!Float.isNaN(leftAnchor)) {
            transform.x = leftAnchor + layout.left.margin;
        } else if (!Float.isNaN(rightAnchor)) {
            transform.x = rightAnchor - layout.right.margin - dimensions.width;
        }
    }

    private void processVertical(LayoutComponent layout, TransformComponent transform,
                                  DimensionsComponent dimensions, DimensionsComponent parentDimensions,
                                  boolean skipCycleDeps) {
        float bottomAnchor = resolveAnchor(layout.bottom, parentDimensions, skipCycleDeps);
        float topAnchor = resolveAnchor(layout.top, parentDimensions, skipCycleDeps);

        if (!Float.isNaN(bottomAnchor) && !Float.isNaN(topAnchor)) {
            float bottomPos = bottomAnchor + layout.bottom.margin;
            float topPos = topAnchor - layout.top.margin;
            float availableSpace = topPos - bottomPos - dimensions.height;
            transform.y = bottomPos + availableSpace * layout.verticalBias;
        } else if (!Float.isNaN(bottomAnchor)) {
            transform.y = bottomAnchor + layout.bottom.margin;
        } else if (!Float.isNaN(topAnchor)) {
            transform.y = topAnchor - layout.top.margin - dimensions.height;
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

            return resolveSiblingSide(data.targetSide, siblingTransform, siblingDimensions);
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

    private float resolveSiblingSide(LayoutComponent.ConstraintSide side,
                                      TransformComponent siblingTransform,
                                      DimensionsComponent siblingDimensions) {
        if (side == null) return Float.NaN;
        switch (side) {
            case LEFT:
                return siblingTransform.x;
            case RIGHT:
                return siblingTransform.x + siblingDimensions.width;
            case BOTTOM:
                return siblingTransform.y;
            case TOP:
                return siblingTransform.y + siblingDimensions.height;
            default:
                return Float.NaN;
        }
    }

    // ----------------------------------------------------------------
    // Checksum – mirrors the pattern used in BoundingBoxSystem
    // ----------------------------------------------------------------

    private float calcChecksum(LayoutComponent layout, TransformComponent transform,
                                DimensionsComponent dimensions, DimensionsComponent parentDimensions,
                                BoundingBoxComponent bb, boolean skipCycleDeps) {
        float cs = layout.horizontalBias * 3 + layout.verticalBias * 7
                 + dimensions.width * 11 + dimensions.height * 13
                 + parentDimensions.width * 17 + parentDimensions.height * 19
                 + transform.x * 71 + transform.y * 73;
        if (bb != null) cs += bb.checksum * 79;

        cs += constraintChecksum(layout.left, skipCycleDeps) * 23;
        cs += constraintChecksum(layout.right, skipCycleDeps) * 29;
        cs += constraintChecksum(layout.top, skipCycleDeps) * 31;
        cs += constraintChecksum(layout.bottom, skipCycleDeps) * 37;

        return cs;
    }

    private float constraintChecksum(LayoutComponent.ConstraintData data, boolean skipCycleDeps) {
        if (data == null) return 0;

        float cs = data.margin * 41 + data.targetEntity * 43;
        if (data.targetSide != null) cs += data.targetSide.ordinal() * 47;

        // Include sibling spatial data so any position/size change propagates.
        // The BoundingBoxComponent checksum captures transform changes across the
        // entire parent chain (rotation, scale, origin, etc.) that raw x/y alone miss.
        if (data.targetEntity != -1 && !(skipCycleDeps && inCycle.contains(data.targetEntity))) {
            TransformComponent st = transformMapper.get(data.targetEntity);
            DimensionsComponent sd = dimensionsMapper.get(data.targetEntity);
            if (st != null && sd != null) {
                cs += st.x * 53 + st.y * 59 + sd.width * 61 + sd.height * 67;
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
