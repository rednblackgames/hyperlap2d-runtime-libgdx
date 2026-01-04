package games.rednblack.editor.renderer.ecs.injection;

import games.rednblack.editor.renderer.ecs.BaseSystem;
import games.rednblack.editor.renderer.ecs.Component;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.Engine;
import com.badlogic.gdx.utils.reflect.Field;
import games.rednblack.editor.renderer.ecs.Manager;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Can resolve {@link Engine}, {@link ComponentMapper}, {@link BaseSystem} and
 * {@link Manager} types registered in the {@link Engine}
 *
 * @author Snorre E. Brekke
 */
public class ArtemisFieldResolver implements FieldResolver, UseInjectionCache {

	private Engine engine;
	private InjectionCache cache;

	private Map<Class<?>, Class<?>> systems;

	public ArtemisFieldResolver() {
		systems = new IdentityHashMap<Class<?>, Class<?>>();
	}

	@Override
	public void initialize(Engine engine) {
		this.engine = engine;

		for (BaseSystem es : engine.getSystems()) {
			Class<?> origin = es.getClass();
			Class<?> clazz = origin;
			do {
				systems.put(clazz, origin);
			} while ((clazz = clazz.getSuperclass()) != Object.class);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolve(Object target, Class<?> fieldType, Field field) {
		ClassType injectionType = cache.getFieldClassType(fieldType);
		switch (injectionType) {
			case MAPPER:
				return getComponentMapper(field);
			case SYSTEM:
				return engine.getSystem((Class<BaseSystem>) systems.get(fieldType));
			case ENGINE:
				return engine;
			default:
				return null;

		}
	}

	@SuppressWarnings("unchecked")
	private ComponentMapper<?> getComponentMapper(Field field) {
		Class<?> mapperType = cache.getGenericType(field);
		return engine.getMapper((Class<? extends Component>) mapperType);

	}

	@Override
	public void setCache(InjectionCache cache) {
		this.cache = cache;
	}
}
