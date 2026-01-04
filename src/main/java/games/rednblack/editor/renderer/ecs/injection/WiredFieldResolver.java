package games.rednblack.editor.renderer.ecs.injection;

import games.rednblack.editor.renderer.ecs.MundaneWireException;
import games.rednblack.editor.renderer.ecs.Engine;
import com.badlogic.gdx.utils.reflect.Field;
import games.rednblack.editor.renderer.ecs.EngineConfiguration;
import games.rednblack.editor.renderer.ecs.annotations.Wire;

import java.util.HashMap;
import java.util.Map;

/**
 * Can inject arbitrary fields annotated with {@link Wire},
 * typically registered via registered via {@link EngineConfiguration#register}
 *
 * @author Snorre E. Brekke
 */
public class WiredFieldResolver implements UseInjectionCache, PojoFieldResolver {
	private InjectionCache cache;

	private Map<String, Object> pojos = new HashMap<String, Object>();
	private Engine engine;

	public WiredFieldResolver() {
	}

	@Override
	public void initialize(Engine engine) {
		this.engine = engine;
	}

	@Override
	public Object resolve(Object target, Class<?> fieldType, Field field) {
		ClassType injectionType = cache.getFieldClassType(fieldType);
		CachedField cachedField = cache.getCachedField(field);

		if (injectionType == ClassType.CUSTOM || injectionType == ClassType.ENGINE) {
			if (cachedField.wireType == WireType.WIRE) {
				String key = cachedField.name;
				if ("".equals(key)) {
					key = field.getType().getName();
				}

				if (!pojos.containsKey(key) && cachedField.failOnNull) {
					String err = "Not registered: " + key + "=" + fieldType;
					throw new MundaneWireException(err);
				}

				return pojos.get(key);
			}
		}
		return null;
	}

	@Override
	public void setCache(InjectionCache cache) {
		this.cache = cache;
	}

	@Override
	public void setPojos(Map<String, Object> pojos) {
		this.pojos = pojos;
	}
}
