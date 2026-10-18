package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.widget.SelectBoxComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.widget.WidgetType;
import games.rednblack.editor.renderer.widget.WidgetTypes;
import games.rednblack.editor.renderer.widget.handlers.CoreStateOverrides;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A select box at (100, 300), 200 by 40, with three items. Its rows are 30 tall, so the list opens
 * 90 tall right under it: the first row from 270 to 300, the second from 240, the third from 210.
 */
public class SelectBoxTest {

    private static final float ROW_HEIGHT = 30;

    /**
     * Makes the rows by hand, the way the scene loader's factory would: tests have none. A row is a
     * composite holding a label, and a button when asked, to see the checked look driven.
     */
    @All({SelectBoxComponent.class, WidgetComponent.class})
    private class TestableSelectBoxSystem extends SelectBoxSystem {
        boolean buildRows = true;
        boolean buttonRows = false;

        @Override
        protected int cloneRow(int template, int parent) {
            int row = create(parent, 0, 0, 200, ROW_HEIGHT);
            if (buttonRows) scene.engine.edit(row).create(ButtonComponent.class);
            int label = create(row, 0, 0, 200, ROW_HEIGHT);
            scene.engine.edit(label).create(LabelComponent.class);
            return row;
        }

        @Override
        protected boolean buildsRows(int entity) {
            return buildRows;
        }
    }

    private InputTestScene scene;
    private TestableSelectBoxSystem system;
    private int box, text, list, template;
    private SelectBoxComponent selectBox;
    private WidgetComponent widget;
    private final List<String> changes = new ArrayList<>();

    @Before
    public void setUp() {
        system = new TestableSelectBoxSystem();
        scene = new InputTestScene(system);
        changes.clear();

        box = scene.entity(scene.root, 100, 300, 200, 40);
        widget = scene.engine.edit(box).create(WidgetComponent.class);
        WidgetTypes.get(WidgetTypes.SELECT_BOX).setup(widget);
        widget.properties.put(WidgetTypes.PROPERTY_ITEMS, "apple\nbanana\ncherry");

        text = part(scene.entity(box, 10, 10, 150, 20), WidgetTypes.ROLE_TEXT);
        scene.engine.edit(text).create(LabelComponent.class);
        list = part(scene.entity(box, 0, -100, 200, 100), WidgetTypes.ROLE_LIST);
        template = part(scene.entity(box, 0, -200, 200, ROW_HEIGHT), WidgetTypes.ROLE_ROW);

        selectBox = scene.engine.edit(box).create(SelectBoxComponent.class);
        selectBox.addListener((entity, index, item) -> changes.add(index + ":" + item));
        scene.frame();
    }

    private int part(int entity, String role) {
        scene.engine.edit(entity).create(WidgetPartComponent.class).role = role;
        return entity;
    }

    /** An entity made while the engine is running, so without the extra frame the scene helper adds. */
    private int create(int parent, float x, float y, float width, float height) {
        int entity = scene.engine.create();
        scene.engine.edit(entity).create(NodeComponent.class);
        scene.engine.edit(entity).create(MainItemComponent.class);
        scene.engine.edit(entity).create(ZIndexComponent.class);
        TransformComponent transform = scene.engine.edit(entity).create(TransformComponent.class);
        transform.x = x;
        transform.y = y;
        DimensionsComponent dimensions = scene.engine.edit(entity).create(DimensionsComponent.class);
        dimensions.width = width;
        dimensions.height = height;
        scene.engine.edit(entity).create(ParentNodeComponent.class).parentEntity = parent;
        scene.engine.getMapper(NodeComponent.class).get(parent).addChild(entity);
        return entity;
    }

    private String labelOf(int entity) {
        return scene.engine.getMapper(LabelComponent.class).get(entity).getText().toString();
    }

    private String rowText(int index) {
        int row = selectBox.rows.get(index);
        int label = scene.engine.getMapper(NodeComponent.class).get(row).children.first();
        return labelOf(label);
    }

    private boolean visible(int entity) {
        return scene.engine.getMapper(MainItemComponent.class).get(entity).visible;
    }

    private void click(float x, float y) {
        scene.press(x, y);
        scene.release(x, y);
        scene.frame();
    }

    private void key(int keycode) {
        scene.input.keyDown(keycode);
        scene.input.keyUp(keycode);
        scene.frame();
    }

    // ------------------------------------------------------------------ items and rows

    @Test
    public void theItemsComeFromTheSettingAndTheFirstIsChosen() {
        assertEquals(3, selectBox.items.size);
        assertEquals(0, selectBox.selected);
        assertEquals("apple", labelOf(text));
    }

