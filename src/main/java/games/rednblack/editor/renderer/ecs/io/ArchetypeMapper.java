package games.rednblack.editor.renderer.ecs.io;

import games.rednblack.editor.renderer.ecs.*;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is only public in order to keep GWT happy.
 * Nothing to see here (API can change without prior notice)
 */
public class ArchetypeMapper {
	public Map<Integer, TransmuterEntry> compositionIdMapper = new HashMap<Integer, TransmuterEntry>();
	public SaveFileFormat serializationState;

	public ArchetypeMapper(Engine engine, IntBag toSave) {
		int[] ids = toSave.getData();
		Bag<Component> components = new Bag<Component>();
		Bag<Class<? extends Component>> types = new Bag<Class<? extends Component>>();

		for (int i = 0, s = toSave.size(); s > i; i++) {
			int compositionId = engine.getEntity(ids[i]).getCompositionId();
			if (!compositionIdMapper.containsKey(compositionId)) {
				components.clear();
				types.clear();

				engine.getComponentManager().getComponentsFor(ids[i], components);
				compositionIdMapper.put(compositionId,
					new TransmuterEntry(toClasses(components, types)));
			}
		}
	}

	public ArchetypeMapper() {} // for serialization

	private static Bag<Class<? extends Component>> toClasses(Bag<Component> source,
	                                                         Bag<Class<? extends Component>> target) {
		for (int i = 0; i < source.size(); i++) {
			target.add(source.get(i).getClass());
		}

		return target;
	}
	public void transmute(Entity e, int compositionId) {
		compositionIdMapper.get(compositionId).transmute(e);
	}

	public Iterable<? extends Map.Entry<Integer, TransmuterEntry>> entrySet() {
		return compositionIdMapper.entrySet();
	}

	public static class TransmuterEntry {
		public List<Class<? extends Component>> componentTypes = new ArrayList<Class<? extends Component>>();
		private transient EntityTransmuter transmuter;

		public TransmuterEntry(Bag<Class<? extends Component>> types) {
			for (Class<? extends Component> c : types) {
				if (ClassReflection.getAnnotation(c, Transient.class) != null)
					continue;

				componentTypes.add(c);
			}
		}

		public TransmuterEntry() {}

		public void transmute(Entity e) {
			if (transmuter == null) {
				EntityTransmuterFactory factory = new EntityTransmuterFactory(e.getEngine());
				for (int i = 0, s = componentTypes.size(); s > i; i++)
					factory.add(componentTypes.get(i));

				transmuter = factory.build();
			}

			transmuter.transmute(e);
		}
	}
}
