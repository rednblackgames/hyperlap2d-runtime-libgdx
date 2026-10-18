package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EngineConfigurationBuilder;
import games.rednblack.editor.renderer.systems.WidgetStateSystem;
import games.rednblack.editor.renderer.widget.handlers.CoreStateOverrides;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** A state building on another one shows the parent's look, with its own overrides on top. */
public class WidgetInheritanceTest {

    private Engine engine;
    private int widget, part;

    @Before
    public void setUp() {
        engine = new Engine(new EngineConfigurationBuilder().with(new WidgetStateSystem()).build());

        widget = engine.create();
        WidgetTypes.get(WidgetTypes.CHECKBOX).setup(engine.edit(widget).create(WidgetComponent.class));

        part = engine.create();
        engine.edit(part).create(ParentNodeComponent.class).parentEntity = widget;
        engine.edit(part).create(MainItemComponent.class);
        engine.edit(part).create(TintComponent.class);
        engine.edit(part).create(TransformComponent.class);

        // the check mark lives in checked, the hover tint in checked hover only
        WidgetPartComponent partComponent = engine.edit(part).create(WidgetPartComponent.class);
        partComponent.setOverride(WidgetTypes.STATE_CHECKED, CoreStateOverrides.X, "10.0");
        partComponent.setOverride(WidgetTypes.STATE_CHECKED_HOVER, CoreStateOverrides.TINT, "ff0000ff");
        engine.process();
    }

    private void setState(String state) {
        engine.getMapper(WidgetComponent.class).get(widget).currentState = state;
        engine.process();
    }

    private float x() {
        return engine.getMapper(TransformComponent.class).get(part).x;
    }

    private String tint() {
        return engine.getMapper(TintComponent.class).get(part).color.toString();
    }

    @Test
    public void aChildShowsItsParentsLookPlusItsOwn() {
        setState(WidgetTypes.STATE_CHECKED_HOVER);
        assertEquals(10, x(), 0);
        assertEquals("ff0000ff", tint());
    }

    @Test
    public void theParentAloneShowsNothingOfTheChild() {
        setState(WidgetTypes.STATE_CHECKED);
        assertEquals(10, x(), 0);
        assertEquals("ffffffff", tint());
    }

    @Test
    public void goingFromChildToParentDropsOnlyTheChildsOwn() {
        setState(WidgetTypes.STATE_CHECKED_HOVER);
        setState(WidgetTypes.STATE_CHECKED);
        assertEquals(10, x(), 0);
        assertEquals("ffffffff", tint());

        setState(null);
        assertEquals(0, x(), 0);
    }

    @Test
    public void aChildsOwnValueWinsOverItsParents() {
        engine.getMapper(WidgetPartComponent.class).get(part)
                .setOverride(WidgetTypes.STATE_CHECKED_HOVER, CoreStateOverrides.X, "20.0");
        setState(WidgetTypes.STATE_CHECKED_HOVER);
        assertEquals(20, x(), 0);
    }
}
