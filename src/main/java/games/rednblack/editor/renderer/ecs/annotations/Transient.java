package games.rednblack.editor.renderer.ecs.annotations;

import java.lang.annotation.*;

/**
 * Never persisted when serializing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Transient {
}
