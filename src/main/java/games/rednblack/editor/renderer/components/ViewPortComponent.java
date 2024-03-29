package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ViewPortComponent extends PooledComponent {
	public transient Viewport viewPort;
	public int pixelsPerWU = 1;

	@Override
	public void reset() {
		viewPort = null;
		pixelsPerWU = 1;
	}
}
