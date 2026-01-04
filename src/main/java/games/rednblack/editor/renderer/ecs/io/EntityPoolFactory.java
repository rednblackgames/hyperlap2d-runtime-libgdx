package games.rednblack.editor.renderer.ecs.io;

import games.rednblack.editor.renderer.ecs.Archetype;
import games.rednblack.editor.renderer.ecs.ArchetypeBuilder;
import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.utils.IntBag;

import java.util.Arrays;

/**
 * Maintains the pool of entities to be laoded; ensures that the
 * entity id order matches the order in the json.
 */
class EntityPoolFactory {
	private final Archetype archetype;
	private final Engine engine;

	private IntBag pool = new IntBag();
	private int poolIndex;

	EntityPoolFactory(Engine engine) {
		this.engine = engine;
		archetype = new ArchetypeBuilder().build(engine);
	}

	void configureWith(int count) {
		poolIndex = 0;
		pool.setSize(0);
		pool.ensureCapacity(count);
		for (int i = 0; i < count; i++) {
			pool.add(engine.create(archetype));
		}

		Arrays.sort(pool.getData(), 0, pool.size());
	}

	Entity createEntity() {
		return engine.getEntity(pool.getData()[poolIndex++]);
	}
}
