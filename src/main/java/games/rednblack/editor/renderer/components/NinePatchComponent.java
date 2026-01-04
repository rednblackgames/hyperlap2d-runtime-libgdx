package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import com.badlogic.gdx.graphics.g2d.NinePatch;

public class NinePatchComponent  extends PooledComponent {
	public String textureRegionName;
	public transient NinePatch ninePatch;

	@Override
	public void reset() {
		textureRegionName = null;
		ninePatch = null;
	}
}
