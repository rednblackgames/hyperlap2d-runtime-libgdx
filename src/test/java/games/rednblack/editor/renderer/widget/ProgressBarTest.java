package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.widget.ProgressBarComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EngineConfigurationBuilder;
import games.rednblack.editor.renderer.systems.ProgressBarSystem;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProgressBarTest {

    private Engine engine;
    private int bar, background, fill, knob;

    @Before
    public void setUp() {
        engine = new Engine(new EngineConfigurationBuilder().with(new ProgressBarSystem()).build());

        bar = engine.create();
        engine.edit(bar).create(NodeComponent.class);
        WidgetTypes.get(WidgetTypes.PROGRESS_BAR).setup(engine.edit(bar).create(WidgetComponent.class));
        engine.edit(bar).create(ProgressBarComponent.class);

        background = part(WidgetTypes.ROLE_BACKGROUND, 10, 5, 200, 20);
        fill = part(WidgetTypes.ROLE_FILL, 0, 0, 1, 1);
        knob = part(WidgetTypes.ROLE_KNOB, 0, 0, 10, 30);
        setting(WidgetTypes.PROPERTY_VALUE, "50");

        // what the entity factory does as the scene loads
        engine.getMapper(ProgressBarComponent.class).get(bar).initialise(engine.getMapper(WidgetComponent.class).get(bar));
    }

    private int part(String role, float x, float y, float width, float height) {
        int entity = engine.create();
        engine.edit(entity).create(ParentNodeComponent.class).parentEntity = bar;
        engine.getMapper(NodeComponent.class).get(bar).addChild(entity);
        TransformComponent transform = engine.edit(entity).create(TransformComponent.class);
        transform.x = x;
        transform.y = y;
        transform.originX = 0;
        transform.originY = 0;
        DimensionsComponent dimensions = engine.edit(entity).create(DimensionsComponent.class);
        dimensions.width = width;
        dimensions.height = height;
        engine.edit(entity).create(WidgetPartComponent.class).role = role;
        return entity;
    }

    private void setting(String key, String value) {
        engine.getMapper(WidgetComponent.class).get(bar).properties.put(key, value);
    }

    private void frame(float delta) {
        engine.setDelta(delta);
        engine.process();
    }

    private TransformComponent transform(int entity) {
        return engine.getMapper(TransformComponent.class).get(entity);
    }

    private DimensionsComponent dimensions(int entity) {
        return engine.getMapper(DimensionsComponent.class).get(entity);
    }

    @Test
    public void theFillTakesTheValuesShareOfTheBackground() {
        frame(0);
        assertEquals(10, transform(fill).x, 0);
        assertEquals(5, transform(fill).y, 0);
        assertEquals(100, dimensions(fill).width, 0.001f);
        assertEquals(20, dimensions(fill).height, 0);
    }

    @Test
    public void theKnobStaysInsideTheBackgroundAtBothEnds() {
        frame(0);
        assertEquals(10 + 0.5f * (200 - 10), transform(knob).x, 0.001f);
        assertEquals(5 + (20 - 30) / 2f, transform(knob).y, 0.001f);

        setting(WidgetTypes.PROPERTY_VALUE, "0");
        frame(0);
        assertEquals(10, transform(knob).x, 0.001f);

        setting(WidgetTypes.PROPERTY_VALUE, "100");
        frame(0);
        assertEquals(10 + 200 - 10, transform(knob).x, 0.001f);
    }

    @Test
    public void aVerticalBarFillsUpwards() {
        setting(WidgetTypes.PROPERTY_VERTICAL, "true");
        frame(0);
        assertEquals(200, dimensions(fill).width, 0);
        assertEquals(10, dimensions(fill).height, 0.001f);
    }

    @Test
    public void theValueStaysInRangeAndOnItsSteps() {
        ProgressBarComponent component = engine.getMapper(ProgressBarComponent.class).get(bar);
        component.setValue(150);
        frame(0);
        assertEquals(100, component.getValue(), 0);

        setting(WidgetTypes.PROPERTY_STEP, "10");
        component.setValue(43);
        frame(0);
        assertEquals(40, component.getValue(), 0);
    }

    @Test
    public void aValueSetByCodeIsNotUndoneByTheSetting() {
        frame(0);
        engine.getMapper(ProgressBarComponent.class).get(bar).setValue(80);
        frame(0);
        assertEquals(160, dimensions(fill).width, 0.001f);
    }

    /**
     * The editor zeroes the transform of a composite it shows from the inside, keeping the real one
     * aside: the bar must keep moving the knob's real position and leave the view alone.
     */
    @Test
    public void aKnobShownFromTheInsideKeepsItsViewTransform() {
        frame(0);
        TransformComponent knobTransform = transform(knob);
        knobTransform.disableTransform();

        setting(WidgetTypes.PROPERTY_VALUE, "100");
        frame(0);
        assertEquals(0, knobTransform.x, 0);
        assertEquals(0, knobTransform.y, 0);

        // leaving it puts back a position that followed the value meanwhile
        knobTransform.enableTransform();
        assertEquals(10 + 200 - 10, knobTransform.x, 0.001f);
    }

    @Test
    public void theBarGlidesWhenAskedTo() {
        setting(WidgetTypes.PROPERTY_ANIMATE_DURATION, "1");
        frame(0);
        assertEquals(100, dimensions(fill).width, 0.001f);

        setting(WidgetTypes.PROPERTY_VALUE, "100");
        frame(0.5f);
        assertEquals(150, dimensions(fill).width, 0.001f);
        frame(0.5f);
        assertEquals(200, dimensions(fill).width, 0.001f);
    }
}
