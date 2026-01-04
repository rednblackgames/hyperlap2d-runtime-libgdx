package games.rednblack.editor.renderer.ecs.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.ecs.io.SaveFileFormat;

/**
 * Creates a tag, local to an instance of {@link SaveFileFormat}.
 *
 * @see SaveFileFormat#get(String)
 * @see SaveFileFormat#has(String)
 */
@Transient
public class SerializationTag extends PooledComponent {
	public String tag;

	@Override
	protected void reset() {
		tag = null;
	}
}
