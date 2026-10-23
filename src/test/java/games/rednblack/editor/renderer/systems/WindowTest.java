package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.components.widget.WindowComponent;
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
import static org.junit.Assert.assertTrue;

/**
 * A window at (100, 100), 300 by 200: its title bar is the top 30, with the close part in the corner
 * from (370, 270) to (400, 300). The backdrop is drawn the window's size until a camera says otherwise.
 */
public class WindowTest {

    @All({WindowComponent.class, WidgetComponent.class})
    private static class TestableWindowSystem extends WindowSystem {
        boolean live = true;

        @Override
        protected boolean interactive(int entity) {
            return live;
        }
    }

    private InputTestScene scene;
    private TestableWindowSystem system;
    private int windowEntity, titleBar, title, close, backdrop;
    private WindowComponent window;
    private WidgetComponent widget;
    private final List<String> events = new ArrayList<>();

    @Before
    public void setUp() {
        system = new TestableWindowSystem();
        scene = new InputTestScene(system, new ButtonSystem(), new WidgetStateSystem());
        events.clear();
        windowEntity = window(scene.root, 100, 100);
        widget = scene.engine.getMapper(WidgetComponent.class).get(windowEntity);
        window = scene.engine.getMapper(WindowComponent.class).get(windowEntity);
        window.addListener(new WindowComponent.WindowListener() {
            @Override public void opened(int entity) { events.add("opened"); }
            @Override public void closed(int entity) { events.add("closed"); }
            @Override public void moved(int entity, float x, float y) { events.add("moved " + (int) x + "," + (int) y); }
        });
        scene.frame();
    }

    /** A window with its parts, made where asked. */
    private int window(int parent, float x, float y) {
        int entity = scene.entity(parent, x, y, 300, 200);
        WidgetComponent widget = scene.engine.edit(entity).create(WidgetComponent.class);
        WidgetTypes.get(WidgetTypes.WINDOW).setup(widget);

        backdrop = part(scene.entity(entity, 0, 0, 300, 200), WidgetTypes.ROLE_BACKDROP);
        titleBar = part(scene.entity(entity, 0, 170, 300, 30), WidgetTypes.ROLE_TITLE_BAR);
        title = part(scene.entity(entity, 8, 170, 250, 30), WidgetTypes.ROLE_TITLE);
        scene.engine.edit(title).create(LabelComponent.class);
        close = part(scene.entity(entity, 270, 170, 30, 30), WidgetTypes.ROLE_CLOSE);

        scene.engine.edit(entity).create(WindowComponent.class);
        scene.frame();
        return entity;
    }

    private int part(int entity, String role) {
        scene.engine.edit(entity).create(WidgetPartComponent.class).role = role;
        return entity;
    }

    private void click(float x, float y) {
        scene.press(x, y);
        scene.release(x, y);
        settle();
    }

    /** Two frames: the one where the state system gives the parts their look, and the one after it. */
    private void settle() {
        scene.frame();
        scene.frame();
    }

    private boolean visible(int entity) {
        return scene.engine.getMapper(MainItemComponent.class).get(entity).visible;
    }

    private TransformComponent transform(int entity) {
        return scene.engine.getMapper(TransformComponent.class).get(entity);
    }

    /** A camera over the whole test screen, set by hand: one that updates itself needs natives. */
    private void camera() {
        OrthographicCamera camera = new OrthographicCamera();
        camera.viewportWidth = InputTestScene.WIDTH;
        camera.viewportHeight = InputTestScene.HEIGHT;
        camera.position.set(InputTestScene.WIDTH / 2f, InputTestScene.HEIGHT / 2f, 0);
        scene.engine.getMapper(ViewPortComponent.class).get(scene.root).viewPort.setCamera(camera);
        scene.frame();
    }

    // ------------------------------------------------------------------ opening and closing

    @Test
    public void theTitleLabelShowsTheTitleSetting() {
        widget.properties.put(WidgetTypes.PROPERTY_TITLE, "Options");
        scene.frame();

        assertEquals("Options", scene.engine.getMapper(LabelComponent.class).get(title).getText().toString());
    }

    @Test
    public void aClickOnTheClosePartClosesIt() {
        click(385, 285);

        assertFalse(window.isOpen());
        assertFalse(visible(windowEntity));
        assertEquals("[closed]", events.toString());
    }

    @Test
    public void aCloseButtonClosesItWithItsClick() {
        scene.engine.edit(close).create(ButtonComponent.class);
        scene.frame();

        click(385, 285);

        assertFalse(window.isOpen());
    }

