package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.physics.SensorComponent;

import java.util.Objects;

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

	public float bottomHeightPercent;
	public float leftWidthPercent;
	public float rightWidthPercent;
	public float topHeightPercent;

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

		bottomHeightPercent = vo.bottomHeightPercent;
		leftWidthPercent = vo.leftWidthPercent;
		rightWidthPercent = vo.rightWidthPercent;
		topHeightPercent = vo.topHeightPercent;
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

		bottomHeightPercent = sensorComponent.bottomHeightPercent;
		leftWidthPercent = sensorComponent.leftWidthPercent;
		rightWidthPercent = sensorComponent.rightWidthPercent;
		topHeightPercent = sensorComponent.topHeightPercent;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SensorDataVO that = (SensorDataVO) o;
		return bottom == that.bottom && left == that.left && right == that.right && top == that.top && Float.compare(that.bottomSpanPercent, bottomSpanPercent) == 0 && Float.compare(that.leftSpanPercent, leftSpanPercent) == 0 && Float.compare(that.rightSpanPercent, rightSpanPercent) == 0 && Float.compare(that.topSpanPercent, topSpanPercent) == 0 && Float.compare(that.bottomHeightPercent, bottomHeightPercent) == 0 && Float.compare(that.leftWidthPercent, leftWidthPercent) == 0 && Float.compare(that.rightWidthPercent, rightWidthPercent) == 0 && Float.compare(that.topHeightPercent, topHeightPercent) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bottom, left, right, top, bottomSpanPercent, leftSpanPercent, rightSpanPercent, topSpanPercent, bottomHeightPercent, leftWidthPercent, rightWidthPercent, topHeightPercent);
	}
}
