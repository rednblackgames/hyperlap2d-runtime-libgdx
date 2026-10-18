package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.systems.ButtonSystem;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ButtonSystemTest {

    private InputTestScene scene;
    private int button;
    private final List<String> events = new ArrayList<>();

    @Before
    public void setUp() {
        scene = new InputTestScene(new ButtonSystem());
        button = createButton("b", 0);
    }

    /** A 100x100 button widget at the given place, taking the whole height of the scene's bottom. */
    private int createButton(final String name, float x) {
        int entity = scene.entity(scene.root, x, 0, 100, 100);
        WidgetTypes.get(WidgetTypes.BUTTON).setup(scene.engine.edit(entity).create(WidgetComponent.class));

        scene.engine.edit(entity).create(ButtonComponent.class).addListener(new ButtonComponent.ButtonListener() {
            @Override
            public void touchUp(int e) {
                events.add(name + ":up");
            }

            @Override
            public void touchDown(int e) {
                events.add(name + ":down");
            }

            @Override
            public void clicked(int e) {
                events.add(name + ":clicked");
            }
        });
        scene.frame();
        return entity;
    }

    private String state(int entity) {
        scene.frame();
        return scene.engine.getMapper(WidgetComponent.class).get(entity).getState();
    }

    private String state() {
        return state(button);
    }

    @Test
    public void hoverThenPressThenClick() {
        scene.move(500, 50);
        assertEquals("normal", state());

        scene.move(50, 50);
        assertEquals("hover", state());

        scene.press(50, 50);
        assertEquals("pressed", state());

        scene.drag(60, 50);
        assertEquals("pressed", state());

        scene.release(60, 50);
        assertEquals("hover", state());

        assertEquals("[b:down, b:up, b:clicked]", events.toString());
    }

    @Test
    public void aHeldPressFollowsThePointerInAndOut() {
        scene.move(50, 50);
        scene.press(50, 50);
        assertEquals("pressed", state());

        scene.drag(500, 50);
        assertEquals("normal", state());

        scene.drag(50, 50);
        assertEquals("pressed", state());

        scene.drag(500, 50);
        assertEquals("normal", state());

        scene.release(500, 50);
        assertEquals("normal", state());

        // let go outside: released, not clicked
        assertEquals("[b:down, b:up]", events.toString());
    }

    @Test
    public void aPressStartedElsewhereNeverPressesTheButton() {
        scene.press(500, 50);
        scene.drag(50, 50);
        assertEquals("normal", state());

        scene.release(50, 50);
        scene.move(50, 50);
        assertEquals("hover", state());

        assertEquals("[]", events.toString());
    }

    @Test
    public void theButtonInFrontTakesThePress() {
        // added later, so it is the one drawn last: x=70 is over both
        int inFront = createButton("front", 50);

        scene.press(70, 50);
        assertEquals("pressed", state(inFront));
        assertEquals("normal", state(button));

        scene.release(70, 50);
        assertEquals("[front:down, front:up, front:clicked]", events.toString());
    }

    @Test
    public void aSecondPointerDoesNotTakeAButtonAlreadyHeld() {
        scene.press(50, 50, 0, 0);
        scene.press(60, 50, 1, 0);
        assertEquals("pressed", state());

        // the first pointer still owns it: letting the second one go changes nothing
        scene.release(60, 50, 1, 0);
        assertEquals("pressed", state());

        scene.release(50, 50, 0, 0);
        assertEquals("[b:down, b:up, b:clicked]", events.toString());
    }

    @Test
    public void aDisabledButtonLetsGoOfItsPress() {
        scene.press(50, 50);
        assertEquals("pressed", state());

        scene.engine.getMapper(ButtonComponent.class).get(button).isTouchEnabled = false;
        assertEquals("disabled", state());

        scene.engine.getMapper(ButtonComponent.class).get(button).isTouchEnabled = true;
        // still the same held press: it was dropped, it does not press again
        scene.drag(50, 50);
        assertEquals("normal", state());
    }
}