    @Test
    public void theOpenSettingIsWhereItStartsAndGameCodeOpensIt() {
        widget.properties.put(WidgetTypes.PROPERTY_OPEN, "false");
        settle();
        assertFalse(visible(windowEntity));

        window.open();
        scene.frame();

        assertTrue(visible(windowEntity));
        assertEquals("[closed, opened]", events.toString());
    }

    @Test
    public void escapeClosesItOnlyWhenTheSettingSaysSo() {
        scene.input.setKeyboardFocus(windowEntity);

        scene.input.keyDown(Input.Keys.ESCAPE);
        scene.frame();
        assertTrue(window.isOpen());

        widget.properties.put(WidgetTypes.PROPERTY_CLOSE_ON_ESCAPE, "true");
        scene.input.keyDown(Input.Keys.ESCAPE);
        scene.frame();
        assertFalse(window.isOpen());
    }

    @Test
    public void closingLetsGoOfTheKeyboardHeldInside() {
        scene.input.setKeyboardFocus(title);

        window.close();
        scene.frame();

        assertEquals(-1, scene.input.getKeyboardFocus());
    }

    // ------------------------------------------------------------------ moving

    @Test
    public void theTitleBarDragsIt() {
        scene.press(200, 285);
        scene.frame();
        assertEquals(WidgetTypes.STATE_DRAGGED, widget.getState());

        scene.drag(230, 265);
        scene.release(230, 265);
        scene.frame();

        assertEquals(130, transform(windowEntity).x, 0.001f);
        assertEquals(80, transform(windowEntity).y, 0.001f);
        assertEquals(WidgetTypes.STATE_NORMAL, widget.getState());
        assertEquals("[moved 130,80]", events.toString());
    }

    @Test
    public void aWindowThatIsNotMovableStaysPut() {
        widget.properties.put(WidgetTypes.PROPERTY_MOVABLE, "false");

        scene.press(200, 285);
        scene.drag(230, 265);
        scene.frame();

        assertEquals(100, transform(windowEntity).x, 0.001f);
    }

    /** The window is a panel: a press on its body stays there, it does not move it. */
    @Test
    public void aPressOnTheBodyIsTakenButDoesNotDrag() {
        assertTrue(scene.press(200, 150));
        scene.drag(250, 150);
        scene.frame();

        assertEquals(100, transform(windowEntity).x, 0.001f);
    }

    @Test
    public void itIsKeptWithinTheViewport() {
        camera();

        scene.press(200, 285);
        scene.drag(700, 285);
        scene.release(700, 285);
        scene.frame();

        //800 wide screen, 300 wide window
        assertEquals(500, transform(windowEntity).x, 0.001f);
    }

    /** Drawn in place, without a backdrop: in front of its siblings on its layer, the way scene2d does it. */
    @Test
    public void aPressBringsItInFrontOfItsSiblings() {
        int firstBackdrop = backdrop;
        int other = window(scene.root, 450, 350);
        scene.engine.getMapper(WidgetPartComponent.class).get(firstBackdrop).role = "";
        scene.engine.getMapper(WidgetPartComponent.class).get(backdrop).role = "";
        scene.frame();
        scene.engine.getMapper(ZIndexComponent.class).get(windowEntity).setZIndex(0);
        scene.engine.getMapper(ZIndexComponent.class).get(other).setZIndex(1);

        scene.press(200, 150);

        assertEquals(2, scene.engine.getMapper(ZIndexComponent.class).get(windowEntity).getZIndex());
    }

    // ------------------------------------------------------------------ modal and backdrop

    @Test
    public void aModalWindowIsAModalOverlayWhileOpen() {
        widget.properties.put(WidgetTypes.PROPERTY_MODAL, "true");
        scene.frame();

        assertTrue(scene.input.isOverlayOpen(windowEntity));
        assertEquals(windowEntity, scene.input.topModal());
        //behind it nothing is reached, and the press is swallowed
        assertTrue(scene.press(600, 500));

        window.close();
        settle();
        assertFalse(scene.input.isOverlayOpen(windowEntity));
    }

    /** The whole gesture is the modal's, so an input processor after this one never sees half of it. */
    @Test
    public void aModalWindowKeepsTheWholeGesture() {
        widget.properties.put(WidgetTypes.PROPERTY_MODAL, "true");
        scene.frame();

        //behind it
        assertTrue(scene.press(600, 500));
        assertTrue(scene.drag(650, 520));
        assertTrue(scene.release(650, 520));

        //on a part of it nobody listens to
        assertTrue(scene.press(200, 150));
        assertTrue(scene.drag(250, 160));
        assertTrue(scene.release(250, 160));

        //once it is closed, the input behind it is free again
        window.close();
        settle();
        assertFalse(scene.press(600, 500));
        assertFalse(scene.drag(650, 520));
        assertFalse(scene.release(650, 520));
    }

