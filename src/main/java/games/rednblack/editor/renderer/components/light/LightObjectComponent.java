package games.rednblack.editor.renderer.components.light;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import games.rednblack.editor.renderer.lights.ConeLight;
import games.rednblack.editor.renderer.lights.Light;
import games.rednblack.editor.renderer.lights.PointLight;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.commons.RefreshableComponent;

public class LightObjectComponent extends RefreshableComponent {
	public enum LightType {POINT, CONE}
	public LightType type;

	public int rays = 12;
	public float distance = 300;
	public float directionDegree = 0;
	public float height = 0;
	public float coneDegree = 45;
	public float softnessLength = -1f;
	public float intensity = 1f;
	public Vector3 falloff = new Vector3(1, 0, 5);

	public boolean isStatic = false;
	public boolean isXRay = false;
	public boolean isSoft = false;
	public boolean isActive = true;

	public transient Light lightObject = null;
	private transient RayHandler rayHandler;

	public LightObjectComponent() {
	}

	public void setRayHandler(RayHandler rayHandler) {
		this.rayHandler = rayHandler;
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

		falloff.set(1, 0, 0);

		if (lightObject != null) {
			lightObject.remove();
			lightObject = null;
		}
	}

	@Override
	protected void refresh(int entity) {
		if (rayHandler == null)
			return;

		if (lightObject != null)
			lightObject.remove();

		if (type == LightType.POINT) {
			lightObject = new PointLight(rayHandler, rays);
		} else {
			lightObject = new ConeLight(rayHandler, rays, Color.WHITE, 1, 0, 0, 0, 0);
		}
	}
}
