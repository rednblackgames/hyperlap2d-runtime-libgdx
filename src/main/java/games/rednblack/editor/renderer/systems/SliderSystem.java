package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.widget.ProgressBarComponent;
import games.rednblack.editor.renderer.components.widget.SliderComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * Turns pointers into the value of a slider, and tells the listeners when it changes.
 *
 * The knob always goes where the pointer is: pressing the track puts it there and dragging carries
 * it along, which is one rule instead of two and is how a slider behaves elsewhere. The press
 * belongs to the slider until it is released, so the pointer may wander off the widget and the
 * value simply stops at the end of the track.
 *
 * Everything else about a slider is the progress bar underneath it: the range, the step, the value
 * and the placing of the parts.
 */
@All({SliderComponent.class, ProgressBarComponent.class, WidgetComponent.class})
public class SliderSystem extends BaseEntitySystem implements UIInputListener {

    protected ComponentMapper<SliderComponent> sliderCM;
    protected ComponentMapper<ProgressBarComponent> barCM;
    protected ComponentMapper<WidgetComponent> widgetCM;
    protected ComponentMapper<ViewPortComponent> viewPortCM;
    protected ComponentMapper<InputTargetComponent> inputTargetCM;

    /** Wired by the engine: the bar underneath knows where the track and the knob are. */
    protected ProgressBarSystem progressBarSystem;
    protected UIInputSystem inputSystem;

    private final Rectangle track = new Rectangle();
    private final Vector2 knob = new Vector2();

    public SliderSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public SliderSystem() {
    }

    /** Makes the slider an input target, so the pointer can find it, and listens to it. */
    @Override
    protected void inserted(int entityId) {
        inputTargetCM.create(entityId).addListener(this);
    }

    @Override
    protected void removed(int entityId) {
        InputTargetComponent target = inputTargetCM.get(entityId);
        if (target != null) target.removeListener(this);
        if (inputSystem != null) inputSystem.cancelTouchFocus(entityId);
    }

    @Override
    protected final void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        for (int i = 0, s = actives.size(); i < s; i++) {
            process(ids[i]);
        }
    }

    protected void process(int entity) {
        SliderComponent slider = sliderCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);

        //the composite being viewed from the inside in the editor is opened, not dragged
        if (viewPortCM.has(entity) || !slider.isTouchEnabled) releaseSilently(entity, slider);

        markKnob(entity);

        notifyValue(entity, slider);
        updateWidgetState(entity, widget, slider);
    }

    /**
     * Makes the knob a handle the pointer can find. It listens to nothing of its own, so the press
     * travels up to the slider and is handled there exactly as a press on the track is; what the
     * handle says is that dragging it drags the value, and nothing else.
     */
    private void markKnob(int entity) {
        if (progressBarSystem == null) return;

        int knob = progressBarSystem.findPart(entity, WidgetTypes.ROLE_KNOB);
        if (knob == -1) return;

        InputTargetComponent target = inputTargetCM.get(knob);
        if (target == null) target = inputTargetCM.create(knob);
        target.dragHandle = true;
    }

    /** Picks the most relevant state the widget declares: disabled, dragged, hover, default. */
    protected void updateWidgetState(int entity, WidgetComponent widget, SliderComponent slider) {
        if (!slider.isTouchEnabled && widget.setState(WidgetTypes.STATE_DISABLED)) return;
        if (slider.isDragging() && widget.setState(WidgetTypes.STATE_DRAGGED)) return;
        if (slider.isHovered && widget.setState(WidgetTypes.STATE_HOVER)) return;
        widget.currentState = null; //back to the default state
    }

    /** Tells the listeners once per change, whoever made it: a drag, the settings or game code. */
    private void notifyValue(int entity, SliderComponent slider) {
        ProgressBarComponent bar = barCM.get(entity);
        if (bar == null) return;

        if (!slider.notified) {
            slider.notified = true;
            slider.notifiedValue = bar.value;
            return;
        }
        if (bar.value == slider.notifiedValue) return;

        slider.notifiedValue = bar.value;
        for (int i = 0; i < slider.listeners.size; i++) {
            slider.listeners.get(i).valueChanged(entity, bar.value);
        }
    }

    // ------------------------------------------------------------------ the pointer

    @Override
    public void touchDown(UIEvent event) {
        int entity = event.listenerEntity;
        SliderComponent slider = sliderCM.get(entity);
        if (slider == null || !slider.isTouchEnabled) return;
        if (viewPortCM.has(entity)) return;
        //a second pointer does not take a slider already held
        if (slider.touchPointer != -1) return;

        slider.touchPointer = event.pointer;
        moveTo(entity, event.localX, event.localY);
        event.handle();
    }

    @Override
    public void touchDragged(UIEvent event) {
        int entity = event.listenerEntity;
        SliderComponent slider = sliderCM.get(entity);
        if (slider == null || slider.touchPointer != event.pointer) return;

        moveTo(entity, event.localX, event.localY);
    }

    @Override
    public void touchUp(UIEvent event) {
        int entity = event.listenerEntity;
        SliderComponent slider = sliderCM.get(entity);
        if (slider == null || slider.touchPointer != event.pointer) return;

        slider.touchPointer = -1;
    }

    @Override
    public void enter(UIEvent event) {
        SliderComponent slider = sliderCM.get(event.listenerEntity);
        if (slider != null) slider.isHovered = true;
    }

    @Override
    public void exit(UIEvent event) {
        SliderComponent slider = sliderCM.get(event.listenerEntity);
        if (slider != null) slider.isHovered = false;
    }

    /**
     * Puts the middle of the knob under the pointer, as far as the track allows.
     *
     * @param localX the pointer in the widget's own coordinates, where the track is measured
     */
    private void moveTo(int entity, float localX, float localY) {
        ProgressBarComponent bar = barCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);
        if (bar == null || widget == null || progressBarSystem == null) return;
        if (!progressBarSystem.track(entity, track)) return;
        progressBarSystem.knobSize(entity, knob);

        boolean vertical = Boolean.parseBoolean(widget.properties.get(WidgetTypes.PROPERTY_VERTICAL));
        float travel = vertical ? track.height - knob.y : track.width - knob.x;
        float position = vertical ? localY - track.y - knob.y / 2 : localX - track.x - knob.x / 2;
        float percent = travel <= 0 ? 0 : MathUtils.clamp(position / travel, 0, 1);

        float min = ProgressBarSystem.setting(widget, WidgetTypes.PROPERTY_MIN, 0);
        float max = Math.max(min, ProgressBarSystem.setting(widget, WidgetTypes.PROPERTY_MAX, 100));
        float step = ProgressBarSystem.setting(widget, WidgetTypes.PROPERTY_STEP, 0);

        bar.value = MathUtils.clamp(ProgressBarSystem.snap(min + percent * (max - min), min, step), min, max);
    }

    /** Drops the pointer the slider may be holding, without a word to the listeners. */
    private void releaseSilently(int entity, SliderComponent slider) {
        if (slider.touchPointer != -1 && inputSystem != null) inputSystem.cancelTouchFocus(entity);
        slider.touchPointer = -1;
        slider.isHovered = false;
    }
}