    /** Drawn over everything so its backdrop covers it all; what it does not cover stays reachable. */
    @Test
    public void aWindowWithABackdropIsAnOverlayEvenWhenNotModal() {
        int button = scene.target(scene.root, 600, 450, 100, 50);
        scene.inputOf(button).addListener(new games.rednblack.editor.renderer.input.UIInputListener() {
            @Override public void touchDown(games.rednblack.editor.renderer.input.UIEvent event) {
                events.add("button");
                event.handle();
            }
        });

        assertTrue(scene.input.isOverlayOpen(windowEntity));
        assertEquals(-1, scene.input.topModal());

        click(650, 475);
        assertEquals("[button]", events.toString());

        //in front of the others without becoming modal
        click(200, 150);
        assertEquals(-1, scene.input.topModal());

        window.close();
        settle();
        assertFalse(scene.input.isOverlayOpen(windowEntity));
    }

    /** Two windows over everything: a press brings the one behind in front of the other. */
    @Test
    public void aPressBringsAnOverlayWindowInFrontOfTheOthers() {
        //(100, 100) to (400, 300) and (250, 150) to (550, 350): they overlap around (300, 200)
        int other = window(scene.root, 250, 150);
        assertEquals(other, scene.hitAt(300, 200));

        scene.press(150, 150);
        scene.release(150, 150);
        scene.frame();

        assertEquals(windowEntity, scene.hitAt(300, 200));
        assertEquals(-1, scene.input.topModal());
    }

    @Test
    public void withoutABackdropOrModalItIsDrawnInPlace() {
        scene.engine.getMapper(WidgetPartComponent.class).get(backdrop).role = "";
        scene.frame();

        assertFalse(scene.input.isOverlayOpen(windowEntity));
    }

    @Test
    public void turningModalOnWhileOpenIsFollowed() {
        widget.properties.put(WidgetTypes.PROPERTY_MODAL, "true");
        scene.frame();

        assertEquals(windowEntity, scene.input.topModal());
    }

    @Test
    public void theBackdropCoversWhatTheCameraSees() {
        camera();

        DimensionsComponent dimensions = scene.engine.getMapper(DimensionsComponent.class).get(backdrop);
        assertEquals(-100, transform(backdrop).x, 0.001f);
        assertEquals(-100, transform(backdrop).y, 0.001f);
        assertEquals(800, dimensions.width, 0.001f);
        assertEquals(600, dimensions.height, 0.001f);
        assertTrue(visible(backdrop));
    }

    @Test
    public void aTapOnTheBackdropClosesItOnlyWhenTheSettingSaysSo() {
        camera();

        click(600, 500);
        assertTrue(window.isOpen());

        widget.properties.put(WidgetTypes.PROPERTY_CLOSE_ON_BACKDROP_TAP, "true");
        scene.frame();

        //on the window itself it is no tap on the backdrop
        click(200, 150);
        assertTrue(window.isOpen());

        click(600, 500);
        assertFalse(window.isOpen());
    }

    // ------------------------------------------------------------------ the closed look

    /** The backdrop fading to nothing over a second when the window closes, and back when it opens. */
    private void fadeBackdropWhenClosed() {
        scene.engine.edit(backdrop).create(TintComponent.class);
        WidgetPartComponent part = scene.engine.getMapper(WidgetPartComponent.class).get(backdrop);
        ObjectMap<String, String> values = new ObjectMap<>();
        values.put(CoreStateOverrides.TINT, "00000000");
        part.overrides.put(WidgetTypes.STATE_CLOSED, values);
        WidgetPartComponent.Transition fade = new WidgetPartComponent.Transition();
        fade.duration = 1;
        fade.interpolation = "linear";
        ObjectMap<String, WidgetPartComponent.Transition> transitions = new ObjectMap<>();
        transitions.put(CoreStateOverrides.TINT, fade);
        part.transitions.put(WidgetTypes.STATE_CLOSED, transitions);
        part.dirty = true;
        scene.frame();
    }

    private void tick(float seconds) {
        scene.engine.setDelta(seconds);
        scene.frame();
        scene.engine.setDelta(0);
    }

    @Test
    public void aClosingWindowShowsUntilItsClosedLookHasPlayed() {
        camera();
        fadeBackdropWhenClosed();

        window.close();
        scene.frame();
        tick(0.5f);
        assertEquals(WidgetTypes.STATE_CLOSED, widget.getState());
        assertTrue(visible(windowEntity));
        assertTrue(visible(backdrop));

        tick(0.6f);
        scene.frame();
        assertFalse(visible(windowEntity));
        assertEquals("[closed]", events.toString());
    }

