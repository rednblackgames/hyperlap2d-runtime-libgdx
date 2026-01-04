package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.Component;
import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.EntitySubscription;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import static games.rednblack.editor.renderer.ecs.Aspect.all;

class EntityBagFieldMutator implements MultiFieldMutator<Bag<Entity>, Component> {
	private final Bag<Entity> empty = new Bag<Entity>();
	private EntitySubscription all;

	@Override
	public void validate(int sourceId, Bag<Entity> entities, LinkListener listener) {
		for (int i = 0; entities.size() > i; i++) {
			Entity e = entities.get(i);
			if (!all.getActiveEntityIds().unsafeGet(e.getId())) {
				entities.remove(i--);
				if (listener != null)
					listener.onTargetDead(sourceId, e.getId());
			}
		}
	}

	@Override
	public Bag<Entity> read(Component c, Field f) {
		try {
			Bag<Entity> e = (Bag<Entity>) f.get(c);
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
