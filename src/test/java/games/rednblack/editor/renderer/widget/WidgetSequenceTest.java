package games.rednblack.editor.renderer.widget;

import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.data.WidgetOverrideSequenceVO;
import games.rednblack.editor.renderer.data.WidgetPartVO;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EngineConfigurationBuilder;
import games.rednblack.editor.renderer.systems.WidgetStateSystem;
import games.rednblack.editor.renderer.utils.HyperJson;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The chaining itself, driven through a handler that only writes down what it is told, so the test
 * says nothing about skeletons or frame ranges.
 */
public class WidgetSequenceTest {

    private static final String KEY = "animation";
    private static final String PRESSED = WidgetTypes.STATE_PRESSED;
    private static final String HOVER = WidgetTypes.STATE_HOVER;

    private Engine engine;
    private int widget, part;
    private final List<String> played = new ArrayList<>();

    /** Writes down every call as {@code exit>enter>value}, leaving out whatever is missing. */
    private class Recorder implements SequencedOverrideHandler, ChoiceOverrideHandler {
        private String current = "idle";

        @Override
        public boolean supports(int entity) {
            return true;
        }

        @Override
        public String capture(int entity) {
            return current;
        }

        @Override
        public void apply(int entity, String value) {
            applySequence(entity, value, null, null);
        }

        @Override
        public void applySequence(int entity, String value, String enter, String exit) {
            StringBuilder chain = new StringBuilder();
            if (exit != null) chain.append(exit).append('>');
            if (enter != null) chain.append(enter).append('>');
            played.add(chain.append(value).toString());
            current = value;
        }

        @Override
        public Array<String> getChoices(int entity) {
            return new Array<>(new String[]{"idle", "press-start", "press-loop", "press-end"});
        }
    }

    @Before
    public void setUp() {
        WidgetStateSystem system = new WidgetStateSystem();
        system.registerHandler(KEY, new Recorder());
        engine = new Engine(new EngineConfigurationBuilder().with(system).build());

        widget = engine.create();
        WidgetTypes.get(WidgetTypes.BUTTON).setup(engine.edit(widget).create(WidgetComponent.class));

        part = engine.create();
        engine.edit(part).create(ParentNodeComponent.class).parentEntity = widget;
        engine.edit(part).create(MainItemComponent.class);

        WidgetPartComponent partComponent = engine.edit(part).create(WidgetPartComponent.class);
        partComponent.setOverride(PRESSED, KEY, "press-loop");
        partComponent.setSequence(PRESSED, KEY, "press-start", "press-end");

        engine.process();
        played.clear();
    }

    private void setState(String state) {
        engine.getMapper(WidgetComponent.class).get(widget).currentState = state;
        engine.process();
    }

    @Test
    public void enteringPlaysTheEntryBeforeTheLoop() {
        setState(PRESSED);
        assertEquals("[press-start>press-loop]", played.toString());
    }

    @Test
    public void leavingPlaysTheExitOfTheStateBeingLeft() {
        setState(PRESSED);
        played.clear();

        setState(null);
        assertEquals("[press-end>idle]", played.toString());
    }

    @Test
    public void goingStraightToAnotherStatePlaysBothEnds() {
        WidgetPartComponent partComponent = engine.getMapper(WidgetPartComponent.class).get(part);
        partComponent.setOverride(HOVER, KEY, "hover-loop");
        partComponent.setSequence(HOVER, KEY, "hover-start", "");

        setState(PRESSED);
        played.clear();

        setState(HOVER);
        assertEquals("[press-end>hover-start>hover-loop]", played.toString());
    }

    @Test
    public void aStateWithNothingAroundItsValueJustPlaysIt() {
        WidgetPartComponent partComponent = engine.getMapper(WidgetPartComponent.class).get(part);
        partComponent.removeSequence(PRESSED, KEY);

        setState(PRESSED);
        assertEquals("[press-loop]", played.toString());
    }

    @Test
    public void anEntryPlaysEvenWhenTheLoopIsWhatIsAlreadyPlaying() {
        WidgetPartComponent partComponent = engine.getMapper(WidgetPartComponent.class).get(part);
        partComponent.setOverride(PRESSED, KEY, "idle");

        setState(PRESSED);
        assertEquals("[press-start>idle]", played.toString());
    }

    @Test
    public void droppingTheOverrideDropsWhatPlayedAroundIt() {
        WidgetPartVO vo = new WidgetPartVO();
        vo.setOverride(PRESSED, KEY, "press-loop");
        vo.setSequence(PRESSED, KEY, new WidgetOverrideSequenceVO("press-start", "press-end"));

        vo.setOverride(PRESSED, KEY, null);
        assertNull(vo.getSequence(PRESSED, KEY));
    }

    @Test
    public void chainsSurviveSerialization() {
        WidgetPartVO vo = new WidgetPartVO();
        vo.loadFromComponent(engine.getMapper(WidgetPartComponent.class).get(part));

        String text = HyperJson.newJson().toJson(vo);
        WidgetPartVO read = HyperJson.newJson().fromJson(WidgetPartVO.class, text);

        assertEquals(vo, read);
        assertEquals("press-start", read.getSequence(PRESSED, KEY).enter);
        assertEquals("press-end", read.getSequence(PRESSED, KEY).exit);
        assertFalse(text.contains("java.lang"));
    }
}
