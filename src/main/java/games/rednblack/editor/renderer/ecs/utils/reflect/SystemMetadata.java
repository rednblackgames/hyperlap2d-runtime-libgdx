package games.rednblack.editor.renderer.ecs.utils.reflect;

import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseSystem;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.annotations.Exclude;
import games.rednblack.editor.renderer.ecs.annotations.One;

/**
 * Annotation reader for systems.
 *
 * @author Daan van Yperen
 */
public class SystemMetadata {
    private final Class<? extends BaseSystem> c;

    public SystemMetadata(Class<? extends BaseSystem> c) {
        this.c = c;
    }

    /**
     * Return aspect as defined in annotation.
     *
     * @return {@code Aspect.Builder} as defined in annotations, or {@code null} if none.
     */
    public Aspect.Builder getAspect() {
        final Aspect.Builder aspect = Aspect.all();
        final All all = ReflectionUtil.getAnnotation(c, All.class);
        if (all != null) {
            aspect.all(all.value());
        }
        final One one = ReflectionUtil.getAnnotation(c, One.class);
        if (one != null) {
            aspect.one(one.value());
        }
        final Exclude exclude = ReflectionUtil.getAnnotation(c, Exclude.class);
        if (exclude != null) {
            aspect.exclude(exclude.value());
        }
        return (all != null || exclude != null || one != null) ? aspect : null;
    }
}
