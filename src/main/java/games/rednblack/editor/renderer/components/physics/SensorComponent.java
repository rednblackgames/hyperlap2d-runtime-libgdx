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
    	
        needsRefresh = false;
    }

	@Override
	protected void refresh(Entity entity) {
		// TODO create the sensors
	}

}
