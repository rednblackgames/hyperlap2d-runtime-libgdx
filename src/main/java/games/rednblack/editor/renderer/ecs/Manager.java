package games.rednblack.editor.renderer.ecs;

import games.rednblack.editor.renderer.ecs.utils.IntBag;

import static games.rednblack.editor.renderer.ecs.Aspect.all;
import static games.rednblack.editor.renderer.ecs.EntitySystem.FLAG_INSERTED;
import static games.rednblack.editor.renderer.ecs.EntitySystem.FLAG_REMOVED;
import static games.rednblack.editor.renderer.ecs.utils.reflect.ReflectionUtil.implementsObserver;


/**
 * A manager for handling entities in the engine.
 *
 * In odb Manager has been absorbed into the {@link BaseSystem} hierarchy.
 * While Manager is still available we recommend implementing new
 * managers using IteratingSystem, {@link BaseEntitySystem} with
 * {@link Aspect#all()}, or {@link BaseSystem} depending on your needs.
 *
 * @author Arni Arent
 * @author Adrian Papari
 */
public abstract class Manager extends BaseSystem {
	private int methodFlags;

	/** Called when entity gets added to engine. */
	public void added(Entity e) {
		throw new RuntimeException("I shouldn't be here...");
	}

	/** Called when entity gets deleted from engine. */
	public void deleted(Entity e) {
		throw new RuntimeException("... if it weren't for the tests.");
	}

	/**
	 * Set the engine this system works on.
	 *
	 * @param engine
	 *			the engine to set
	 */
	@Override
	protected void setEngine(Engine engine) {
		super.setEngine(engine);
		if(implementsObserver(this, "added"))
			methodFlags |= FLAG_INSERTED;
		if(implementsObserver(this, "deleted"))
			methodFlags |= FLAG_REMOVED;
	}

	/** Hack to register manager to right subscription */
	protected void registerManager() {
		engine.getAspectSubscriptionManager()
				.get(all())
				.addSubscriptionListener(new EntitySubscription.SubscriptionListener() {
					@Override
					public void inserted(IntBag entities) {
						added(entities);
					}

					@Override
					public void removed(IntBag entities) {
						deleted(entities);
					}
				});
	}

	private void added(IntBag entities) {
		// performance hack, skip if manager lacks implementation of inserted.
		if ((methodFlags & FLAG_INSERTED) == 0)
			return;

		int[] ids = entities.getData();
		for (int i = 0, s = entities.size(); s > i; i++) {
			added(engine.getEntity(ids[i]));
		}
	}

	private void deleted(IntBag entities) {
		// performance hack, skip if manager lacks implementation of removed.
		if ((methodFlags & FLAG_REMOVED) == 0)
			return;

		int[] ids = entities.getData();
		for (int i = 0, s = entities.size(); s > i; i++) {
			deleted(engine.getEntity(ids[i]));
		}
	}

	/** Managers are not interested in processing. */
	@Override
	protected final void processSystem() {}
}
