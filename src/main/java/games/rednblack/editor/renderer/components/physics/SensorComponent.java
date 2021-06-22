package games.rednblack.editor.renderer.components.physics;

import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.components.RemovableObject;

/**
 * The component for the sensors.
 *
 * @author Jan-Thierry Wegener
 */
public class SensorComponent extends RefreshableComponent implements RemovableObject {

    protected boolean needsRefresh = false;

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
    public void scheduleRefresh() {
        needsRefresh = true;
    }

    @Override
    public void executeRefresh(int entity) {
        if (needsRefresh) {
            refresh(entity);
            needsRefresh = false;
        }
    }

    protected void refresh(int entity) {
        // TODO create the sensors
    }

}
