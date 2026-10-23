package games.rednblack.editor.renderer.widget;

import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.widget.ScrollPaneComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.input.InputTestScene;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.systems.ScrollPaneSystem;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A 200x100 pane holding 200x300 of content, so it can travel 200 downwards, with a 10x100 bar down
 * its right side and a knob on it.
 */
public class ScrollPaneTest {

    private InputTestScene scene;
    private int pane, content, barY, knobY;
    private final List<String> events = new ArrayList<>();

    @Before
    public void setUp() {
        scene = new InputTestScene(new ScrollPaneSystem());

        pane = scene.entity(scene.root, 0, 0, 200, 100);
        scene.engine.edit(pane).create(CompositeTransformComponent.class);
        WidgetTypes.get(WidgetTypes.SCROLL_PANE).setup(scene.engine.edit(pane).create(WidgetComponent.class));
        scene.engine.edit(pane).create(ScrollPaneComponent.class)
                .addListener((entity, x, y) -> events.add("scrolled " + Math.round(y)));

        content = part(WidgetTypes.ROLE_CONTENT, 0, 0, 200, 300);
        barY = part(WidgetTypes.ROLE_SCROLL_BAR_Y, 190, 0, 10, 100);
        knobY = part(WidgetTypes.ROLE_KNOB_Y, 190, 0, 10, 20);

        // most tests want the pane where it is heading, not on its way there
        setting(WidgetTypes.PROPERTY_SMOOTH_SCROLLING, "false");
        frame();
    }

    private int part(String role, float x, float y, float width, float height) {
        int entity = scene.entity(pane, x, y, width, height);
        TransformComponent transform = scene.engine.getMapper(TransformComponent.class).get(entity);
        transform.originX = 0;
        transform.originY = 0;
        scene.engine.edit(entity).create(WidgetPartComponent.class).role = role;
        scene.engine.edit(entity).create(TintComponent.class);
        scene.frame();
        return entity;
    }

    private void setting(String key, String value) {
        scene.engine.getMapper(WidgetComponent.class).get(pane).properties.put(key, value);
    }

    private void frame() {
        frame(0);
    }

    private void frame(float delta) {
        scene.engine.setDelta(delta);
        scene.frame();
    }

    private ScrollPaneComponent state() {
        return scene.engine.getMapper(ScrollPaneComponent.class).get(pane);
    }

    private TransformComponent transform(int entity) {
        return scene.engine.getMapper(TransformComponent.class).get(entity);
    }

    private void wheel(float amountY) {
        scene.move(100, 50);
        scene.input.scrolled(0, amountY);
    }

    @Test
    public void thePaneOwnsItsComposite() {
        CompositeTransformComponent composite = scene.engine.getMapper(CompositeTransformComponent.class).get(pane);
        assertTrue(composite.scissorsEnabled);
        assertFalse(composite.automaticResize);
    }

    @Test
    public void theContentHangsFromTheTopOfTheWindow() {
        assertEquals(200, state().maxY, 0.001f);
        assertEquals(0, state().maxX, 0.001f);
        assertEquals(100 - 300, transform(content).y, 0.001f);
    }

    @Test
    public void theWheelScrollsAndStopsAtTheEnd() {
        wheel(1);
        frame();
        assertEquals(40, state().scrollY, 0.001f);
        assertEquals(100 - 300 + 40, transform(content).y, 0.001f);

        for (int i = 0; i < 10; i++) {
            wheel(1);
            frame();
        }
        assertEquals(200, state().scrollY, 0.001f);

        for (int i = 0; i < 20; i++) {
            wheel(-1);
            frame();
        }
        assertEquals(0, state().scrollY, 0.001f);
    }

    @Test
    public void theKnobIsAsLongAsTheShareInViewAndSaysWhereWeAre() {
        // a third of the content is in view, so a third of the bar
        assertEquals(100f / 3, scene.engine.getMapper(DimensionsComponent.class).get(knobY).height, 0.01f);
        assertEquals(100 - 100f / 3, transform(knobY).y, 0.01f);

        wheel(5);
        frame();
        assertEquals(200, state().scrollY, 0.001f);
        assertEquals(0, transform(knobY).y, 0.01f);
    }

