package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.Component;
import games.rednblack.editor.renderer.ecs.EntitySubscription;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import static games.rednblack.editor.renderer.ecs.Aspect.all;

class IntBagFieldMutator implements MultiFieldMutator<IntBag, Component> {
	private final IntBag empty = new IntBag();
	private EntitySubscription all;

	@Override
	public void validate(int sourceId, IntBag ids, LinkListener listener) {
		for (int i = 0; ids.size() > i; i++) {
			int id = ids.get(i);
			if (!all.getActiveEntityIds().unsafeGet(id)) {
				ids.removeIndex(i--);
				if (listener != null)
					listener.onTargetDead(sourceId, id);
			}
		}
	}

	@Override
	public IntBag read(Component c, Field f) {
		try {
			final boolean isNotAccessible = !f.isAccessible();
			if (isNotAccessible) {
				f.setAccessible(true);
			}
			IntBag e = (IntBag) f.get(c);
			if (isNotAccessible) {
				f.setAccessible(false);
			}
			return (e != null) ? e : empty;
		} catch (ReflectionException exc) {
			throw new RuntimeException(exc);
		}
	}

	@Override
	public void setEngine(Engine engine) {
		all = engine.getAspectSubscriptionManager().get(all());
	}
}
