package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.physics.SensorComponent;

/**
 * The data transfer object for the sensors.
 * 
 * @author Jan-Thierry Wegener
 */
public class SensorDataVO {

	public boolean bottom;
	public boolean left;
	public boolean right;
	public boolean top;
	
	public SensorDataVO() {
	}
	
	public SensorDataVO(SensorDataVO vo) {
		bottom = vo.bottom;
		left = vo.left;
		right = vo.right;
		top = vo.top;
	}

    public void loadFromComponent(SensorComponent sensorComponent) {
    	bottom = sensorComponent.bottom;
    	left = sensorComponent.left;
    	right = sensorComponent.right;
    	top = sensorComponent.top;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bottom ? 1231 : 1237);
		result = prime * result + (left ? 1231 : 1237);
		result = prime * result + (right ? 1231 : 1237);
		result = prime * result + (top ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SensorDataVO other = (SensorDataVO) obj;
		if (bottom != other.bottom)
			return false;
		if (left != other.left)
			return false;
		if (right != other.right)
			return false;
		if (top != other.top)
			return false;
		return true;
	}
	
}