    @Test
    public void theKnobIsAHandleThatDragsThePane() {
        assertTrue(scene.inputOf(knobY).dragHandle);

        // grab the knob at its top and pull it all the way down
        scene.press(195, 99);
        scene.drag(195, 0);
        frame();
        assertEquals(200, state().scrollY, 0.001f);

        scene.drag(195, 99);
        frame();
        assertEquals(0, state().scrollY, 0.001f);
        scene.release(195, 99);
    }

    @Test
    public void theBackgroundFillsThePaneAndStaysBehind() {
        int background = part(WidgetTypes.ROLE_BACKGROUND, -110, -129, 300, 300);
        scene.engine.getMapper(ZIndexComponent.class).get(background).setZIndex(3);
        frame();

        assertEquals(0, transform(background).x, 0.001f);
        assertEquals(0, transform(background).y, 0.001f);
        assertEquals(200, scene.engine.getMapper(DimensionsComponent.class).get(background).width, 0.001f);
        assertEquals(100, scene.engine.getMapper(DimensionsComponent.class).get(background).height, 0.001f);
        assertEquals(0, scene.engine.getMapper(ZIndexComponent.class).get(background).getZIndex());
    }

    @Test
    public void theBarIsLaidOutAlongTheEdgeWhereverItWasDrawn() {
        // dropped in the middle of the pane, as anybody would the first time
        transform(barY).x = 50;
        transform(barY).y = 20;
        transform(knobY).x = 50;
        frame();

        assertEquals(200 - 10, transform(barY).x, 0.001f);
        assertEquals(0, transform(barY).y, 0.001f);
        assertEquals(100, scene.engine.getMapper(DimensionsComponent.class).get(barY).height, 0.001f);
        assertEquals(200 - 10, transform(knobY).x, 0.001f);
    }

    @Test
    public void thetwoBarsMakeRoomForEachOtherAndForTheCorner() {
        scene.engine.getMapper(DimensionsComponent.class).get(content).width = 600;
        int barX = part(WidgetTypes.ROLE_SCROLL_BAR_X, 0, 0, 40, 8);
        int knobX = part(WidgetTypes.ROLE_KNOB_X, 0, 0, 20, 8);
        int corner = part(WidgetTypes.ROLE_CORNER, 0, 0, 4, 4);
        frame();

        // the vertical bar stands on the horizontal one, which stops short of it
        assertEquals(8, transform(barY).y, 0.001f);
        assertEquals(100 - 8, scene.engine.getMapper(DimensionsComponent.class).get(barY).height, 0.001f);
        assertEquals(0, transform(barX).x, 0.001f);
        assertEquals(0, transform(barX).y, 0.001f);
        assertEquals(200 - 10, scene.engine.getMapper(DimensionsComponent.class).get(barX).width, 0.001f);

        // and the corner fills what they leave
        assertEquals(200 - 10, transform(corner).x, 0.001f);
        assertEquals(0, transform(corner).y, 0.001f);
        assertEquals(10, scene.engine.getMapper(DimensionsComponent.class).get(corner).width, 0.001f);
        assertEquals(8, scene.engine.getMapper(DimensionsComponent.class).get(corner).height, 0.001f);
        assertTrue(scene.engine.getMapper(MainItemComponent.class).get(knobX).visible);
    }

    @Test
    public void theBarsCanRunAlongTheOtherEdges() {
        setting(WidgetTypes.PROPERTY_BARS_ON_RIGHT, "false");
        frame();

        assertEquals(0, transform(barY).x, 0.001f);
        assertEquals(0, transform(knobY).x, 0.001f);
    }

