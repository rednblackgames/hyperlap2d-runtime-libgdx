package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.Component;
import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.Engine;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

class EntityFieldMutator implements UniFieldMutator {
	private Engine engine;

	@Override
	public int read(Component c, Field f) {
		try {
			Entity e = (Entity) f.get(c);
			return (e != null) ? e.getId() : -1;
		} catch (ReflectionException exc) {
			throw new RuntimeException(exc);
		}
	}

	@Override
	public void write(int value, Component c, Field f) {
		try {
			Entity e = (value != -1) ? engine.getEntity(value) : null;
			f.set(c, e);
		} catch (ReflectionException exc) {
			throw new RuntimeException(exc);
		}
	}

	@Override
	public void setEngine(Engine engine) {
		this.engine = engine;
	}
}
