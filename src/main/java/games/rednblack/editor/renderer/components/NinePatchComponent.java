package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.g2d.NinePatch;

public class NinePatchComponent  extends PooledComponent {
	public String textureRegionName;
	public NinePatch ninePatch;

	@Override
	public void reset() {
		textureRegionName = null;
		ninePatch = null;
	}
}