    @Test
    public void aKnobWithoutABarTravelsTheEdgeOfThePane() {
        // a pane given a knob and no bar to put it on: the edge of the pane is the bar
        scene.engine.getMapper(WidgetPartComponent.class).get(barY).role = "";
        frame();

        assertEquals(100f / 3, scene.engine.getMapper(DimensionsComponent.class).get(knobY).height, 0.01f);
        assertEquals(100 - 100f / 3, transform(knobY).y, 0.01f);

        wheel(5);
        frame();
        assertEquals(0, transform(knobY).y, 0.01f);

        // the knob still drags the pane, but there is no strip beside it to page in
        scene.press(195, 10);
        scene.drag(195, 99);
        frame();
        assertEquals(0, state().scrollY, 0.001f);
        scene.release(195, 99);
    }

    @Test
    public void pressingTheBarBesideTheKnobMovesByAWindowful() {
        scene.press(195, 10);
        scene.release(195, 10);
        frame();
        assertEquals(100, state().scrollY, 0.001f);
    }

    @Test
    public void aPressBecomesAScrollOnceItHasTravelledFarEnough() {
        int button = scene.target(content, 10, 250, 100, 40);
        scene.inputOf(button).addListener(new UIInputListener() {
            @Override
            public void touchDown(UIEvent event) {
                events.add("button:down");
                event.handle();
            }

            @Override
            public void touchUp(UIEvent event) {
                events.add(event.cancelled ? "button:cancelled" : "button:up");
            }
        });
        frame();

        scene.press(50, 60);
        assertEquals("[button:down]", events.toString());

        // a wobble is still a press
        events.clear();
        scene.drag(52, 60);
        frame();
        assertEquals(0, state().scrollY, 0.001f);
        assertEquals("[]", events.toString());

        // further than the tap square and the pane takes over, the button is let go of.
        // the finger goes up, so the content goes up with it and the pane looks further down
        scene.drag(52, 80);
        frame();
        assertEquals(20, state().scrollY, 0.001f);
        assertEquals("[button:cancelled, scrolled 20]", events.toString());

        scene.release(52, 80);
    }

    @Test
    public void lettingGoAtSpeedFlingsOn() {
        scene.press(50, 20);
        scene.engine.setDelta(0.016f);
        scene.drag(50, 50);
        scene.drag(50, 80);
        scene.release(50, 80);

        assertTrue(state().flingTimer > 0);
        float before = state().scrollY;
        frame(0.016f);
        assertTrue("the fling carries on", state().scrollY > before);
    }

    @Test
    public void anOverscrollSpringsBack() {
        // pulling the content down from the top takes the pane past the edge
        scene.engine.setDelta(0.016f);
        scene.press(50, 80);
        scene.drag(50, 60);
        scene.drag(50, 40);
        frame(0.016f);
        assertTrue("held past the edge", state().scrollY < 0);

        scene.release(50, 40);
        for (int i = 0; i < 60; i++) frame(0.016f);
        assertEquals(0, state().scrollY, 0.001f);
    }

    @Test
    public void theBarsFadeOnceThePaneIsStill() {
        wheel(1);
        frame();
        assertEquals(1, scene.engine.getMapper(TintComponent.class).get(knobY).color.a, 0.001f);

        for (int i = 0; i < 60; i++) frame(0.05f);
        assertEquals(0, scene.engine.getMapper(TintComponent.class).get(knobY).color.a, 0.001f);

        wheel(1);
        frame();
        assertEquals(1, scene.engine.getMapper(TintComponent.class).get(knobY).color.a, 0.001f);
    }

    @Test
    public void whatFitsIsNotScrollableAndShowsNoBars() {
        scene.engine.getMapper(DimensionsComponent.class).get(content).height = 80;
        frame();

        assertEquals(0, state().maxY, 0.001f);
        assertFalse(scene.engine.getMapper(MainItemComponent.class).get(barY).visible);
        // and what fits still hangs from the top of the window
        assertEquals(100 - 80, transform(content).y, 0.001f);
    }

