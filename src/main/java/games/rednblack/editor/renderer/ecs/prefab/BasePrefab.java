package games.rednblack.editor.renderer.ecs.prefab;

import games.rednblack.editor.renderer.ecs.MundaneWireException;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.annotations.PrefabData;
import games.rednblack.editor.renderer.ecs.io.SaveFileFormat;
import games.rednblack.editor.renderer.ecs.managers.EngineSerializationManager;
import games.rednblack.editor.renderer.ecs.managers.EngineSerializationManager.ArtemisSerializer;
import games.rednblack.editor.renderer.ecs.utils.reflect.ReflectionUtil;

/**
 * Shared functionality for prefabs. {@link #create()} is expected to be wrapped
 * by concrete prefab implementations, e.g. inside <code>PlayerPrefab::create(color, x, y)</code>.
 *
 * @param <DATA> Data source
 * @param <SERIALIZER> Serializer, one of libgdx's or json-beans.
 */
public abstract class BasePrefab<DATA, SERIALIZER extends ArtemisSerializer> {
	protected final Engine engine;
	private final PrefabReader<DATA> data;
	private EngineSerializationManager serializationManager;

	protected BasePrefab(Engine engine, PrefabReader<DATA> data) {
		this.engine = engine;
		this.data = data;

		serializationManager = engine.getSystem(EngineSerializationManager.class);
		if (serializationManager == null)
			throw new MundaneWireException(EngineSerializationManager.class);

		engine.inject(this);

		// TODO: #439 - generate .class  from .json
		data.initialize(getPrefabDataPath());
	}

	private String getPrefabDataPath() {
		PrefabData pd = ReflectionUtil.getAnnotation(getClass(), PrefabData.class);
		if (pd != null) {
			return pd.value();
		} else {
			String annotation = PrefabData.class.getSimpleName();
			String message = getClass().getName() + " must be annotated with @" + annotation;
			throw new MissingPrefabDataException(message);
		}
	}

	public final SaveFileFormat create() {
		SERIALIZER serializer = serializationManager.getSerializer();
		return create(serializer, data.getData(), saveFileFormat());
	}

	protected abstract <T extends SaveFileFormat> T create(SERIALIZER serializer,
	                                                       DATA data,
	                                                       Class<T> saveFileFormatClass);

	protected Class<SaveFileFormat> saveFileFormat() {
		return SaveFileFormat.class;
	}
}
