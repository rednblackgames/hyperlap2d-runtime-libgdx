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

	public float bottomSpanPercent;
	public float leftSpanPercent;
	public float rightSpanPercent;
	public float topSpanPercent;

	public SensorDataVO() {
	}
	
	public SensorDataVO(SensorDataVO vo) {
		bottom = vo.bottom;
		left = vo.left;
		right = vo.right;
		top = vo.top;
		
		bottomSpanPercent = vo.bottomSpanPercent;
		leftSpanPercent = vo.leftSpanPercent;
		rightSpanPercent = vo.rightSpanPercent;
		topSpanPercent = vo.topSpanPercent;
	}

    public void loadFromComponent(SensorComponent sensorComponent) {
    	bottom = sensorComponent.bottom;
    	left = sensorComponent.left;
    	right = sensorComponent.right;
    	top = sensorComponent.top;

    	bottomSpanPercent = sensorComponent.bottomSpanPercent;
    	leftSpanPercent = sensorComponent.leftSpanPercent;
    	rightSpanPercent = sensorComponent.rightSpanPercent;
    	topSpanPercent = sensorComponent.topSpanPercent;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bottom ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(bottomSpanPercent);
		result = prime * result + (left ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(leftSpanPercent);
		result = prime * result + (right ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(rightSpanPercent);
		result = prime * result + (top ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(topSpanPercent);
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
		if (Float.floatToIntBits(bottomSpanPercent) != Float.floatToIntBits(other.bottomSpanPercent))
			return false;
		if (left != other.left)
			return false;
		if (Float.floatToIntBits(leftSpanPercent) != Float.floatToIntBits(other.leftSpanPercent))
			return false;
		if (right != other.right)
			return false;
		if (Float.floatToIntBits(rightSpanPercent) != Float.floatToIntBits(other.rightSpanPercent))
			return false;
		if (top != other.top)
			return false;
		if (Float.floatToIntBits(topSpanPercent) != Float.floatToIntBits(other.topSpanPercent))
			return false;
		return true;
	}

}
