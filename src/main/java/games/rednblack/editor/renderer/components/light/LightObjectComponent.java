package games.rednblack.editor.renderer.components.light;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.Color;
import games.rednblack.editor.renderer.box2dLight.ConeLight;
import games.rednblack.editor.renderer.box2dLight.Light;
import games.rednblack.editor.renderer.box2dLight.PointLight;
import games.rednblack.editor.renderer.box2dLight.RayHandler;

public class LightObjectComponent extends PooledComponent {
	public enum LightType {POINT, CONE}
	public LightType type;

	public int rays = 12;
	public float distance = 300;
	public float directionDegree = 0;
	public float height = 0;
	public float coneDegree = 45;
	public float softnessLength = -1f;
	public float intensity = 1f;

	public boolean isStatic = true;
	public boolean isXRay = true;
	public boolean isSoft = true;
	public boolean isActive = true;

	public transient Light lightObject = null;

	public LightObjectComponent() {
	}

	public Light rebuildRays(RayHandler rayHandler) {
		if (rayHandler == null)
			return lightObject;

		lightObject.remove();

		if (type == LightType.POINT) {
			lightObject = new PointLight(rayHandler, rays);
		} else {
			lightObject = new ConeLight(rayHandler, rays, Color.WHITE, 1, 0, 0, 0, 0);
		}

		return lightObject;
	}

	@Override
	public void reset() {
		type = null;

		rays = 12;
		distance = 10;
		directionDegree = 0;
		height = 0;
		coneDegree = 30;
		softnessLength = 1f;

		isStatic = true;
		isXRay = true;
		isSoft = true;
		isActive = true;

		lightObject = null;
	}
}
