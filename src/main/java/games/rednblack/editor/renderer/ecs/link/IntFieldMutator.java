package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.Component;
import games.rednblack.editor.renderer.ecs.Engine;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

class IntFieldMutator implements UniFieldMutator {
	@Override
	public int read(Component c, Field f) {
		try {
			return (Integer) f.get(c);
		} catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(int value, Component c, Field f) {
		try {
			f.set(c, value);
		} catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setEngine(Engine engine) {}
}