    @Test
    public void aForcedBarIsShownWithNothingToScroll() {
        scene.engine.getMapper(DimensionsComponent.class).get(content).height = 80;
        setting(WidgetTypes.PROPERTY_FORCE_SCROLL_Y, "true");
        frame();

        assertTrue(scene.engine.getMapper(MainItemComponent.class).get(barY).visible);
    }

    @Test
    public void scrollToBringsAPieceOfTheContentIntoView() {
        state().scrollTo(0, 250, 200, 50);
        frame();
        assertEquals(200, state().scrollY, 0.001f);

        state().scrollTo(0, 0, 200, 50);
        frame();
        assertEquals(0, state().scrollY, 0.001f);
    }

    @Test
    public void whatIsDrawnCatchesUpWhenTheScrollingIsSmooth() {
        setting(WidgetTypes.PROPERTY_SMOOTH_SCROLLING, "true");
        wheel(1);
        frame(0.016f);

        assertEquals(40, state().scrollY, 0.001f);
        assertTrue("still on its way", state().visualScrollY > 0 && state().visualScrollY < 40);

        for (int i = 0; i < 30; i++) frame(0.016f);
        assertEquals(40, state().visualScrollY, 0.001f);
    }

    @Test
    public void aPressStopsThePaneWhereItIsDrawn() {
        setting(WidgetTypes.PROPERTY_SMOOTH_SCROLLING, "true");
        wheel(1);
        frame(0.016f);

        float drawn = state().visualScrollY;
        assertTrue("still gliding", drawn > 0 && drawn < 40);

        scene.press(50, 50);
        frame(0.016f);
        assertEquals(drawn, state().scrollY, 0.001f);
        assertEquals(drawn, state().visualScrollY, 0.001f);
        scene.release(50, 50);
    }

    @Test
    public void aPaneHeldPastItsEdgeStaysThere() {
        scene.engine.setDelta(0.016f);
        scene.press(50, 80);
        scene.drag(50, 60);
        scene.drag(50, 40);
        frame(0.016f);

        float held = state().scrollY;
        assertTrue("held past the edge", held < 0);

        for (int i = 0; i < 10; i++) frame(0.016f);
        assertEquals("a held pane does not crawl back", held, state().scrollY, 0.001f);

        scene.release(50, 40);
        for (int i = 0; i < 60; i++) frame(0.016f);
        assertEquals(0, state().scrollY, 0.001f);
    }

    @Test
    public void aWayThePaneDoesNotScrollHasNoOverscroll() {
        // wider content, so X would scroll if it were not disabled
        scene.engine.getMapper(DimensionsComponent.class).get(content).width = 400;
        setting(WidgetTypes.PROPERTY_SCROLL_DISABLED_X, "true");
        frame();

        // a drag right and down pulls past the left and the top edges
        scene.engine.setDelta(0.016f);
        scene.press(50, 80);
        scene.drag(70, 60);
        scene.drag(90, 40);
        frame(0.016f);
        assertEquals("disabled X does not move", 0, state().scrollX, 0.001f);
        assertTrue("Y still overscrolls", state().scrollY < 0);
        scene.release(90, 40);
    }

    @Test
    public void whatFitsHasNoOverscrollEither() {
        // the content is as wide as the window: nothing to scroll sideways
        scene.engine.setDelta(0.016f);
        scene.press(50, 80);
        scene.drag(70, 60);
        scene.drag(90, 40);
        frame(0.016f);
        assertEquals(0, state().scrollX, 0.001f);
        scene.release(90, 40);
    }

    @Test
    public void theStateFollowsTheDrag() {
        assertEquals("normal", scene.engine.getMapper(WidgetComponent.class).get(pane).getState());

        scene.press(50, 80);
        scene.drag(50, 40);
        frame();
        assertEquals("dragged", scene.engine.getMapper(WidgetComponent.class).get(pane).getState());

        scene.release(50, 40);
        frame();
        assertEquals("normal", scene.engine.getMapper(WidgetComponent.class).get(pane).getState());
    }
}
