package games.rednblack.editor.renderer.utils;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.*;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;

public class TransformMathUtils {

    private static final Matrix3 tmpMat = new Matrix3();

    public static float sceneToLocalRotation(int entity, float rotation, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        while (entity != -1) {
            TransformComponent transform = transformMapper.get(entity);
            rotation = transform.rotation - rotation;
            ParentNodeComponent parentNodeComponent = parentMapper.get(entity);
            if (parentNodeComponent != null) {
                entity = parentNodeComponent.parentEntity;
            } else {
                entity = -1;
            }
        }
        return rotation;
    }

    /**
     * Transforms the specified point in the scene's coordinates to the entity's local coordinate system.
     */
    public static Vector2 sceneToLocalCoordinates(int entity, Vector2 sceneCoords, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        ParentNodeComponent parentNodeComponent = parentMapper.get(entity);
        int parentEntity = -1;
        if (parentNodeComponent != null) {
            parentEntity = parentNodeComponent.parentEntity;
        }
        if (parentEntity != -1) sceneToLocalCoordinates(parentEntity, sceneCoords, transformMapper, parentMapper);
        parentToLocalCoordinates(entity, sceneCoords, transformMapper);
        return sceneCoords;
    }

    public static Vector2 globalToLocalCoordinates(int entity, Vector2 sceneCoords, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper, ComponentMapper<ViewPortComponent> viewportMapper) {
        ParentNodeComponent parentNodeComponent = parentMapper.get(entity);
        int parentEntity = -1;
        if (parentNodeComponent != null) {
            ViewPortComponent viewPortComponent = viewportMapper.get(parentNodeComponent.parentEntity);
            if (viewPortComponent == null) {
                parentEntity = parentNodeComponent.parentEntity;
            } else {
                viewPortComponent.viewPort.unproject(sceneCoords);
            }
        }
        if (parentEntity != -1) {
            globalToLocalCoordinates(parentEntity, sceneCoords, transformMapper, parentMapper, viewportMapper);
        }
        parentToLocalCoordinates(entity, sceneCoords, transformMapper);
        return sceneCoords;
    }


    /**
     * Converts the coordinates given in the parent's coordinate system to this entity's coordinate system.
     */
    public static Vector2 parentToLocalCoordinates(int childEntity, Vector2 parentCoords, ComponentMapper<TransformComponent> transformMapper) {
        TransformComponent transform = transformMapper.get(childEntity);

        final float rotation = transform.rotation;
        final float scaleX = transform.scaleX * (transform.flipX ? -1 : 1);
        final float scaleY = transform.scaleY * (transform.flipY ? -1 : 1);
        final float childX = transform.x;
        final float childY = transform.y;
        if (rotation == 0) {
            if (scaleX == 1 && scaleY == 1) {
                parentCoords.x -= childX;
                parentCoords.y -= childY;
            } else {
                final float originX = transform.originX;
                final float originY = transform.originY;

                parentCoords.x = (parentCoords.x - childX - originX) / scaleX + originX;
                parentCoords.y = (parentCoords.y - childY - originY) / scaleY + originY;
            }
        } else {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            final float originX = transform.originX;
            final float originY = transform.originY;
            final float tox = parentCoords.x - childX - originX;
            final float toy = parentCoords.y - childY - originY;
            parentCoords.x = (tox * cos + toy * sin) / scaleX + originX;
            parentCoords.y = (tox * -sin + toy * cos) / scaleY + originY;
        }
        return parentCoords;
    }

    /**
     * Transforms the specified point array in the entity's coordinates to be in the scene's coordinates.
     */
    public static Vector2[] localToSceneCoordinates(int entity, Vector2[] localCoords, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        return localToAscendantCoordinates(-1, entity, localCoords, transformMapper, parentMapper);
    }

    /**
     * Converts coordinates for this entity to those of a parent entity. The ascendant does not need to be a direct parent.
     */
    public static Vector2[] localToAscendantCoordinates(int ascendant, int entity, Vector2[] localCoords, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        while (entity != -1) {
            for (Vector2 localCoord : localCoords) {
                localToParentCoordinates(entity, localCoord, transformMapper);
            }

            ParentNodeComponent parentNode = parentMapper.get(entity);
            if (parentNode == null) {
                break;
            }
            entity = parentNode.parentEntity;
            if (entity == ascendant) break;
        }
        return localCoords;
    }

