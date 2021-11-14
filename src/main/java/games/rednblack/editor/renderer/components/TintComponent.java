package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.Color;

public class TintComponent  extends PooledComponent {
	public Color color = new Color(1, 1, 1, 1);

	@Override
	public void reset() {
		color.set(1, 1, 1, 1);
	}
}
