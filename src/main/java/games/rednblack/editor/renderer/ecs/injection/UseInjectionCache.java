package games.rednblack.editor.renderer.ecs.injection;

import games.rednblack.editor.renderer.ecs.Engine;

import java.util.Map;

/**
 * {@link FieldResolver} implementing this interface will have the {@link #setCache(InjectionCache)}
 * method called during {@link FieldHandler#initialize(Engine, Map)}, prior to {@link FieldResolver#initialize(Engine)}
 * being called.
 *
 * @author Snorre E. Brekke
 */
public interface UseInjectionCache {
	/**
	 * @param cache used by the {@link FieldHandler}
	 */
	void setCache(InjectionCache cache);
}
