package games.rednblack.editor.renderer.ecs.injection;

import games.rednblack.editor.renderer.ecs.*;
import games.rednblack.editor.renderer.ecs.annotations.Wire;

import java.util.Map;

/**
 * <p>API used by {@link Engine} to inject objects annotated with {@link Wire} with
 * dependencies. An injector injects {@link ComponentMapper}, {@link BaseSystem} and {@link games.rednblack.editor.renderer.ecs.Manager} types into systems and managers.
 * </p>
 * <p>To inject arbitrary types, use registered through {@link EngineConfiguration#register}.</p>
 * <p>To customize the injection-strategy for arbitrary types further, registered a custom {@link FieldHandler}
 * with custom one or more {@link FieldResolver}.</p>
 *
 * @author Snorre E. Brekke
 * @see FieldHandler
 */
public interface Injector {

	/**
	 * Programmatic retrieval of registered objects. Useful when
	 * full injection isn't necessary.
	 *
	 * @param id Name or class name.
	 * @return the requested object, or null if not found
	 *
	 * @see EngineConfiguration#register(String, Object)
	 */
	<T> T getRegistered(String id);

	/**
	 * Programmatic retrieval of registered objects. Useful when
	 * full injection isn't necessary. This method internally
	 * calls {@link #getRegistered(String)}, with the class name
	 * as parameter.
	 *
	 * @param id Uniquely registered instance, identified by class..
	 * @return the requested object, or null if not found
	 *
	 * @see EngineConfiguration#register(Object)
	 */
	<T> T getRegistered(Class<T> id);

	/**
	 * @param engine       this Injector will be used for
	 * @param injectables registered via {@link EngineConfiguration#register}
	 * @throws InjectionException when injector lacks a means to inject injectables.
	 */
	void initialize(Engine engine, Map<String, Object> injectables);

	/**
	 * Inject dependencies on object. The injector delegates to {@link FieldHandler} to resolve
	 * feiled values.
	 *
	 * @param target object which should have dependencies injected.
	 * @throws RuntimeException
	 * @see FieldHandler
	 */
	void inject(Object target) throws RuntimeException;

	/**
	 * Determins if a target object can be injected by this injector.
	 *
	 * @param target eligable for injection
	 * @return true if the Injector is capable of injecting the target object.
	 */
	boolean isInjectable(Object target);

	/**
	 * Enables the injector to be configured with a custom {@link FieldHandler} which will
	 * be used to resolve instance values for target-fields.
	 *
	 * @param fieldHandler to use for resolving dependency values
	 * @return this Injector for chaining
	 */
	Injector setFieldHandler(FieldHandler fieldHandler);

}
