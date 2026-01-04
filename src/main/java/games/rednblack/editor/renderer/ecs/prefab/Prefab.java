package games.rednblack.editor.renderer.ecs.prefab;

import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.io.JsonArtemisSerializer;
import games.rednblack.editor.renderer.ecs.io.SaveFileFormat;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.utils.JsonValue;

public abstract class Prefab extends BasePrefab<JsonValue, JsonArtemisSerializer> {
	protected Prefab(Engine engine, PrefabReader<JsonValue> reader) {
		super(engine, reader);
	}

	protected Prefab(Engine engine, FileHandleResolver resolver) {
		this(engine, new JsonValuePrefabReader(resolver));
	}

	@Override
	protected final <T extends SaveFileFormat> T create(JsonArtemisSerializer serializer,
	                                                    JsonValue data,
	                                                    Class<T> saveFileFormatClass) {

		return serializer.load(data, saveFileFormatClass);
	}
}
