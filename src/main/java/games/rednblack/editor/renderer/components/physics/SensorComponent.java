package games.rednblack.editor.renderer.components.physics;

import com.artemis.PooledComponent;

/**
 * The component for the sensors.
 *
 * @author Jan-Thierry Wegener
 */
public class SensorComponent extends PooledComponent {
    public boolean bottom = false;
    public boolean left = false;
    public boolean right = false;
    public boolean top = false;

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
    public void reset() {
        bottom = false;
        left = false;
        right = false;
        top = false;

        bottomSpanPercent = 1.0f;
        leftSpanPercent = 1.0f;
        rightSpanPercent = 1.0f;
        topSpanPercent = 1.0f;
    }
}
