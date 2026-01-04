package games.rednblack.editor.renderer.ecs.annotations;

import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.link.EntityLinkManager;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import games.rednblack.editor.renderer.ecs.utils.IntBag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks <code>int</code> and {@link IntBag} fields as holding entity id:s.
 * Only works on component types. This annotation ensures that:
 * <ul>
 *     <li>Entity references can be safely serialized</li>
 *     <li>Tracks inter-entity relationships, if the {@link EntityLinkManager}
 *         is registered with the engine.</li>
 * </ul>
 *
 * Only supports public fields. Kotlin requires fields with this annotation to also be annotated with {@code @JvmField}.
 *
 * <p>Annotation has no effect on {@link Bag}-of-entities and plain {@link Entity}
 * fields.</p>
 *
 * @see <a href="https://github.com/junkdog/artemis-odb/wiki/Entity-References-and-Serialization">Entity References and Serialization</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EntityId {
}
