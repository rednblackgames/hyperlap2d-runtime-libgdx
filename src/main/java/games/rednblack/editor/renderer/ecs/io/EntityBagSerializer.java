package games.rednblack.editor.renderer.ecs.io;

import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.annotations.SkipWire;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class EntityBagSerializer implements Json.Serializer<Bag> {
	@SkipWire private final Engine engine;

	public EntityBagSerializer(Engine engine) {
		this.engine = engine;
		engine.inject(this);
	}

	@Override
	public void write(Json json, Bag bag, Class knownType) {
		json.writeArrayStart();
		for (Object item : bag)
			json.writeValue(item);
		json.writeArrayEnd();
	}

	@Override
	public Bag read(Json json, JsonValue jsonData, Class type) {
		Bag<Entity> result = new Bag<Entity>();
		for (JsonValue child = jsonData.child; child != null; child = child.next)
			result.add(json.readValue(Entity.class, child));

		return result;
	}
}
