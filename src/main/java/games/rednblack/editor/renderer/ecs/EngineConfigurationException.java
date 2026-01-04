package games.rednblack.editor.renderer.ecs;

import java.lang.RuntimeException;import java.lang.String;import java.lang.Throwable; /**
 * Engine configuration failed.
 *
 * @author Daan van Yperen
 */
public class EngineConfigurationException extends RuntimeException {
	public EngineConfigurationException(String msg) {
		super(msg);
	}

	public EngineConfigurationException(String msg, Throwable e) {
		super(msg,e);
	}
}
