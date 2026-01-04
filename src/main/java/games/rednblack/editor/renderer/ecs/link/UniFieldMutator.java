package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.Component;
import games.rednblack.editor.renderer.ecs.Engine;
import com.badlogic.gdx.utils.reflect.Field;

/**
 * <p>Internal interface. Used for reading/writing entity
 * fields pointing to a single entity.</p>
 */
public interface UniFieldMutator {
	int read(Component c, Field f);
	void write(int value, Component c, Field f);
	void setEngine(Engine engine);
}
