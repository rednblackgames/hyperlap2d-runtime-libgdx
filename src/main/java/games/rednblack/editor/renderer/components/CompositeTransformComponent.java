package games.rednblack.editor.renderer.components;

import com.badlogic.gdx.math.Rectangle;

public class CompositeTransformComponent implements BaseComponent {
	public boolean automaticResize = true;
	public boolean scissorsEnabled = false;
	public boolean renderToFBO = false;

	public final Rectangle scissors = new Rectangle();
	public final Rectangle clipBounds = new Rectangle();

	@Override
	public void reset() {
		automaticResize = true;
		scissorsEnabled = false;
		renderToFBO = false;

		scissors.set(0, 0, 0, 0);
		clipBounds.set(0, 0, 0, 0);
	}
}
