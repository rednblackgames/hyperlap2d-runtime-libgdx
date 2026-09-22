package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.widget.ProgressBarComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import games.rednblack.editor.renderer.utils.InterpolationMap;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * Lays out the parts of a progress bar after its value.
 *
 * The background is the track and the one thing that gives the bar its size. The fill takes the
 * background's rectangle, all of it across the track and the value's share of it along the track,
 * so it never reaches past the background. The knob travels along the track minus its own length,
 * so it stays inside the background at both ends. The composite therefore always measures exactly
 * the background, with automatic resize on or off.
 *
 * Runs after the layout system, so a background placed by constraints is where it should be.
 * The parts are direct children of the widget, so they share its space; the fill and the knob are
 * expected unrotated, the background may be scaled.
 */
@All({ProgressBarComponent.class, WidgetComponent.class})
public class ProgressBarSystem extends IteratingSystem {

    protected ComponentMapper<ProgressBarComponent> progressCM;
    protected ComponentMapper<WidgetComponent> widgetCM;
    protected ComponentMapper<WidgetPartComponent> partCM;
    protected ComponentMapper<NodeComponent> nodeCM;
    protected ComponentMapper<TransformComponent> transformCM;
    protected ComponentMapper<DimensionsComponent> dimensionsCM;

    @Override
    protected void process(int entity) {
        ProgressBarComponent bar = progressCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);

        float min = setting(widget, WidgetTypes.PROPERTY_MIN, 0);
        float max = Math.max(min, setting(widget, WidgetTypes.PROPERTY_MAX, 100));
        float step = setting(widget, WidgetTypes.PROPERTY_STEP, 0);
        bar.min = min;
        bar.max = max;

        // the value setting is where the bar starts: a change of it is followed, a value set by code is kept
        String valueSetting = widget.properties.get(WidgetTypes.PROPERTY_VALUE);
        if (valueSetting == null ? bar.valueSetting != null : !valueSetting.equals(bar.valueSetting)) {
            bar.valueSetting = valueSetting;
            bar.value = parse(valueSetting, min);
        }
        bar.value = snap(MathUtils.clamp(bar.value, min, max), min, step);

        updateVisualValue(bar, widget);
        layoutParts(entity, bar, widget);
    }

    private void updateVisualValue(ProgressBarComponent bar, WidgetComponent widget) {
        float duration = setting(widget, WidgetTypes.PROPERTY_ANIMATE_DURATION, 0);
        if (!bar.started || duration <= 0) {
            bar.started = true;
            bar.visualValue = bar.value;
            bar.animationTarget = bar.value;
            return;
        }

        if (bar.value != bar.animationTarget) {
            // a new value: glide from wherever the bar is drawn now, even halfway through a glide
            bar.animationFrom = bar.visualValue;
            bar.animationTarget = bar.value;
            bar.animationTime = 0;
        }
        if (bar.visualValue == bar.animationTarget) return;

        bar.animationTime = Math.min(duration, bar.animationTime + engine.getDelta());
        Interpolation interpolation = InterpolationMap.map.get(widget.properties.get(WidgetTypes.PROPERTY_ANIMATE_INTERPOLATION));
        if (interpolation == null) interpolation = Interpolation.linear;

        float alpha = interpolation.apply(bar.animationTime / duration);
        bar.visualValue = bar.animationTime >= duration ? bar.animationTarget
                : bar.animationFrom + (bar.animationTarget - bar.animationFrom) * alpha;
    }

    private void layoutParts(int entity, ProgressBarComponent bar, WidgetComponent widget) {
        int background = findPart(entity, WidgetTypes.ROLE_BACKGROUND);
        if (background == -1) return;

        TransformComponent backgroundTransform = transformOf(background);
        DimensionsComponent backgroundDimensions = dimensionsCM.get(background);
        if (backgroundTransform == null || backgroundDimensions == null) return;

        // the background's rectangle as drawn, scale around its origin included
        float trackX = backgroundTransform.x + backgroundTransform.originX * (1 - backgroundTransform.scaleX);
        float trackY = backgroundTransform.y + backgroundTransform.originY * (1 - backgroundTransform.scaleY);
        float trackWidth = backgroundDimensions.width * backgroundTransform.scaleX;
        float trackHeight = backgroundDimensions.height * backgroundTransform.scaleY;

        boolean vertical = Boolean.parseBoolean(widget.properties.get(WidgetTypes.PROPERTY_VERTICAL));
        float percent = MathUtils.clamp(bar.getVisualPercent(), 0, 1);

        int fill = findPart(entity, WidgetTypes.ROLE_FILL);
        if (fill != -1) {
            float width = vertical ? trackWidth : trackWidth * percent;
            float height = vertical ? trackHeight * percent : trackHeight;
            place(fill, trackX, trackY, width, height);
        }

        int knob = findPart(entity, WidgetTypes.ROLE_KNOB);
        if (knob != -1) {
            TransformComponent knobTransform = transformOf(knob);
            DimensionsComponent knobDimensions = dimensionsCM.get(knob);
            if (knobTransform != null && knobDimensions != null) {
                float knobWidth = knobDimensions.width * knobTransform.scaleX;
                float knobHeight = knobDimensions.height * knobTransform.scaleY;
                float x = vertical ? trackX + (trackWidth - knobWidth) / 2 : trackX + percent * (trackWidth - knobWidth);
                float y = vertical ? trackY + percent * (trackHeight - knobHeight) : trackY + (trackHeight - knobHeight) / 2;
                setVisualPosition(knobTransform, x, y);
            }
        }
    }

    /** Puts a part's drawn rectangle exactly there, compensating for its own scale. */
    private void place(int part, float x, float y, float width, float height) {
        TransformComponent transform = transformOf(part);
        DimensionsComponent dimensions = dimensionsCM.get(part);
        if (transform == null || dimensions == null) return;

        float scaleX = transform.scaleX == 0 ? 1 : transform.scaleX;
        float scaleY = transform.scaleY == 0 ? 1 : transform.scaleY;
        dimensions.width = width / scaleX;
        dimensions.height = height / scaleY;
        if (dimensions.boundBox != null) {
            dimensions.boundBox.width = dimensions.width;
            dimensions.boundBox.height = dimensions.height;
        }
        setVisualPosition(transform, x, y);
    }

    private void setVisualPosition(TransformComponent transform, float x, float y) {
        transform.x = x - transform.originX * (1 - transform.scaleX);
        transform.y = y - transform.originY * (1 - transform.scaleY);
    }

    /**
     * A part's real transform. The editor zeroes the transform of the composite it shows from the
     * inside, keeping the real one aside: placing the knob into the zeroed one would move the view
     * and throw every click off. Outside that, the real transform is the transform itself.
     */
    private TransformComponent transformOf(int part) {
        TransformComponent transform = transformCM.get(part);
        return transform == null ? null : transform.getRealComponent();
    }

    /** @return the direct child playing that role, -1 if none does */
    private int findPart(int widget, String role) {
        NodeComponent node = nodeCM.get(widget);
        if (node == null) return -1;

        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            WidgetPartComponent part = partCM.get(child);
            if (part != null && role.equals(part.role)) return child;
        }
        return -1;
    }

    private static float setting(WidgetComponent widget, String key, float fallback) {
        return parse(widget.properties.get(key), fallback);
    }

    private static float parse(String value, float fallback) {
        if (value == null) return fallback;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float snap(float value, float min, float step) {
        if (step <= 0) return value;
        return min + Math.round((value - min) / step) * step;
    }
}
