package games.rednblack.editor.renderer.ecs.io;

import games.rednblack.editor.renderer.ecs.ComponentCollector;
import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.managers.EngineSerializationManager;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.*;

public class JsonArtemisSerializer extends EngineSerializationManager.ArtemisSerializer<Json.Serializer> {
	private final Json json;
	private final ComponentLookupSerializer lookup;
	private final IntBagEntitySerializer intBagEntitySerializer;
	private final TransmuterEntrySerializer transmuterEntrySerializer;
	private final EntitySerializer entitySerializer;
	private final ComponentCollector componentCollector;

	private boolean prettyPrint;
	private ReferenceTracker referenceTracker;

	public JsonArtemisSerializer(Engine engine) {
		super(engine);

		componentCollector = new ComponentCollector(engine);
		referenceTracker = new ReferenceTracker(engine);

		lookup = new ComponentLookupSerializer();
		intBagEntitySerializer = new IntBagEntitySerializer(engine);
		entitySerializer = new EntitySerializer(engine, referenceTracker);
		transmuterEntrySerializer = new TransmuterEntrySerializer();

		json = new Json(JsonWriter.OutputType.json);
		json.setIgnoreUnknownFields(true);
		json.setSerializer(SaveFileFormat.ComponentIdentifiers.class, lookup);
		json.setSerializer(Bag.class, new EntityBagSerializer(engine));
		json.setSerializer(IntBag.class, intBagEntitySerializer);
		json.setSerializer(Entity.class, entitySerializer);
		json.setSerializer(ArchetypeMapper.class, new ArchetypeMapperSerializer());
		json.setSerializer(ArchetypeMapper.TransmuterEntry.class, transmuterEntrySerializer);
	}

	public JsonArtemisSerializer prettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		return this;
	}

	public JsonArtemisSerializer setUsePrototypes(boolean usePrototypes) {
		json.setUsePrototypes(usePrototypes);
		entitySerializer.setUsePrototypes(usePrototypes);
		return this;
	}

	@Override
	public EngineSerializationManager.ArtemisSerializer register(Class<?> type, Json.Serializer serializer) {
		json.setSerializer(type, serializer);
		return this;
	}

	public void save(Writer writer, SaveFileFormat save) {
		try {
			referenceTracker.inspectTypes(engine);
			referenceTracker.preWrite(save);

			save.archetypes = new ArchetypeMapper(engine, save.entities);

			componentCollector.preWrite(save);
			entitySerializer.serializationState = save;
			transmuterEntrySerializer.identifiers = save.componentIdentifiers;
			entitySerializer.archetypeMapper = new ArchetypeMapper(engine, save.entities);
			entitySerializer.archetypeMapper.serializationState = save;
			save.componentIdentifiers.build();
			if (prettyPrint) {
				writer.append(json.prettyPrint(save));
				writer.flush();
			} else {
				json.toJson(save, writer);
			}
		} catch (IOException e) {
			throw new SerializationException(e);
		}
	}

	@Override
	protected void save(OutputStream out, SaveFileFormat save) {
		save(new OutputStreamWriter(out), save);
	}

	@Override
	protected <T extends SaveFileFormat> T load(InputStream is, Class<T> format) {
		return load(new JsonReader().parse(is), format);
	}

	public <T extends SaveFileFormat> T load(JsonValue jsonData, Class<T> format) {
		entitySerializer.preLoad();

		SaveFileFormat partial = partialLoad(jsonData);
		referenceTracker.inspectTypes(partial.componentIdentifiers.getTypes());
		entitySerializer.factory.configureWith(countChildren(jsonData.get("entities")));

		T t = newInstance(format);
		json.readFields(t, jsonData);
		t.tracker = entitySerializer.keyTracker;
		referenceTracker.translate(intBagEntitySerializer.getTranslatedIds());
		return t;
	}

	private <T extends SaveFileFormat> T newInstance(Class<T> format) {
		if (format.getClass().equals(SaveFileFormat.class))
			return (T) new SaveFileFormat();

		try {
			Constructor ctor = ClassReflection.getDeclaredConstructor(format);
			ctor.setAccessible(true);
			return (T) ctor.newInstance();
		} catch (ReflectionException e) {
			throw new RuntimeException(e);
		}
	}

	private SaveFileFormat partialLoad(JsonValue jsonMap) {
		SaveFileFormat save = new SaveFileFormat((IntBag)null);
		json.readField(save, "componentIdentifiers", jsonMap);
		transmuterEntrySerializer.identifiers = save.componentIdentifiers;

		json.readField(save, "archetypes", jsonMap);
		entitySerializer.archetypeMapper = save.archetypes;

		entitySerializer.serializationState = save;
		if (entitySerializer.archetypeMapper != null) {
			entitySerializer.archetypeMapper.serializationState = save;
			transmuterEntrySerializer.identifiers = save.componentIdentifiers;
		}

		return save;
	}

	private int countChildren(JsonValue jsonData) {
		if (jsonData == null || jsonData.child == null)
			return 0;

		JsonValue entity = jsonData.child;
		int count = 0;
		while (entity != null) {
			count++;
			entity = entity.next;
		}
		return count;
	}
}
