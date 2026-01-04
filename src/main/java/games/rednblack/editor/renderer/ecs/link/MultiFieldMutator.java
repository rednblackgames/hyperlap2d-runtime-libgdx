package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.Component;
import games.rednblack.editor.renderer.ecs.Engine;
import com.badlogic.gdx.utils.reflect.Field;

/**
 * <p>Internal interface. Used for reading/writing entity
 * fields pointing to multiple entities.</p>
 */
public interface MultiFieldMutator<T, C extends Component> {
	void validate(int sourceId, T collection, LinkListener listener);
	T read(C c, Field f);
	void setEngine(Engine engine);
}
