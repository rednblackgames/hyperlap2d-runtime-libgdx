package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.WidgetPartVO;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EngineConfigurationBuilder;
import games.rednblack.editor.renderer.systems.WidgetStateSystem;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.HyperJson;
import games.rednblack.editor.renderer.widget.handlers.CoreStateOverrides;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WidgetTransitionTest {

    private static final String PRESSED = WidgetTypes.STATE_PRESSED;

    private Engine engine;
    private WidgetStateSystem system;
    private int widget, part;

    @Before
    public void setUp() {
        system = new WidgetStateSystem();
        engine = new Engine(new EngineConfigurationBuilder().with(system).build());

        widget = engine.create();
        WidgetTypes.get(WidgetTypes.BUTTON).setup(engine.edit(widget).create(WidgetComponent.class));

        part = engine.create();
        engine.edit(part).create(ParentNodeComponent.class).parentEntity = widget;
        engine.edit(part).create(MainItemComponent.class);
        engine.edit(part).create(TintComponent.class);
        engine.edit(part).create(ZIndexComponent.class);
        engine.edit(part).create(TransformComponent.class).y = 0;

        // y travels for one second, linearly; rotation jumps
        WidgetPartComponent partComponent = engine.edit(part).create(WidgetPartComponent.class);
        partComponent.setOverride(PRESSED, CoreStateOverrides.Y, "-20.0");
        partComponent.setTransition(PRESSED, CoreStateOverrides.Y, 1f, "linear");
        partComponent.setOverride(PRESSED, CoreStateOverrides.ROTATION, "90.0");

        frame(0);
    }

    private void frame(float delta) {
        engine.setDelta(delta);
        engine.process();
    }

    private void setState(String state) {
        engine.getMapper(WidgetComponent.class).get(widget).currentState = state;
    }

    private TransformComponent transform() {
        return engine.getMapper(TransformComponent.class).get(part);
    }

    @Test
    public void aPropertyWithATransitionTravelsTheOthersJump() {
        setState(PRESSED);
        frame(0);
        assertEquals(0, transform().y, 0.001f);
        assertEquals(90, transform().rotation, 0);

        frame(0.25f);
        assertEquals(-5, transform().y, 0.001f);
        frame(0.5f);
        assertEquals(-15, transform().y, 0.001f);
        frame(0.5f);
        assertEquals(-20, transform().y, 0);
    }

    @Test
    public void goingBackToTheBaseLookUsesTheTransitionOfTheStateBeingLeft() {
        setState(PRESSED);
        frame(0);
        frame(2);
        assertEquals(-20, transform().y, 0);

        setState(null);
        frame(0);
        assertEquals(0, transform().rotation, 0);
        frame(0.5f);
        assertEquals(-10, transform().y, 0.001f);
        frame(0.5f);
        assertEquals(0, transform().y, 0);
    }

    @Test
    public void anInterruptedTransitionTurnsAroundFromWhereItIs() {
        setState(PRESSED);
        frame(0);
        frame(0.5f);
        assertEquals(-10, transform().y, 0.001f);

        // released halfway: no jump, and the base value is still the real one, not the one in flight
        setState(null);
        frame(0);
        assertEquals(-10, transform().y, 0.001f);
        frame(0.5f);
        assertEquals(-5, transform().y, 0.001f);

        // pressed again before it is back
        setState(PRESSED);
        frame(0);
        frame(1);
        assertEquals(-20, transform().y, 0);

        setState(null);
        frame(0);
        frame(1);
        assertEquals(0, transform().y, 0);
    }

    @Test
    public void aStateThatDoesNotCareLetsTheReturnFinish() {
        setState(PRESSED);
        frame(0);
        frame(2);

        setState(null);
        frame(0);
        frame(0.5f);

        // hover says nothing about y: the way back to base goes on as it was
        setState(WidgetTypes.STATE_HOVER);
        frame(0.25f);
        assertEquals(-5, transform().y, 0.001f);
        frame(0.25f);
        assertEquals(0, transform().y, 0);
    }

    @Test
    public void colorsTravelChannelByChannel() {
        WidgetPartComponent partComponent = engine.getMapper(WidgetPartComponent.class).get(part);
        partComponent.setOverride(PRESSED, CoreStateOverrides.TINT, "000000ff");
        partComponent.setTransition(PRESSED, CoreStateOverrides.TINT, 1f, "linear");

        setState(PRESSED);
        frame(0);
        frame(0.5f);

        TintComponent tint = engine.getMapper(TintComponent.class).get(part);
        assertEquals(0.5f, tint.color.r, 0.001f);
        assertEquals(1f, tint.color.a, 0.001f);
    }

    @Test
    public void readingTheBaseLookNeverReplaysTheTransition() {
        ComponentRetriever.initialize(engine);
        setState(PRESSED);
        frame(0);
        frame(0.5f);

        MainItemVO vo = new MainItemVO() {
            @Override
            public String getResourceName() {
                return null;
            }
        };
        vo.loadFromEntity(part, engine, null);
        assertEquals(0, vo.y, 0);
        assertEquals(0, transform().y, 0);

        // back at once, and at the end of the way: a read is nothing to animate
        frame(0);
        assertEquals(-20, transform().y, 0);
    }

    @Test
    public void applyNowLeavesNothingRunning() {
        setState(PRESSED);
        system.applyNow(part);

        assertEquals(-20, transform().y, 0);
        assertEquals(0, engine.getMapper(WidgetPartComponent.class).get(part).tweens.size);
    }

    /** Default values are left out of the file: the entry alone is what says "animated". */
    @Test
    public void aTransitionWithDefaultSettingsIsStillThereAfterALoad() {
        WidgetPartVO vo = new WidgetPartVO();
        vo.setOverride(PRESSED, CoreStateOverrides.Y, "-20.0");
        vo.setTransition(PRESSED, CoreStateOverrides.Y, new games.rednblack.editor.renderer.data.WidgetOverrideTransitionVO());

        String text = HyperJson.newJson().toJson(vo);
        WidgetPartVO read = HyperJson.newJson().fromJson(WidgetPartVO.class, text);

        assertEquals("{\"overrides\":{\"pressed\":{\"values\":{\"y\":\"-20.0\"},\"transitions\":{\"y\":{}}}}}", text);
        assertNotNull(read.getTransition(PRESSED, CoreStateOverrides.Y));
        assertEquals(0.15f, read.getTransition(PRESSED, CoreStateOverrides.Y).duration, 0);
        assertEquals("linear", read.getTransition(PRESSED, CoreStateOverrides.Y).interpolation);
    }

    @Test
    public void transitionsSurviveSerialization() {
        WidgetPartVO vo = new WidgetPartVO();
        vo.loadFromComponent(engine.getMapper(WidgetPartComponent.class).get(part));

        String text = HyperJson.newJson().toJson(vo);
        WidgetPartVO read = HyperJson.newJson().fromJson(WidgetPartVO.class, text);

        assertEquals(vo, read);
        assertEquals(1f, read.getTransition(PRESSED, CoreStateOverrides.Y).duration, 0);
        assertNull(read.getTransition(PRESSED, CoreStateOverrides.ROTATION));
        assertFalse(text.contains("java.lang"));

        // dropping the override drops its transition
        read.setOverride(PRESSED, CoreStateOverrides.Y, null);
        assertNull(read.getTransition(PRESSED, CoreStateOverrides.Y));
    }
}
