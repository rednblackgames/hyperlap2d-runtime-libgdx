package games.rednblack.editor.renderer.components.physics;

/**
 * This enum is used to check which body sensor collides with another box2d object.
 * 
 * Each surrounding sensor sets the user data to the corresponding enum.
 * 
 * @author Jan-Thierry Wegener
 */
public enum SensorUserData {
	
	/**
	 * Bottom sensor.
	 */
	BOTTOM,
	/**
	 * Left sensor.
	 */
	LEFT,
	/**
	 * Right sensor.
	 */
	RIGHT,
	/**
	 * Top sensor.
	 */
	TOP

}
