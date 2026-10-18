package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.systems.ButtonSystem;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CheckBoxTest {

    private InputTestScene scene;
    private int group;
    private final List<String> changes = new ArrayList<>();

    @Before
    public void setUp() {
        scene = new InputTestScene(new ButtonSystem());

        // a plain composite holding radio buttons, the way a group is built
        group = scene.entity(scene.root, 0, 0, InputTestScene.WIDTH, InputTestScene.HEIGHT);
    }

    /** A 100x100 checkbox at x, inside the group composite. */
    private int checkBox(final String name, float x, String radioGroup) {
        int entity = scene.entity(group, x, 0, 100, 100);

        WidgetComponent widget = scene.engine.edit(entity).create(WidgetComponent.class);
        WidgetTypes.get(WidgetTypes.CHECKBOX).setup(widget);
        widget.properties.put(WidgetTypes.PROPERTY_GROUP, radioGroup);

        scene.engine.edit(entity).create(ButtonComponent.class).addListener(new ButtonComponent.ButtonListener() {
            @Override public void touchUp(int e) { }
            @Override public void touchDown(int e) { }
            @Override public void clicked(int e) { }
            @Override public void checkedChanged(int e, boolean checked) { changes.add(name + "=" + checked); }
        });
        scene.frame();
        return entity;
    }

    /** A click on the box at x, the pointer staying over it afterwards. */
    private void click(int x) {
        scene.click(x, 50);
        scene.frame();
    }

    /** The pointer away from every box. */
    private void away() {
        scene.move(500, 50);
        scene.frame();
    }

    private boolean checked(int entity) {
        return scene.engine.getMapper(ButtonComponent.class).get(entity).isChecked;
    }

    private String state(int entity) {
        return scene.engine.getMapper(WidgetComponent.class).get(entity).getState();
    }

    @Test
    public void aClickChecksAndAnotherUnchecks() {
        int box = checkBox("a", 0, "");

        click(50);
        assertTrue(checked(box));
        assertEquals("checkedHover", state(box));

        away();
        assertEquals("checked", state(box));

        click(50);
        assertFalse(checked(box));
        assertEquals("[a=true, a=false]", changes.toString());
    }

    @Test
    public void aCheckedBoxStillShowsItsPress() {
        int box = checkBox("a", 0, "");
        click(50);

        scene.press(50, 50);
        scene.frame();
        assertEquals("checkedPressed", state(box));
    }

    @Test
    public void aRadioGroupKeepsOneChecked() {
        int first = checkBox("a", 0, "size");
        int second = checkBox("b", 200, "size");

        click(50);
        assertTrue(checked(first));

        click(250);
        assertTrue(checked(second));
        assertFalse(checked(first));

        // clicking the checked one leaves it checked
        click(250);
        assertTrue(checked(second));
        assertEquals("[a=true, a=false, b=true]", changes.toString());
    }

    @Test
    public void boxesOfAnotherGroupAreLeftAlone() {
        int first = checkBox("a", 0, "size");
        int other = checkBox("b", 200, "colour");

        click(50);
        click(250);
        assertTrue(checked(first));
        assertTrue(checked(other));
    }

    @Test
    public void codeActingRightAfterTheLoadIsNotUndone() {
        int box = checkBox("a", 0, "");
        WidgetComponent widget = scene.engine.getMapper(WidgetComponent.class).get(box);
        ButtonComponent button = scene.engine.getMapper(ButtonComponent.class).get(box);

        // as the scene loads, then game code checking it before the first frame
        widget.properties.put(WidgetTypes.PROPERTY_CHECKED, "false");
        button.initialise(widget);
        button.isChecked = true;

        away();
        assertTrue(checked(box));
    }

    @Test
    public void theCheckedSettingIsWhereItStarts() {
        int box = checkBox("a", 0, "");
        scene.engine.getMapper(WidgetComponent.class).get(box).properties.put(WidgetTypes.PROPERTY_CHECKED, "true");
        away();
        assertTrue(checked(box));

        // a click afterwards is not undone by the setting that has not changed
        click(50);
        assertFalse(checked(box));
    }
}
