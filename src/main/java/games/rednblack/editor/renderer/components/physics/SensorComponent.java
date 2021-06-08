package games.rednblack.editor.renderer.components.physics;

import com.badlogic.ashley.core.Entity;

import games.rednblack.editor.renderer.commons.RefreshableObject;
import games.rednblack.editor.renderer.components.RemovableComponent;

/**
 * The component for the sensors.
 * 
 * @author Jan-Thierry Wegener
 */
public class SensorComponent extends RefreshableObject implements RemovableComponent {
	
	public boolean bottom;
	public boolean left;
	public boolean right;
	public boolean top;

	/**
	 * The width of the sensor in percents of the body.
	 */
	public float bottomSpanPercent = 1.0f;
	/**
	 * The height of the sensor in percents of the body.
	 */
	public float leftSpanPercent = 1.0f;
	/**
	 * The height of the sensor in percents of the body.
	 */
	public float rightSpanPercent = 1.0f;
	/**
	 * The width of the sensor in percents of the body.
	 */
	public float topSpanPercent = 1.0f;

	public SensorComponent() {
	}

    @Override
    public void onRemove() {
    }

    @Override
    public void reset() {
    	bottom = false;
    	left = false;
    	right = false;
    	top = false;
    	
    	bottomSpanPercent = 1.0f;
    	leftSpanPercent = 1.0f;
    	rightSpanPercent = 1.0f;
    	topSpanPercent = 1.0f;
    	
        needsRefresh = false;
    }

	@Override
	protected void refresh(Entity entity) {
		// TODO create the sensors
	}

}
