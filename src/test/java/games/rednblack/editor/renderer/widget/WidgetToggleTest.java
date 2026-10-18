package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EngineConfigurationBuilder;
import games.rednblack.editor.renderer.systems.WidgetStateSystem;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A property that is simply on or off: its base is on, a state can only turn it off, and leaving
 * that state turns it back on.
 */
public class WidgetToggleTest {

    private static final String KEY = "emitting";

    private Engine engine;
    private int widget, part;
    private boolean on = true;
    private int applied = 0;

    private class Switch implements ToggleOverrideHandler {
        @Override
        public boolean supports(int entity) {
            return true;
        }

        @Override
        public String capture(int entity) {
            return Boolean.toString(on);
        }

        @Override
        public void apply(int entity, String value) {
            on = Boolean.parseBoolean(value);
            applied++;
        }
    }

    @Before
    public void setUp() {
        WidgetStateSystem system = new WidgetStateSystem();
        system.registerHandler(KEY, new Switch());
        engine = new Engine(new EngineConfigurationBuilder().with(system).build());

        widget = engine.create();
        WidgetTypes.get(WidgetTypes.BUTTON).setup(engine.edit(widget).create(WidgetComponent.class));

        part = engine.create();
        engine.edit(part).create(ParentNodeComponent.class).parentEntity = widget;
        engine.edit(part).create(MainItemComponent.class);
        engine.edit(part).create(WidgetPartComponent.class)
                .setOverride(WidgetTypes.STATE_PRESSED, KEY, "false");

        engine.process();
    }

    private void setState(String state) {
        engine.getMapper(WidgetComponent.class).get(widget).currentState = state;
        engine.process();
    }

    @Test
    public void aStateTurnsItOffAndLeavingTurnsItBackOn() {
        assertTrue(on);

        setState(WidgetTypes.STATE_PRESSED);
        assertFalse(on);

        setState(null);
        assertTrue(on);
    }

    @Test
    public void aStateThatSaysNothingLeavesItOn() {
        setState(WidgetTypes.STATE_HOVER);
        assertTrue(on);
    }

    @Test
    public void itIsNotWrittenAgainWhileItAlreadySaysSo() {
        setState(WidgetTypes.STATE_PRESSED);
        int afterTurningOff = applied;

        // a state applied again, e.g. after the look was taken off to be read
        engine.getMapper(WidgetPartComponent.class).get(part).dirty = true;
        engine.process();

        assertEquals(afterTurningOff, applied);
    }
}
