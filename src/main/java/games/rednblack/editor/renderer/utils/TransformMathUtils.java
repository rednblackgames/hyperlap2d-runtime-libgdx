package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.*;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;

public class TransformMathUtils {

	private static final Matrix3 tmpMat = new Matrix3();

	/** Transforms the specified point in the scene's coordinates to the entity's local coordinate system. */
	public static Vector2 sceneToLocalCoordinates (int entity, Vector2 sceneCoords) {
		ParentNodeComponent parentNodeComponent = ComponentRetriever.get(entity, ParentNodeComponent.class);
		int parentEntity = -1;
		if(parentNodeComponent != null){
			parentEntity = parentNodeComponent.parentEntity;
		}
		if (parentEntity != -1) sceneToLocalCoordinates(parentEntity, sceneCoords);
		parentToLocalCoordinates(entity, sceneCoords);
		return sceneCoords;
	}

    public static Vector2 globalToLocalCoordinates (int entity, Vector2 sceneCoords) {
        ParentNodeComponent parentNodeComponent = ComponentRetriever.get(entity, ParentNodeComponent.class);
        int parentEntity = -1;
        if(parentNodeComponent != null){
            ViewPortComponent viewPortComponent = ComponentRetriever.get(parentNodeComponent.parentEntity, ViewPortComponent.class);
            if(viewPortComponent == null) {
                parentEntity = parentNodeComponent.parentEntity;
            } else {
				viewPortComponent.viewPort.unproject(sceneCoords);
            }
        }
        if (parentEntity != -1) {
            globalToLocalCoordinates(parentEntity, sceneCoords);
        }
        parentToLocalCoordinates(entity, sceneCoords);
        return sceneCoords;
    }


	/** Converts the coordinates given in the parent's coordinate system to this entity's coordinate system. */
	public static Vector2 parentToLocalCoordinates (int childEntity, Vector2 parentCoords) {
		TransformComponent transform = ComponentRetriever.get(childEntity, TransformComponent.class);

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

	/** Transforms the specified point array in the entity's coordinates to be in the scene's coordinates.*/
	public static Vector2[] localToSceneCoordinates (Entity entity, Vector2[] localCoords) {
		return localToAscendantCoordinates(null, entity, localCoords);
	}

	/** Converts coordinates for this entity to those of a parent entity. The ascendant does not need to be a direct parent. */
	public static Vector2[] localToAscendantCoordinates (Entity ascendant, Entity entity, Vector2[] localCoords) {
		while (entity != null) {
			for (int i = 0; i < localCoords.length; i++) {
				localToParentCoordinates(entity, localCoords[i]);
			}

			ParentNodeComponent parentNode = ComponentRetriever.get(entity, ParentNodeComponent.class);
			if(parentNode == null){
				break;
			}
			entity = parentNode.parentEntity;
			if (entity == ascendant) break;
		}
		return localCoords;
	}

	/** Transforms the specified point in the entity's coordinates to be in the scene's coordinates.*/
	public static Vector2 localToSceneCoordinates (int entity, Vector2 localCoords) {
		return localToAscendantCoordinates(-1, entity, localCoords);
	}

	/** Converts coordinates for this entity to those of a parent entity. The ascendant does not need to be a direct parent. */
	public static Vector2 localToAscendantCoordinates (int ascendant, int entity, Vector2 localCoords) {
		while (entity != -1) {
			localToParentCoordinates(entity, localCoords);
			ParentNodeComponent parentNode = ComponentRetriever.get(entity, ParentNodeComponent.class);
			if(parentNode == null){
				break;
			}
			entity = parentNode.parentEntity;
			if (entity == ascendant) break;
		}
		return localCoords;
	}

	/** Transforms the specified point in the actor's coordinates to be in the parent's coordinates. */
	public static Vector2 localToParentCoordinates (int entity, Vector2 localCoords) {
		TransformComponent transform = ComponentRetriever.get(entity, TransformComponent.class);

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

	public static Matrix4 computeTransform(int entity) {
		TransformComponent curTransform = ComponentRetriever.get(entity, TransformComponent.class);

		Affine2 worldTransform = curTransform.worldTransform;

		float originX = curTransform.originX;
		float originY = curTransform.originY;
		float x = curTransform.x;
		float y = curTransform.y;
		float rotation = curTransform.rotation;
		float scaleX = curTransform.scaleX * (curTransform.flipX ? -1 : 1);
		float scaleY = curTransform.scaleY * (curTransform.flipY ? -1 : 1);

		worldTransform.setToTrnRotScl(x + originX , y + originY, rotation, scaleX, scaleY);
		if (originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

		curTransform.computedTransform.set(worldTransform);

		return curTransform.computedTransform;
	}

	public static void applyTransform(int entity, Batch batch) {
		TransformComponent curTransform = ComponentRetriever.get(entity, TransformComponent.class);
		curTransform.oldTransform.set(batch.getTransformMatrix());
		batch.setTransformMatrix(curTransform.computedTransform);
	}

	public static void resetTransform(int entity, Batch batch) {
		TransformComponent curTransform = ComponentRetriever.get(entity, TransformComponent.class);
		batch.setTransformMatrix(curTransform.oldTransform);
	}
}
