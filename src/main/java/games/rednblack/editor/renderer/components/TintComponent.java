package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.Color;

public class TintComponent  extends PooledComponent {
	public Color color = new Color();

	@Override
	public void reset() {
		color.set(0, 0, 0, 0);
	}
}
