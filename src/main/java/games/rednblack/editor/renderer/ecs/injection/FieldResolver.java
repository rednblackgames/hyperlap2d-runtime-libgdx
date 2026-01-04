package games.rednblack.editor.renderer.ecs.injection;

import games.rednblack.editor.renderer.ecs.Engine;
import com.badlogic.gdx.utils.reflect.Field;

/**
 * API used by {@link FieldHandler} to resolve field values in classes eligible for injection.
 *
 * @author Snorre E. Brekke
 */
public interface FieldResolver {

	/**
	 * Called after Wo
	 *
	 * @param engine
	 */
	void initialize(Engine engine);

	/**
	 * @param target object which should have dependencies injected.
	 */
	Object resolve(Object target, Class<?> fieldType, Field field);
}
