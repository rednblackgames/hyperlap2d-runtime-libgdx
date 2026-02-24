package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;

@All(LayoutComponent.class)
public class LayoutSystem extends BaseEntitySystem {

    protected ComponentMapper<LayoutComponent> layoutMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;

    @Override
    protected void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        int size = actives.size();

        // Pass 1: process entities constrained only to parent
        for (int i = 0; i < size; i++) {
            LayoutComponent layout = layoutMapper.get(ids[i]);
            if (isParentOnly(layout)) {
                processEntity(ids[i]);
            }
        }

        // Pass 2: process entities with sibling constraints (using updated sibling positions)
        for (int i = 0; i < size; i++) {
            LayoutComponent layout = layoutMapper.get(ids[i]);
            if (!isParentOnly(layout)) {
                processEntity(ids[i]);
            }
        }
    }

    private boolean isParentOnly(LayoutComponent layout) {
        return isParentTarget(layout.left) && isParentTarget(layout.right) &&
               isParentTarget(layout.top) && isParentTarget(layout.bottom);
    }

    private boolean isParentTarget(LayoutComponent.ConstraintData data) {
        return data == null || data.targetEntity == -1;
    }

    private void processEntity(int entity) {
        LayoutComponent layout = layoutMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);
        DimensionsComponent dimensions = dimensionsMapper.get(entity);
        ParentNodeComponent parentNode = parentNodeMapper.get(entity);

        if (parentNode == null) return;
        int parent = parentNode.parentEntity;
        if (parent == -1) return;

        DimensionsComponent parentDimensions = dimensionsMapper.get(parent);
        if (parentDimensions == null) return;

        // Resolve lazy constraint targets
        resolveConstraints(layout, entity);

        // Horizontal axis
        processHorizontal(layout, transform, dimensions, parentDimensions);

        // Vertical axis
        processVertical(layout, transform, dimensions, parentDimensions);
    }

    private void processHorizontal(LayoutComponent layout, TransformComponent transform,
                                    DimensionsComponent dimensions, DimensionsComponent parentDimensions) {
        Float leftAnchor = resolveAnchor(layout.left, parentDimensions);
        Float rightAnchor = resolveAnchor(layout.right, parentDimensions);

        if (leftAnchor != null && rightAnchor != null) {
            float leftPos = leftAnchor + layout.left.margin;
            float rightPos = rightAnchor - layout.right.margin;
            float availableSpace = rightPos - leftPos - dimensions.width;
            transform.x = leftPos + availableSpace * layout.horizontalBias;
        } else if (leftAnchor != null) {
            transform.x = leftAnchor + layout.left.margin;
        } else if (rightAnchor != null) {
            transform.x = rightAnchor - layout.right.margin - dimensions.width;
        }
    }

    private void processVertical(LayoutComponent layout, TransformComponent transform,
                                  DimensionsComponent dimensions, DimensionsComponent parentDimensions) {
        Float bottomAnchor = resolveAnchor(layout.bottom, parentDimensions);
        Float topAnchor = resolveAnchor(layout.top, parentDimensions);

        if (bottomAnchor != null && topAnchor != null) {
            float bottomPos = bottomAnchor + layout.bottom.margin;
            float topPos = topAnchor - layout.top.margin;
            float availableSpace = topPos - bottomPos - dimensions.height;
            transform.y = bottomPos + availableSpace * layout.verticalBias;
        } else if (bottomAnchor != null) {
            transform.y = bottomAnchor + layout.bottom.margin;
        } else if (topAnchor != null) {
            transform.y = topAnchor - layout.top.margin - dimensions.height;
        }
    }

    private Float resolveAnchor(LayoutComponent.ConstraintData data, DimensionsComponent parentDimensions) {
        if (data == null) return null;

        if (data.targetEntity == -1) {
            return resolveParentSide(data.targetSide, parentDimensions);
        } else {
            if (!engine.getEntityManager().isActive(data.targetEntity)) return null;

            TransformComponent siblingTransform = transformMapper.get(data.targetEntity);
            DimensionsComponent siblingDimensions = dimensionsMapper.get(data.targetEntity);
            if (siblingTransform == null || siblingDimensions == null) return null;

            return resolveSiblingSide(data.targetSide, siblingTransform, siblingDimensions);
        }
    }

    private Float resolveParentSide(LayoutComponent.ConstraintSide side, DimensionsComponent parentDimensions) {
        if (side == null) return null;
        switch (side) {
            case LEFT: return 0f;
            case RIGHT: return parentDimensions.width;
            case BOTTOM: return 0f;
            case TOP: return parentDimensions.height;
            default: return null;
        }
    }

    private Float resolveSiblingSide(LayoutComponent.ConstraintSide side,
                                      TransformComponent siblingTransform,
                                      DimensionsComponent siblingDimensions) {
        if (side == null) return null;
        switch (side) {
            case LEFT: return siblingTransform.x;
            case RIGHT: return siblingTransform.x + siblingDimensions.width;
            case BOTTOM: return siblingTransform.y;
            case TOP: return siblingTransform.y + siblingDimensions.height;
            default: return null;
        }
    }

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
        // Search siblings of the requesting entity's parent
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
