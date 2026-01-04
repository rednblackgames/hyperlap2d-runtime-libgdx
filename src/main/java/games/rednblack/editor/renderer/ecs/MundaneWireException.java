package games.rednblack.editor.renderer.ecs;

import com.badlogic.gdx.utils.reflect.ClassReflection;


@SuppressWarnings("serial")
public class MundaneWireException extends RuntimeException {

	public MundaneWireException(Class<? extends BaseSystem> klazz) {
		super("Not added to engine: " + ClassReflection.getSimpleName(klazz));
	}

	public MundaneWireException(String message, Throwable cause) {
		super(message, cause);
	}

	public MundaneWireException(String message) {
		super(message);
	}
}
