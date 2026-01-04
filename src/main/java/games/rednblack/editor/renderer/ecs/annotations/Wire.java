package games.rednblack.editor.renderer.ecs.annotations;

import games.rednblack.editor.renderer.ecs.*;

import java.lang.annotation.*;


/**
 * Customizes reflective injection of {@link ComponentMapper}, {@link EntitySystem},
 * {@link Manager}, and registered types via {@link EngineConfiguration#register}.
 *
 * Odb automatically injects above types into entity systems, during initialization.
 *
 * Inject into any object using <code>@Wire</code> and {@link Engine#inject(Object)}
 *
 * Nonstandard dependency fields must be explicitly annotated with
 * <code>@Wire(name="myName")</code> to inject by name, or <code>@Wire</code>
 * to inject by type. Class level <code>@Wire</code> annotation is not enough.
 *
 * By default, systems inject inherited fields from superclasses.
 * Override this behavior with <code>@Wire(injectInherited=false)</code>.
 *
 * By default, if <code>@Wire</code> fails to inject a field - typically because the requested
 * type hasn't been added to the engine instance - a MundaneWireException is thrown.
 * Override this behavior via <code>@Wire(failOnNull=false)</code>.
 *
 * To specify which nonstandard dependencies to inject, use
 * {@link EngineConfiguration#register(String, Object)} and
 * {@link EngineConfiguration#register(Object)}.
 *
 * @see AspectDescriptor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@Documented
public @interface Wire {

	/**
	 * If true, also inject inherited fields.
	 */
	boolean injectInherited() default false;


	/**
	 * Throws a {@link NullPointerException} if field can't be injected.
	 */
	boolean failOnNull() default true;


	/**
	 *
	 */
	String name() default "";
}
