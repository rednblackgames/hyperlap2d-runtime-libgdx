package games.rednblack.editor.renderer.ecs.prefab;

import games.rednblack.editor.renderer.ecs.io.SerializationException;

public class MissingPrefabDataException extends SerializationException {
	public MissingPrefabDataException() {
	}

	public MissingPrefabDataException(String message) {
		super(message);
	}

	public MissingPrefabDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public MissingPrefabDataException(Throwable cause) {
		super(cause);
	}
}
