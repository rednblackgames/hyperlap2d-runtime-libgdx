package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ViewPortComponent  extends PooledComponent {
	public Viewport viewPort;

	@Override
	public void reset() {
		viewPort = null;
	}
}