    @Test
    public void everyItemGetsARowAndTheRowPartStaysHidden() {
        assertEquals(3, selectBox.rows.size);
        assertEquals("apple", rowText(0));
        assertEquals("banana", rowText(1));
        assertEquals("cherry", rowText(2));
        assertFalse(visible(template));

        //stacked from the top of the list, which is as tall as the rows
        TransformComponent first = scene.engine.getMapper(TransformComponent.class).get(selectBox.rows.get(0));
        TransformComponent last = scene.engine.getMapper(TransformComponent.class).get(selectBox.rows.get(2));
        assertEquals(2 * ROW_HEIGHT, first.y, 0.001f);
        assertEquals(0, last.y, 0.001f);
        assertEquals(3 * ROW_HEIGHT, scene.engine.getMapper(DimensionsComponent.class).get(list).height, 0.001f);
    }

    @Test
    public void newItemsRemakeTheRowsAndKeepTheChoiceInRange() {
        selectBox.setSelected(2);
        widget.properties.put(WidgetTypes.PROPERTY_ITEMS, "one\ntwo");
        scene.frame();

        assertEquals(2, selectBox.rows.size);
        assertEquals("two", rowText(1));
        assertEquals(1, selectBox.selected);
        assertEquals("two", labelOf(text));
    }

    @Test
    public void blankLinesAreNoItems() {
        widget.properties.put(WidgetTypes.PROPERTY_ITEMS, "one\r\n\n  \ntwo\n");
        scene.frame();

        assertEquals("[one, two]", selectBox.items.toString());
    }

    @Test
    public void withoutRowsBeingMadeTheRowPartShows() {
        system.buildRows = false;
        scene.frame();

        assertEquals(0, selectBox.rows.size);
        assertTrue(visible(template));
    }

    // ------------------------------------------------------------------ the pointer

    @Test
    public void aPressOnTheBoxOpensTheListBelowIt() {
        assertFalse(visible(list));

        click(150, 320);

        assertTrue(selectBox.isOpen);
        assertEquals(WidgetTypes.STATE_OPEN, widget.getState());
        assertTrue(visible(list));
        assertTrue(scene.input.isOverlayOpen(list));
        assertEquals(-3 * ROW_HEIGHT, scene.engine.getMapper(TransformComponent.class).get(list).y, 0.001f);
    }

    @Test
    public void aPressOnARowChoosesItAndCloses() {
        click(150, 320);

        click(150, 255);

        assertEquals(1, selectBox.selected);
        assertEquals("banana", labelOf(text));
        assertFalse(selectBox.isOpen);
        assertFalse(visible(list));
        assertFalse(scene.input.isOverlayOpen(list));
        assertEquals("[1:banana]", changes.toString());
    }

    @Test
    public void aPressElsewhereClosesWithoutChoosing() {
        click(150, 320);

        click(600, 500);

        assertFalse(selectBox.isOpen);
        assertEquals(0, selectBox.selected);
        assertEquals("[]", changes.toString());
    }

    /** The press missing the list lands on the box: it closes the list, it does not open it again. */
    @Test
    public void aSecondPressOnTheBoxCloses() {
        click(150, 320);

        click(150, 320);

        assertFalse(selectBox.isOpen);
    }

    @Test
    public void aReleaseAwayFromTheRowChoosesNothing() {
        click(150, 320);

        scene.press(150, 255);
        scene.release(600, 500);
        scene.frame();

        assertEquals(0, selectBox.selected);
    }

    @Test
    public void theListOpensAboveWhenThereIsNoRoomBelow() {
        //set by hand: a camera that updates itself needs the native libraries the tests do without
        OrthographicCamera camera = new OrthographicCamera();
        camera.viewportWidth = InputTestScene.WIDTH;
        camera.viewportHeight = InputTestScene.HEIGHT;
        camera.position.set(InputTestScene.WIDTH / 2f, InputTestScene.HEIGHT / 2f, 0);
        scene.engine.getMapper(ViewPortComponent.class).get(scene.root).viewPort.setCamera(camera);
        scene.engine.getMapper(TransformComponent.class).get(box).y = 50;
        scene.frame();

        click(150, 70);

        assertEquals(40, scene.engine.getMapper(TransformComponent.class).get(list).y, 0.001f);
    }

    @Test
    public void buttonRowsShowTheItemTheListPointsAtAsChecked() {
        system.buttonRows = true;
        widget.properties.put(WidgetTypes.PROPERTY_ITEMS, "a\nb\nc");
        scene.frame();

        ButtonComponent first = scene.engine.getMapper(ButtonComponent.class).get(selectBox.rows.get(0));
        ButtonComponent second = scene.engine.getMapper(ButtonComponent.class).get(selectBox.rows.get(1));
        assertTrue(first.isChecked);

        click(150, 320);
        key(Input.Keys.DOWN);

        assertFalse(first.isChecked);
        assertTrue(second.isChecked);
    }

    // ------------------------------------------------------------------ the keys

