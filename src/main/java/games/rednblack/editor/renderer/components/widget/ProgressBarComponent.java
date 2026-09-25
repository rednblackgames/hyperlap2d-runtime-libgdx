package games.rednblack.editor.renderer.components.widget;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.widget.WidgetBehaviour;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * The live value of a progress bar widget. Game code sets it here; the value setting of the widget
 * is where it starts, and what the editor shows.
 *
 * The value is kept within the bar's range and on its steps by the
 * {@link games.rednblack.editor.renderer.systems.ProgressBarSystem}, which also makes the bar glide
 * towards it when the widget asks for an animation.
 */
@Transient
public class ProgressBarComponent extends PooledComponent implements WidgetBehaviour {

    /** Where the bar is heading, within its range once the system has seen it. */
    public float value;
    /** Where the bar is drawn right now: the value itself, or on its way there. */
    public float visualValue;

    public float min, max;

    /** Last seen value setting, so a change of setting is followed but a value set by code is not undone. */
    public String valueSetting = null;
    public boolean started = false;
    public float animationFrom, animationTarget, animationTime;

    @Override
    public void initialise(WidgetComponent widget) {
        valueSetting = widget.properties.get(WidgetTypes.PROPERTY_VALUE);
        value = WidgetComponent.parse(valueSetting, 0);
    }

    public void setValue(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    /** @return where the bar is drawn, from zero to one along its track */
    public float getVisualPercent() {
        return max > min ? (visualValue - min) / (max - min) : 0;
    }

    @Override
    public void reset() {
        value = 0;
        visualValue = 0;
        min = 0;
        max = 0;
        valueSetting = null;
        started = false;
        animationFrom = 0;
        animationTarget = 0;
        animationTime = 0;
    }
}