    /** It is a picture while it closes: presses go to what is behind it and it blocks nothing. */
    @Test
    public void aClosingWindowTakesNoInput() {
        camera();
        fadeBackdropWhenClosed();
        widget.properties.put(WidgetTypes.PROPERTY_MODAL, "true");
        scene.frame();

        window.close();
        scene.frame();
        tick(0.1f);

        assertTrue(visible(windowEntity));
        assertEquals(-1, scene.input.topModal());
        assertEquals(-1, scene.hitAt(200, 150));
    }

    @Test
    public void aWindowThatStartsClosedIsHiddenAtOnce() {
        int other = window(scene.root, 450, 350);
        WidgetComponent otherWidget = scene.engine.getMapper(WidgetComponent.class).get(other);
        WindowComponent otherWindow = scene.engine.getMapper(WindowComponent.class).get(other);
        otherWidget.properties.put(WidgetTypes.PROPERTY_OPEN, "false");
        //set up again from its settings, the way a scene loads it
        otherWindow.initialise(otherWidget);
        otherWindow.notified = false;

        scene.frame();

        assertFalse(visible(other));
    }

    // ------------------------------------------------------------------ outside of a game

    @Test
    public void whileBeingDesignedItShowsStaysAndHidesItsBackdrop() {
        camera();
        system.live = false;
        widget.properties.put(WidgetTypes.PROPERTY_OPEN, "false");
        scene.frame();

        assertTrue(visible(windowEntity));
        assertFalse(visible(backdrop));

        click(385, 285);
        scene.press(200, 285);
        scene.drag(230, 265);
        scene.frame();
        assertEquals(100, transform(windowEntity).x, 0.001f);
    }

    // ------------------------------------------------------------------ made by the editor

    /**
     * The image a window is made from is its background; the editor adds a backdrop and a title bar,
     * a bare composite holding the title and the close button, each held in place by constraints.
     */
    @Test
    public void aNewWindowIsGivenItsPartsPinnedWhereTheyBelong() {
        WidgetType type = WidgetTypes.get(WidgetTypes.WINDOW);
        assertEquals(WidgetTypes.ROLE_BACKGROUND, type.convertedItemRole);

        WidgetType.DefaultPart titleBar = null, backdrop = null;
        for (WidgetType.DefaultPart part : type.defaultParts) {
            assertFalse(WidgetTypes.ROLE_BACKGROUND.equals(part.role));
            if (WidgetTypes.ROLE_TITLE_BAR.equals(part.role)) titleBar = part;
            if (WidgetTypes.ROLE_BACKDROP.equals(part.role)) backdrop = part;
        }

        assertEquals(WidgetType.DefaultPart.Kind.COMPOSITE, titleBar.kind);
        assertTrue(titleBar.matchWidth && titleBar.pinTop);
        assertEquals(0, backdrop.zIndex);

        WidgetType.DefaultPart title = titleBar.children.get(0), close = titleBar.children.get(1);
        assertEquals(WidgetTypes.ROLE_TITLE, title.role);
        assertTrue(title.matchWidth && title.matchHeight);
        assertEquals(WidgetTypes.ROLE_CLOSE, close.role);
        assertTrue(close.pinTop && close.pinRight);
        assertEquals(WidgetTypes.BUTTON, close.widgetType);
    }

    /** The title and the close button may sit in the title bar rather than straight in the window. */
    @Test
    public void partsInsideTheTitleBarAreFoundThere() {
        int bar = part(scene.entity(windowEntity, 0, 170, 300, 30), "");
        scene.engine.getMapper(WidgetPartComponent.class).get(titleBar).role = "";
        scene.engine.getMapper(WidgetPartComponent.class).get(bar).role = WidgetTypes.ROLE_TITLE_BAR;
        int nestedTitle = part(scene.entity(bar, 8, 0, 250, 30), WidgetTypes.ROLE_TITLE);
        scene.engine.edit(nestedTitle).create(LabelComponent.class);
        scene.engine.getMapper(WidgetPartComponent.class).get(title).role = "";
        int nestedClose = part(scene.entity(bar, 270, 0, 30, 30), WidgetTypes.ROLE_CLOSE);
        scene.engine.getMapper(WidgetPartComponent.class).get(close).role = "";
        widget.properties.put(WidgetTypes.PROPERTY_TITLE, "Nested");
        scene.frame();

        assertEquals("Nested", scene.engine.getMapper(LabelComponent.class).get(nestedTitle).getText().toString());

        click(385, 285);
        assertFalse(window.isOpen());
    }
}
