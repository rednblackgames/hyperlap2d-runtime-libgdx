package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.widget.ProgressBarComponent;
import games.rednblack.editor.renderer.components.widget.SliderComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.systems.ProgressBarSystem;
import games.rednblack.editor.renderer.systems.SliderSystem;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The slider is 220x40 with a 200x20 track at (10, 5) and a 10 wide knob, so the knob travels 190
 * and the value is at the pointer minus the track's start and half the knob.
 */
public class SliderTest {

    private InputTestScene scene;
    private int slider, background, knob;
    private final List<String> changes = new ArrayList<>();

    @Before
    public void setUp() {
        scene = new InputTestScene(new ProgressBarSystem(), new SliderSystem());

        slider = scene.entity(scene.root, 0, 0, 220, 40);
        WidgetTypes.get(WidgetTypes.SLIDER).setup(scene.engine.edit(slider).create(WidgetComponent.class));
        scene.engine.edit(slider).create(ProgressBarComponent.class);
        scene.engine.edit(slider).create(SliderComponent.class)
                .addListener((entity, value) -> changes.add(String.valueOf(value)));

        background = part(WidgetTypes.ROLE_BACKGROUND, 10, 5, 200, 20);
        knob = part(WidgetTypes.ROLE_KNOB, 0, 0, 10, 30);
        part(WidgetTypes.ROLE_FILL, 0, 0, 1, 1);
        scene.frame();
    }

    private int part(String role, float x, float y, float width, float height) {
        int entity = scene.entity(slider, x, y, width, height);
        TransformComponent transform = scene.engine.getMapper(TransformComponent.class).get(entity);
        transform.originX = 0;
        transform.originY = 0;
        scene.engine.edit(entity).create(WidgetPartComponent.class).role = role;
        scene.frame();
        return entity;
    }

    /** Stands the slider up: a 20x200 track at (10, 5) with a 10 tall knob, the same 190 of travel. */
    private void standUp() {
        setting(WidgetTypes.PROPERTY_VERTICAL, "true");
        size(slider, 40, 220);
        size(background, 20, 200);
        size(knob, 30, 10);
        scene.frame();
    }

    private void size(int entity, float width, float height) {
        DimensionsComponent dimensions = scene.engine.getMapper(DimensionsComponent.class).get(entity);
        dimensions.width = width;
        dimensions.height = height;
    }

    private void setting(String key, String value) {
        scene.engine.getMapper(WidgetComponent.class).get(slider).properties.put(key, value);
    }

    private float value() {
        scene.frame();
        return scene.engine.getMapper(ProgressBarComponent.class).get(slider).value;
    }

    private String state() {
        scene.frame();
        return scene.engine.getMapper(WidgetComponent.class).get(slider).getState();
    }

    @Test
    public void aPressOnTheTrackPutsTheKnobThere() {
        scene.press(110, 20);
        assertEquals(50, value(), 0.001f);
        assertEquals(10 + 0.5f * (200 - 10), scene.engine.getMapper(TransformComponent.class).get(knob).x, 0.001f);
    }

    @Test
    public void theValueFollowsTheDrag() {
        scene.press(110, 20);
        assertEquals(50, value(), 0.001f);

        scene.drag(15 + 0.25f * 190, 20);
        assertEquals(25, value(), 0.001f);

        scene.drag(15 + 0.75f * 190, 20);
        assertEquals(75, value(), 0.001f);
    }

    @Test
    public void theValueStopsAtTheEndsOfTheTrackWhereverThePointerGoes() {
        scene.press(110, 20);

        scene.drag(-500, 20);
        assertEquals(0, value(), 0.001f);
        assertEquals("dragged", state());

        scene.drag(900, 200);
        assertEquals(100, value(), 0.001f);

        scene.release(900, 200);
        assertEquals(100, value(), 0.001f);
        assertEquals("normal", state());
    }

    @Test
    public void theStepSnapsTheValue() {
        setting(WidgetTypes.PROPERTY_STEP, "25");

        scene.press(15 + 0.6f * 190, 20);
        assertEquals(50, value(), 0.001f);

        scene.drag(15 + 0.7f * 190, 20);
        assertEquals(75, value(), 0.001f);
    }

    @Test
    public void aSliderWithoutStepsTakesEveryValue() {
        setting(WidgetTypes.PROPERTY_STEP, "0");

        scene.press(15 + 0.333f * 190, 20);
        assertEquals(33.3f, value(), 0.5f);
    }

    @Test
    public void aStandingSliderFollowsTheOtherAxis() {
        standUp();

        scene.press(20, 10 + 0.5f * 190);
        assertEquals(50, value(), 0.001f);

        scene.drag(20, 10 + 0.9f * 190);
        assertEquals(90, value(), 0.001f);
    }

    @Test
    public void theStateFollowsThePointer() {
        assertEquals("normal", state());

        scene.move(110, 20);
        assertEquals("hover", state());

        scene.press(110, 20);
        assertEquals("dragged", state());

        scene.release(110, 20);
        assertEquals("hover", state());

        scene.move(500, 300);
        assertEquals("normal", state());
    }

    @Test
    public void aDisabledSliderIgnoresThePointer() {
        scene.engine.getMapper(SliderComponent.class).get(slider).isTouchEnabled = false;

        scene.press(110, 20);
        assertEquals(0, value(), 0.001f);
        assertEquals("disabled", state());
    }

    @Test
    public void theListenersHearEachChangeOnce() {
        scene.press(110, 20);
        scene.frame();
        scene.frame();
        assertEquals("[50.0]", changes.toString());

        // the same place again is not a change, a new one is
        scene.drag(110, 20);
        scene.frame();
        scene.drag(15 + 0.8f * 190, 20);
        scene.frame();
        assertEquals("[50.0, 80.0]", changes.toString());
    }

    @Test
    public void theKnobIsAHandleThePointerFinds() {
        scene.press(110, 20);
        scene.release(110, 20);
        scene.frame();

        // the knob sits at 105..115 across and 0..30 up, where the value left it
        InputTargetComponent target = scene.engine.getMapper(InputTargetComponent.class).get(knob);
        assertNotNull(target);
        assertTrue(target.dragHandle);
        assertEquals(knob, scene.hitAt(110, 15));

        // and the press still reaches the slider through it, handle or not
        scene.press(110, 15);
        scene.drag(15 + 0.3f * 190, 15);
        assertEquals(30, value(), 0.001f);
    }

    @Test
    public void codeSettingTheValueTellsTheListenersToo() {
        scene.frame();
        scene.engine.getMapper(ProgressBarComponent.class).get(slider).setValue(42);
        scene.frame();

        assertEquals("[42.0]", changes.toString());
    }
}