    @Test
    public void closedTheArrowsChooseTheItemBeforeOrAfter() {
        scene.input.setKeyboardFocus(box);

        key(Input.Keys.DOWN);
        key(Input.Keys.DOWN);
        key(Input.Keys.DOWN);

        assertEquals(2, selectBox.selected);
        assertFalse(selectBox.isOpen);

        key(Input.Keys.UP);
        assertEquals(1, selectBox.selected);
        assertEquals("[1:banana, 2:cherry, 1:banana]", changes.toString());
    }

    @Test
    public void openTheArrowsMoveThroughTheListAndEnterChooses() {
        scene.input.setKeyboardFocus(box);

        key(Input.Keys.ENTER);
        assertTrue(selectBox.isOpen);

        key(Input.Keys.DOWN);
        key(Input.Keys.DOWN);
        assertEquals(2, selectBox.highlighted);
        assertEquals(0, selectBox.selected);

        key(Input.Keys.ENTER);
        assertFalse(selectBox.isOpen);
        assertEquals(2, selectBox.selected);
    }

    @Test
    public void escapeClosesWithoutChoosing() {
        scene.input.setKeyboardFocus(box);
        key(Input.Keys.SPACE);
        key(Input.Keys.DOWN);

        key(Input.Keys.ESCAPE);

        assertFalse(selectBox.isOpen);
        assertEquals(0, selectBox.selected);
    }

    @Test
    public void aHeldArrowKeepsMoving() {
        widget.properties.put(WidgetTypes.PROPERTY_ITEMS, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");
        scene.frame();
        scene.input.setKeyboardFocus(box);

        scene.input.keyDown(Input.Keys.DOWN);
        scene.frame();
        assertEquals(1, selectBox.selected);

        //the first repeat after 0.4s, then one every 0.05s
        scene.engine.setDelta(0.4f);
        scene.frame();
        scene.engine.setDelta(0.1f);
        scene.frame();
        scene.engine.setDelta(0);

        assertEquals(4, selectBox.selected);

        scene.input.keyUp(Input.Keys.DOWN);
        scene.engine.setDelta(1);
        scene.frame();
        assertEquals(4, selectBox.selected);
    }

    // ------------------------------------------------------------------ settings and state

    @Test
    public void theSelectedSettingIsFollowedWhenItChanges() {
        widget.properties.put(WidgetTypes.PROPERTY_SELECTED, "2");
        scene.frame();

        assertEquals("cherry", selectBox.getSelected());
    }

    @Test
    public void noItemsMeansNothingChosenAndNothingToOpen() {
        widget.properties.put(WidgetTypes.PROPERTY_ITEMS, "");
        scene.frame();

        assertNull(selectBox.getSelected());
        assertEquals("", labelOf(text));

        click(150, 320);
        assertFalse(selectBox.isOpen);
    }

    @Test
    public void aDisabledBoxClosesAndTakesNothing() {
        click(150, 320);

        selectBox.isTouchEnabled = false;
        scene.frame();

        assertFalse(selectBox.isOpen);
        assertEquals(WidgetTypes.STATE_DISABLED, widget.getState());

        click(150, 320);
        assertFalse(selectBox.isOpen);
    }

    // ------------------------------------------------------------------ made by the editor

    /** A new select box works at once: the editor gives it a label, a list and a row that is a button. */
    @Test
    public void aNewSelectBoxIsGivenATextAListAndAButtonRow() {
        WidgetType type = WidgetTypes.get(WidgetTypes.SELECT_BOX);
        WidgetType.DefaultPart text = null, list = null, row = null;
        for (WidgetType.DefaultPart part : type.defaultParts) {
            if (WidgetTypes.ROLE_TEXT.equals(part.role)) text = part;
            if (WidgetTypes.ROLE_LIST.equals(part.role)) list = part;
            if (WidgetTypes.ROLE_ROW.equals(part.role)) row = part;
        }

        assertEquals(WidgetType.DefaultPart.Kind.LABEL, text.kind);
        assertEquals(WidgetType.DefaultPart.Kind.COMPOSITE, list.kind);
        assertEquals(WidgetTypes.BUTTON, row.widgetType);
        assertEquals(200, row.width(200), 0.001f);

        WidgetType.DefaultPart band = row.children.first();
        assertNull(band.role);
        assertTrue(band.overrides.get(WidgetTypes.STATE_HOVER).containsKey(CoreStateOverrides.TINT));
        assertTrue(band.overrides.get(WidgetTypes.STATE_CHECKED).containsKey(CoreStateOverrides.TINT));
        assertEquals(WidgetType.DefaultPart.Kind.LABEL, row.children.get(1).kind);

        //everything follows the width of what holds it, the labels keeping off the edges
        assertTrue(text.matchWidth && text.matchHeight);
        assertEquals(8, text.marginX, 0.001f);
        assertTrue(list.matchWidth && !list.matchHeight);
        assertTrue(row.matchWidth && !row.matchHeight);
        assertTrue(band.matchWidth && band.matchHeight);
        assertTrue(row.children.get(1).matchWidth);
    }
}
