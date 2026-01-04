package games.rednblack.editor.renderer.ecs.io;

import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.annotations.SkipWire;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class IntBagEntitySerializer implements Json.Serializer<IntBag> {
	@SkipWire private final Engine engine;
	private final Bag<Entity> translatedIds = new Bag<Entity>();

	private int recursionLevel;

	public IntBagEntitySerializer(Engine engine) {
		this.engine = engine;
		engine.inject(this);
	}

	@Override
	public void write(Json json, IntBag entities, Class knownType) {
		recursionLevel++;

		if (recursionLevel == 1) {
			json.writeObjectStart();
			for (int i = 0, s = entities.size(); s > i; i++) {
				Entity e = engine.getEntity(entities.get(i));
				json.writeValue(Integer.toString(e.getId()), e);
			}
			json.writeObjectEnd();
		} else {
			json.writeArrayStart();
			for (int i = 0, s = entities.size(); s > i; i++) {
				json.writeValue(entities.get(i));
			}
			json.writeArrayEnd();
		}

		recursionLevel--;
	}

	@Override
	public IntBag read(Json json, JsonValue jsonData, Class type) {
		recursionLevel++;

		IntBag bag = new IntBag();
		if (recursionLevel == 1) {
			JsonValue entityArray = jsonData.child;
			JsonValue entity = entityArray;
			while (entity != null) {
				Entity e = json.readValue(Entity.class, entity.child);
				translatedIds.set(Integer.parseInt(entity.name), e);
				bag.add(e.getId());

				entity = entity.next;
			}
		} else {
			for (JsonValue child = jsonData.child; child != null; child = child.next) {
				bag.add(json.readValue(Integer.class, child));
			}
		}

		recursionLevel--;

		return bag;

	}

	public Bag<Entity> getTranslatedIds() {
		return translatedIds;
	}
}
