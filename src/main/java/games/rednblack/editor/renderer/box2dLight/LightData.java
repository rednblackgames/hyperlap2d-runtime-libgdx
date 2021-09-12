package games.rednblack.editor.renderer.box2dLight;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Pool;

public class LightData implements Pool.Poolable {

	public Object userData = null;

	public float height;

	public boolean roofShadow;

	int shadowsDropped = 0;

	public LightData () {
		this(0);
	}

	public LightData (float h) {
		height = h;
	}

	public LightData (float h, boolean roofShadow) {
		height = h;
		this.roofShadow = roofShadow;
	}

	public LightData (Object data, float h, boolean roofShadow) {
		height = h;
		userData = data;
		this.roofShadow = roofShadow;
	}

	public float getLimit (float distance, float lightHeight, float lightRange) {
		float l = 0f;
		if (lightHeight > height) {
			l = distance * height / (lightHeight - height);
			float diff = lightRange - distance;
			if (l > diff) {
				l = diff;
			}
		} else if (lightHeight == 0f) {
			l = lightRange;
		} else {
			l = lightRange - distance;
		}

		return l > 0 ? l : 0f;
	}

	public static Object getUserData (Fixture fixture) {
		Object data = fixture.getUserData();
		if (data instanceof LightData) {
			return ((LightData) data).userData;
		} else {
			return data;
		}
	}

	@Override
	public void reset() {
		userData = null;
		height = 0;
		roofShadow = false;
		shadowsDropped = 0;
	}
}
