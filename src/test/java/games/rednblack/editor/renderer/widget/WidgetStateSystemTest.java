package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EngineConfigurationBuilder;
import games.rednblack.editor.renderer.systems.WidgetStateSystem;
import games.rednblack.editor.renderer.widget.handlers.CoreStateOverrides;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WidgetStateSystemTest {

    private Engine engine;
    private WidgetStateSystem system;
    private int widget, group, part;

    @Before
    public void setUp() {
        system = new WidgetStateSystem();
        engine = new Engine(new EngineConfigurationBuilder().with(system).build());

        // widget > group > part: a part follows its nearest widget ancestor, not only its parent
        widget = engine.create();
        WidgetTypes.get(WidgetTypes.BUTTON).setup(engine.edit(widget).create(WidgetComponent.class));

        group = engine.create();
        engine.edit(group).create(ParentNodeComponent.class).parentEntity = widget;

        part = engine.create();
        engine.edit(part).create(ParentNodeComponent.class).parentEntity = group;
        engine.edit(part).create(MainItemComponent.class);
        engine.edit(part).create(TintComponent.class);
        TransformComponent transform = engine.edit(part).create(TransformComponent.class);
        transform.x = 10;

        WidgetPartComponent partComponent = engine.edit(part).create(WidgetPartComponent.class);
        partComponent.setOverride(WidgetTypes.STATE_PRESSED, CoreStateOverrides.X, "25");
        partComponent.setOverride(WidgetTypes.STATE_PRESSED, CoreStateOverrides.TINT, "ff0000ff");
        partComponent.setOverride(WidgetTypes.STATE_CHECKED, CoreStateOverrides.VISIBLE, "false");

        engine.process();
    }

    private WidgetComponent widget() {
        return engine.getMapper(WidgetComponent.class).get(widget);
    }

    private float x() {
        return engine.getMapper(TransformComponent.class).get(part).x;
    }

    private boolean visible() {
        return engine.getMapper(MainItemComponent.class).get(part).visible;
    }

    private String tint() {
        return engine.getMapper(TintComponent.class).get(part).color.toString();
    }

    @Test
    public void defaultStateKeepsBaseLook() {
        assertEquals(10, x(), 0);
        assertEquals("ffffffff", tint());
        assertTrue(visible());
    }

    @Test
    public void stateOverridesAreAppliedAndRestored() {
        assertTrue(widget().setState(WidgetTypes.STATE_PRESSED));
        engine.process();
        assertEquals(25, x(), 0);
        assertEquals("ff0000ff", tint());

        widget().currentState = null;
        engine.process();
        assertEquals(10, x(), 0);
        assertEquals("ffffffff", tint());
    }

    @Test
    public void switchingBetweenStatesNeverMixesThem() {
        widget().setState(WidgetTypes.STATE_PRESSED);
        engine.process();
        widget().setState(WidgetTypes.STATE_CHECKED);
        engine.process();

        assertEquals(10, x(), 0);
        assertEquals("ffffffff", tint());
        assertFalse(visible());
    }

    @Test
    public void undeclaredStateIsRejected() {
        assertFalse(widget().setState("exploded"));
        assertEquals(WidgetTypes.STATE_NORMAL, widget().getState());
    }

    @Test
    public void baseChangesMadeWhileInAStateSurvive() {
        widget().setState(WidgetTypes.STATE_CHECKED);
        engine.process();

        // x is not overridden by "checked", so moving the part is a change of its base look
        engine.getMapper(TransformComponent.class).get(part).x = 40;

        widget().currentState = null;
        engine.process();
        assertEquals(40, x(), 0);
        assertTrue(visible());
    }

    @Test
    public void restoreBaseThenReapply() {
        widget().setState(WidgetTypes.STATE_PRESSED);
        engine.process();

        system.restoreBase(part);
        assertEquals(10, x(), 0);

        engine.process();
        assertEquals(25, x(), 0);
    }

    @Test
    public void valueObjectsAlwaysDescribeTheBaseLook() {
        engine.edit(part).create(ZIndexComponent.class);
        ComponentRetriever.initialize(engine);

        widget().setState(WidgetTypes.STATE_PRESSED);
        engine.process();

        MainItemVO vo = new MainItemVO() {
            @Override
            public String getResourceName() {
                return null;
            }
        };
        vo.loadFromEntity(part, engine, null);

        assertEquals(10, vo.x, 0);
        assertEquals(1f, vo.tint[1], 0);
        assertEquals("25", vo.widgetPart.getOverride(WidgetTypes.STATE_PRESSED, CoreStateOverrides.X));

        // the state comes back on the next pass
        engine.process();
        assertEquals(25, x(), 0);
    }

    @Test
    public void applyNowDoesNotWaitForTheNextPass() {
        widget().setState(WidgetTypes.STATE_PRESSED);
        system.applyNow(part);
        assertEquals(25, x(), 0);
    }

    @Test
    public void editedOverridesAreRefreshed() {
        widget().setState(WidgetTypes.STATE_PRESSED);
        engine.process();

        WidgetPartComponent partComponent = engine.getMapper(WidgetPartComponent.class).get(part);
        partComponent.removeOverride(WidgetTypes.STATE_PRESSED, CoreStateOverrides.X);
        engine.process();

        assertEquals(10, x(), 0);
        assertEquals("ff0000ff", tint());
    }

    /**
     * The editor zeroes the transform of the composite it shows from the inside, keeping the real one
     * aside. States must neither read the zeroed one nor write into it.
     */
    @Test
    public void aCompositeShownFromTheInsideKeepsItsViewTransform() {
        widget().setState(WidgetTypes.STATE_PRESSED);
        engine.process();
        TransformComponent transform = engine.getMapper(TransformComponent.class).get(part);
        assertEquals(25, transform.x, 0);

        // entering it is not an edit: its position still reads as the real one
        transform.disableTransform();
        assertEquals(0, transform.x, 0);
        assertEquals("25.0", system.getHandler(CoreStateOverrides.X).capture(part));

        // a state change while inside leaves the view where it is
        widget().currentState = null;
        engine.process();
        assertEquals(0, transform.x, 0);

        // and leaving it puts back the look of the state now shown
        transform.enableTransform();
        assertEquals(10, transform.x, 0);
    }

    @Test
    public void partOutsideAnyWidgetShowsBaseLook() {
        widget().setState(WidgetTypes.STATE_PRESSED);
        engine.process();

        engine.getMapper(ParentNodeComponent.class).get(group).parentEntity = -1;
        engine.process();
        assertEquals(10, x(), 0);
    }
}