    /**
     * Transforms the specified point in the entity's coordinates to be in the scene's coordinates.
     */
    public static Vector2 localToSceneCoordinates(int entity, Vector2 localCoords, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        return localToAscendantCoordinates(-1, entity, localCoords, transformMapper, parentMapper);
    }

    /**
     * Converts coordinates for this entity to those of a parent entity. The ascendant does not need to be a direct parent.
     */
    public static Vector2 localToAscendantCoordinates(int ascendant, int entity, Vector2 localCoords, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        while (entity != -1) {
            localToParentCoordinates(entity, localCoords, transformMapper);
            ParentNodeComponent parentNode = parentMapper.get(entity);
            if (parentNode == null) {
                break;
            }
            entity = parentNode.parentEntity;
            if (entity == ascendant) break;
        }
        return localCoords;
    }

    /**
     * Transforms the specified point in the actor's coordinates to be in the parent's coordinates.
     */
    public static Vector2 localToParentCoordinates(int entity, Vector2 localCoords, ComponentMapper<TransformComponent> transformMapper) {
        TransformComponent transform = transformMapper.get(entity);

        final float rotation = -transform.rotation;
        final float scaleX = transform.scaleX * (transform.flipX ? -1 : 1);
        final float scaleY = transform.scaleY * (transform.flipY ? -1 : 1);
        final float x = transform.x;
        final float y = transform.y;
        if (rotation == 0) {
            if (scaleX == 1 && scaleY == 1) {
                localCoords.x += x;
                localCoords.y += y;
            } else {
                final float originX = transform.originX;
                final float originY = transform.originY;
                localCoords.x = (localCoords.x - originX) * scaleX + originX + x;
                localCoords.y = (localCoords.y - originY) * scaleY + originY + y;
            }
        } else {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);
            final float originX = transform.originX;
            final float originY = transform.originY;
            final float tox = (localCoords.x - originX) * scaleX;
            final float toy = (localCoords.y - originY) * scaleY;
            localCoords.x = (tox * cos + toy * sin) + originX + x;
            localCoords.y = (tox * -sin + toy * cos) + originY + y;
        }
        return localCoords;
    }

    /**
     * Transforms entity's rotation to be in the scene's coordinates.
     */
    public static float localToSceneRotation(int entity, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        return localToAscendantRotation(-1, entity, 0, transformMapper, parentMapper);
    }

    /**
     * Converts local rotation for this entity to those of a parent entity. The ascendant does not need to be a direct parent.
     */
    public static float localToAscendantRotation(int ascendant, int entity, float rotation, ComponentMapper<TransformComponent> transformMapper, ComponentMapper<ParentNodeComponent> parentMapper) {
        while (entity != -1) {
            TransformComponent transform = transformMapper.get(entity);
            rotation += transform.rotation;
            ParentNodeComponent parentNode = parentMapper.get(entity);
            if (parentNode == null) {
                break;
            }
            entity = parentNode.parentEntity;
            if (entity == ascendant) break;
        }
        return rotation;
    }

    public static Matrix3 transform(TransformComponent transformComponent) {
        float translationX = transformComponent.x + transformComponent.originX;
        float translationY = transformComponent.y + transformComponent.originY;
        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);
        float angle = transformComponent.rotation;
        tmpMat.idt();
        return tmpMat
                .translate(translationX, translationY)
                .rotate(angle)
                .scale(scaleX, scaleY)
                .translate(-translationX, -translationY);

    }

    public static Matrix4 computeTransform(TransformComponent curTransform) {
        Affine2 worldTransform = curTransform.worldTransform;

        float originX = curTransform.originX;
        float originY = curTransform.originY;
        float x = curTransform.x;
        float y = curTransform.y;
        float rotation = curTransform.rotation;
        float scaleX = curTransform.scaleX * (curTransform.flipX ? -1 : 1);
        float scaleY = curTransform.scaleY * (curTransform.flipY ? -1 : 1);

        worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY);
        if (originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

        curTransform.computedTransform.set(worldTransform);

        return curTransform.computedTransform;
    }

    public static void applyTransform(Batch batch, TransformComponent curTransform) {
        curTransform.oldTransform.set(batch.getTransformMatrix());
        batch.setTransformMatrix(curTransform.computedTransform);
    }

    public static void resetTransform(Batch batch, TransformComponent curTransform) {
        batch.setTransformMatrix(curTransform.oldTransform);
    }
}
