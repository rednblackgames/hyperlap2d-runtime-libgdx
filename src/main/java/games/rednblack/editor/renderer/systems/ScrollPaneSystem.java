package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.widget.ScrollPaneComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * Moves the content of a scroll pane behind the window the pane keeps open on it, and places the
 * scrollbars that say where it is looking.
 *
 * The pane owns its composite: clipping on, automatic resize off, so its rectangle is the window
 * and the content slides behind it. Scroll is counted from the left and from the top of the
 * content, the way it reads, and is kept between zero and what sticks out of the window.
 *
 * Pointers reach it three ways: the wheel, a scrollbar - dragging a knob or clicking beside it for
 * a page - and the content itself, which is dragged and flung. That last one starts by following a
 * press without taking it, so a button inside the pane is pressed as usual and only loses the
 * pointer once the press has travelled far enough to be a scroll.
 */
@All({ScrollPaneComponent.class, WidgetComponent.class})
public class ScrollPaneSystem extends BaseEntitySystem implements UIInputListener {

    /** Below this speed letting go is just letting go, not a fling. */
    private static final float FLING_SPEED = 150;
    /** A finger that has been still this long before letting go does not fling either. */
    private static final float FLING_IDLE = 0.1f;
    /** A knob never shrinks past this, or there would be nothing left to grab. */
    private static final float MIN_KNOB = 8;

    protected ComponentMapper<ScrollPaneComponent> paneCM;
    protected ComponentMapper<WidgetComponent> widgetCM;
    protected ComponentMapper<WidgetPartComponent> partCM;
    protected ComponentMapper<NodeComponent> nodeCM;
    protected ComponentMapper<TransformComponent> transformCM;
    protected ComponentMapper<DimensionsComponent> dimensionsCM;
    protected ComponentMapper<MainItemComponent> mainItemCM;
    protected ComponentMapper<TintComponent> tintCM;
    protected ComponentMapper<CompositeTransformComponent> compositeCM;
    protected ComponentMapper<ViewPortComponent> viewPortCM;
    protected ComponentMapper<ZIndexComponent> zIndexCM;
    protected ComponentMapper<InputTargetComponent> inputTargetCM;

    protected UIInputSystem inputSystem;

    private final Rectangle bar = new Rectangle();
    private final Rectangle knob = new Rectangle();
    private final Rectangle span = new Rectangle();

    /**
     * Follows a press made anywhere in the pane without taking it, so that what is inside is
     * pressed as usual, and takes the pointer over once the press turns into a scroll.
     */
    private final UIInputListener flickWatcher = new UIInputListener() {
        @Override
        public void touchDown(UIEvent event) {
            flickDown(event);
        }

        @Override
        public void touchDragged(UIEvent event) {
            flickDragged(event);
        }

        @Override
        public void touchUp(UIEvent event) {
            flickUp(event);
        }
    };

    public ScrollPaneSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public ScrollPaneSystem() {
    }

    @Override
    protected void inserted(int entityId) {
        InputTargetComponent target = inputTargetCM.create(entityId);
        target.addListener(this);
        target.addCaptureListener(flickWatcher);
    }

    @Override
    protected void removed(int entityId) {
        InputTargetComponent target = inputTargetCM.get(entityId);
        if (target != null) {
            target.removeListener(this);
            target.removeCaptureListener(flickWatcher);
        }
        if (inputSystem != null) inputSystem.cancelTouchFocus(entityId);
    }

    @Override
    protected final void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        for (int i = 0, s = actives.size(); i < s; i++) {
            process(ids[i]);
        }
    }

    protected void process(int entity) {
        ScrollPaneComponent pane = paneCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);

        CompositeTransformComponent composite = compositeCM.get(entity);
        if (composite != null) {
            //the pane keeps its own rectangle: it shows what fits in it and never grows to its content
            composite.scissorsEnabled = true;
            composite.automaticResize = false;
        }

        if (viewPortCM.has(entity) || !pane.isTouchEnabled) releaseSilently(entity, pane);

        measure(entity, pane, widget);
        advance(pane, widget);
        layout(entity, pane, widget);
        notifyScroll(entity, pane);
        updateWidgetState(entity, widget, pane);
    }

    // ------------------------------------------------------------------ the numbers

    /** How big the window is, how big the content is, and how far it can therefore travel. */
    private void measure(int entity, ScrollPaneComponent pane, WidgetComponent widget) {
        DimensionsComponent area = dimensionsCM.get(entity);
        if (area != null) {
            pane.areaWidth = area.width;
            pane.areaHeight = area.height;
        }

        int content = findPart(entity, WidgetTypes.ROLE_CONTENT);
        if (content != -1) {
            TransformComponent transform = transformOf(content);
            DimensionsComponent dimensions = dimensionsCM.get(content);
            if (transform != null && dimensions != null) {
                pane.contentWidth = dimensions.width * transform.scaleX;
                pane.contentHeight = dimensions.height * transform.scaleY;
            }
        }

        pane.maxX = flag(widget, WidgetTypes.PROPERTY_SCROLL_DISABLED_X, false)
                ? 0 : Math.max(0, pane.contentWidth - pane.areaWidth);
        pane.maxY = flag(widget, WidgetTypes.PROPERTY_SCROLL_DISABLED_Y, false)
                ? 0 : Math.max(0, pane.contentHeight - pane.areaHeight);
    }

    /** A frame of movement: the fling carries on, an overscroll springs back, the look catches up. */
    private void advance(ScrollPaneComponent pane, WidgetComponent widget) {
        float delta = engine.getDelta();

        if (pane.isDragging()) pane.dragIdle += delta;

        boolean overscroll = flag(widget, WidgetTypes.PROPERTY_OVERSCROLL, true);
        float distance = overscroll
                ? Math.max(0.0001f, number(widget, WidgetTypes.PROPERTY_OVERSCROLL_DISTANCE, 50)) : 0;

        if (pane.flingTimer > 0) {
            resetFade(pane, widget);
            float flingTime = Math.max(0.0001f, number(widget, WidgetTypes.PROPERTY_FLING_TIME, 1));
            float alpha = pane.flingTimer / flingTime;
            pane.scrollX -= pane.velocityX * alpha * delta;
            pane.scrollY += pane.velocityY * alpha * delta;
            clamp(pane, widget);

            //a fling that has run as far as the content goes stops there
            if (pane.scrollX <= -distance || pane.scrollX >= pane.maxX + distance) pane.velocityX = 0;
            if (pane.scrollY <= -distance || pane.scrollY >= pane.maxY + distance) pane.velocityY = 0;

            pane.flingTimer -= delta;
            if (pane.flingTimer <= 0 || (pane.velocityX == 0 && pane.velocityY == 0)) stopFling(pane);
        }

        if (overscroll && pane.touchPointer == -1) {
            float speedMin = number(widget, WidgetTypes.PROPERTY_OVERSCROLL_SPEED_MIN, 30);
            float speedMax = number(widget, WidgetTypes.PROPERTY_OVERSCROLL_SPEED_MAX, 200);

            pane.scrollX = springBack(pane.scrollX, pane.maxX, distance, speedMin, speedMax, delta);
            pane.scrollY = springBack(pane.scrollY, pane.maxY, distance, speedMin, speedMax, delta);
        }
        clamp(pane, widget);

        if (!pane.started || !flag(widget, WidgetTypes.PROPERTY_SMOOTH_SCROLLING, true)) {
            pane.started = true;
            pane.visualScrollX = pane.scrollX;
            pane.visualScrollY = pane.scrollY;
        } else {
            pane.visualScrollX = catchUp(pane.visualScrollX, pane.scrollX, delta);
            pane.visualScrollY = catchUp(pane.visualScrollY, pane.scrollY, delta);
        }
    }

    /** Pushes a scroll that went past an edge back towards it, faster the further out it is. */
    private static float springBack(float scroll, float max, float distance, float speedMin, float speedMax, float delta) {
        if (scroll < 0) {
            float speed = speedMin + (speedMax - speedMin) * Math.min(1, -scroll / distance);
            return Math.min(0, scroll + speed * delta);
        }
        if (scroll > max) {
            float speed = speedMin + (speedMax - speedMin) * Math.min(1, (scroll - max) / distance);
            return Math.max(max, scroll - speed * delta);
        }
        return scroll;
    }

    /** What is drawn moves towards where the pane is heading, never slower than a steady crawl. */
    private static float catchUp(float visual, float target, float delta) {
        if (visual == target) return target;

        float step = Math.max(200 * delta, Math.abs(target - visual) * 7 * delta);
        return visual < target ? Math.min(target, visual + step) : Math.max(target, visual - step);
    }

    private void clamp(ScrollPaneComponent pane, WidgetComponent widget) {
        if (!flag(widget, WidgetTypes.PROPERTY_CLAMP, true)) return;

        float distance = flag(widget, WidgetTypes.PROPERTY_OVERSCROLL, true)
                ? number(widget, WidgetTypes.PROPERTY_OVERSCROLL_DISTANCE, 50) : 0;
        pane.scrollX = MathUtils.clamp(pane.scrollX, -distance, pane.maxX + distance);
        pane.scrollY = MathUtils.clamp(pane.scrollY, -distance, pane.maxY + distance);
    }

    // -------------------------------------------------------------------- the look

    private void layout(int entity, ScrollPaneComponent pane, WidgetComponent widget) {
        layoutBackground(entity, pane);

        int content = findPart(entity, WidgetTypes.ROLE_CONTENT);
        if (content != -1) {
            //scroll counts from the top, and a content shorter than the window hangs from it
            place(transformOf(content), -pane.visualScrollX,
                    pane.areaHeight - pane.contentHeight + pane.visualScrollY);
        }

        fade(pane, widget);

        boolean showX = showsBar(pane, widget, true);
        boolean showY = showsBar(pane, widget, false);
        layoutBar(entity, pane, widget, true, showX);
        layoutBar(entity, pane, widget, false, showY);
        layoutCorner(entity, pane, widget, showX && showY);
    }

    /** The surface the content slides over: the pane's whole rectangle, behind everything else. */
    private void layoutBackground(int entity, ScrollPaneComponent pane) {
        int background = findPart(entity, WidgetTypes.ROLE_BACKGROUND);
        if (background == -1) return;

        TransformComponent transform = transformOf(background);
        DimensionsComponent dimensions = dimensionsCM.get(background);
        if (transform == null || dimensions == null) return;

        resize(dimensions, transform, pane.areaWidth, pane.areaHeight);
        place(transform, 0, 0);

        //a background is the thing behind: it goes to the back whatever order it was given
        ZIndexComponent zIndex = zIndexCM.get(background);
        if (zIndex != null && zIndex.getZIndex() != 0) zIndex.setZIndex(0);
    }

    /**
     * Puts a scrollbar along its edge of the pane and its knob on it. The bar keeps the thickness it
     * was drawn with and nothing else: where it runs, and how far, belongs to the pane.
     */
    private void layoutBar(int entity, ScrollPaneComponent pane, WidgetComponent widget, boolean horizontal, boolean show) {
        int barPart = findPart(entity, horizontal ? WidgetTypes.ROLE_SCROLL_BAR_X : WidgetTypes.ROLE_SCROLL_BAR_Y);
        int knobPart = findPart(entity, horizontal ? WidgetTypes.ROLE_KNOB_X : WidgetTypes.ROLE_KNOB_Y);

        show(barPart, show, pane.fadeAlpha);
        show(knobPart, show, pane.fadeAlpha);
        if (!show || !trackOf(entity, pane, widget, horizontal, bar)) return;

        if (barPart != -1) {
            TransformComponent transform = transformOf(barPart);
            DimensionsComponent dimensions = dimensionsCM.get(barPart);
            if (transform != null && dimensions != null) {
                resize(dimensions, transform, horizontal ? bar.width : Float.NaN,
                        horizontal ? Float.NaN : bar.height);
                place(transform, bar.x, bar.y);
            }
        }
        if (knobPart == -1) return;

        TransformComponent transform = transformOf(knobPart);
        DimensionsComponent dimensions = dimensionsCM.get(knobPart);
        if (transform == null || dimensions == null) return;

        //the knob is a handle: a host holding the scene knows a press on it is a drag of its own
        inputTargetCM.create(knobPart).dragHandle = true;

        boolean variable = flag(widget, WidgetTypes.PROPERTY_VARIABLE_SIZE_KNOBS, true);
        float length = knobLength(pane, horizontal, variable, dimensions, transform);
        if (horizontal) {
            resize(dimensions, transform, length, Float.NaN);
            float across = dimensions.height * transform.scaleY;
            place(transform, bar.x + (bar.width - length) * pane.getVisualScrollPercentX(),
                    bar.y + (bar.height - across) / 2);
        } else {
            resize(dimensions, transform, Float.NaN, length);
            float across = dimensions.width * transform.scaleX;
            //the top of the bar is where the pane is looking when it is at the top of the content
            place(transform, bar.x + (bar.width - across) / 2,
                    bar.y + (bar.height - length) * (1 - pane.getVisualScrollPercentY()));
        }
    }

    /** The square where the two bars meet, shown only when both of them are. */
    private void layoutCorner(int entity, ScrollPaneComponent pane, WidgetComponent widget, boolean show) {
        int corner = findPart(entity, WidgetTypes.ROLE_CORNER);
        show(corner, show, pane.fadeAlpha);
        if (corner == -1 || !show) return;

        TransformComponent transform = transformOf(corner);
        DimensionsComponent dimensions = dimensionsCM.get(corner);
        if (transform == null || dimensions == null) return;

        float alongX = thickness(entity, true);
        float alongY = thickness(entity, false);
        resize(dimensions, transform, alongY, alongX);
        place(transform, flag(widget, WidgetTypes.PROPERTY_BARS_ON_RIGHT, true) ? pane.areaWidth - alongY : 0,
                flag(widget, WidgetTypes.PROPERTY_BARS_ON_BOTTOM, true) ? 0 : pane.areaHeight - alongX);
    }

    /**
     * Where a scrollbar runs: along its edge of the pane, the whole way, stopping short of the other
     * bar where both are shown so that the corner has its place.
     *
     * @return false when the pane has neither a bar nor a knob for that axis
     */
    private boolean trackOf(int entity, ScrollPaneComponent pane, WidgetComponent widget, boolean horizontal, Rectangle out) {
        float alongX = thickness(entity, true);
        float alongY = thickness(entity, false);
        boolean onRight = flag(widget, WidgetTypes.PROPERTY_BARS_ON_RIGHT, true);
        boolean onBottom = flag(widget, WidgetTypes.PROPERTY_BARS_ON_BOTTOM, true);
        float otherX = showsBar(pane, widget, true) ? alongX : 0;
        float otherY = showsBar(pane, widget, false) ? alongY : 0;

        if (horizontal) {
            if (alongX <= 0) return false;
            out.height = alongX;
            out.y = onBottom ? 0 : pane.areaHeight - alongX;
            out.width = pane.areaWidth - otherY;
            out.x = onRight ? 0 : otherY;
        } else {
            if (alongY <= 0) return false;
            out.width = alongY;
            out.x = onRight ? pane.areaWidth - alongY : 0;
            out.height = pane.areaHeight - otherX;
            out.y = onBottom ? otherX : 0;
        }
        return true;
    }

    /** How thick a scrollbar is: the bar as it was drawn, or its knob when it has no bar. */
    private float thickness(int entity, boolean horizontal) {
        int part = findPart(entity, horizontal ? WidgetTypes.ROLE_SCROLL_BAR_X : WidgetTypes.ROLE_SCROLL_BAR_Y);
        if (part == -1) part = findPart(entity, horizontal ? WidgetTypes.ROLE_KNOB_X : WidgetTypes.ROLE_KNOB_Y);
        if (part == -1 || !rectOf(part, span)) return 0;

        return horizontal ? span.height : span.width;
    }

    private boolean showsBar(ScrollPaneComponent pane, WidgetComponent widget, boolean horizontal) {
        return horizontal
                ? pane.maxX > 0 || flag(widget, WidgetTypes.PROPERTY_FORCE_SCROLL_X, false)
                : pane.maxY > 0 || flag(widget, WidgetTypes.PROPERTY_FORCE_SCROLL_Y, false);
    }

    /** As long as the share of the content in view, or the size it was drawn at. */
    private float knobLength(ScrollPaneComponent pane, boolean horizontal, boolean variable,
                             DimensionsComponent dimensions, TransformComponent transform) {
        float drawn = horizontal ? dimensions.width * transform.scaleX : dimensions.height * transform.scaleY;
        if (!variable) return drawn;

        float area = horizontal ? pane.areaWidth : pane.areaHeight;
        float content = horizontal ? pane.contentWidth : pane.contentHeight;
        float along = horizontal ? bar.width : bar.height;
        if (content <= 0 || area <= 0) return drawn;

        return Math.max(MIN_KNOB, along * Math.min(1, area / content));
    }

    /** Scrollbars fade out once the pane has been still for a while, and come straight back. */
    protected void fade(ScrollPaneComponent pane, WidgetComponent widget) {
        if (!flag(widget, WidgetTypes.PROPERTY_FADE_SCROLL_BARS, true)) {
            pane.fadeAlpha = 1;
            return;
        }

        float delta = engine.getDelta();
        if (pane.fadeDelayLeft > 0) {
            pane.fadeDelayLeft -= delta;
            return;
        }

        float duration = Math.max(0.0001f, number(widget, WidgetTypes.PROPERTY_FADE_DURATION, 1));
        pane.fadeAlpha = Math.max(0, pane.fadeAlpha - delta / duration);
    }

    private void resetFade(ScrollPaneComponent pane, WidgetComponent widget) {
        pane.fadeAlpha = 1;
        pane.fadeDelayLeft = number(widget, WidgetTypes.PROPERTY_FADE_DELAY, 1);
    }

    /** Picks the most relevant state the widget declares: disabled, dragged, hover, default. */
    protected void updateWidgetState(int entity, WidgetComponent widget, ScrollPaneComponent pane) {
        if (!pane.isTouchEnabled && widget.setState(WidgetTypes.STATE_DISABLED)) return;
        if (pane.isDragging() && widget.setState(WidgetTypes.STATE_DRAGGED)) return;
        if (pane.isHovered && widget.setState(WidgetTypes.STATE_HOVER)) return;
        widget.currentState = null; //back to the default state
    }

    private void notifyScroll(int entity, ScrollPaneComponent pane) {
        if (!pane.notified) {
            pane.notified = true;
            pane.notifiedX = pane.scrollX;
            pane.notifiedY = pane.scrollY;
            return;
        }
        if (pane.scrollX == pane.notifiedX && pane.scrollY == pane.notifiedY) return;

        pane.notifiedX = pane.scrollX;
        pane.notifiedY = pane.scrollY;
        for (int i = 0; i < pane.listeners.size; i++) {
            pane.listeners.get(i).scrolled(entity, pane.scrollX, pane.scrollY);
        }
    }

    // ------------------------------------------------------------------ the wheel and the bars

    @Override
    public void scrolled(UIEvent event) {
        int entity = event.listenerEntity;
        ScrollPaneComponent pane = paneCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);
        if (pane == null || widget == null || !pane.isTouchEnabled) return;
        if (pane.maxX <= 0 && pane.maxY <= 0) return;

        if (pane.maxY > 0) {
            pane.scrollY = MathUtils.clamp(pane.scrollY
                    + event.amountY * number(widget, WidgetTypes.PROPERTY_MOUSE_WHEEL_Y, 40), 0, pane.maxY);
        }
        if (pane.maxX > 0) {
            pane.scrollX = MathUtils.clamp(pane.scrollX
                    + event.amountX * number(widget, WidgetTypes.PROPERTY_MOUSE_WHEEL_X, 40), 0, pane.maxX);
        }

        stopFling(pane);
        resetFade(pane, widget);
        event.handle();
    }

    @Override
    public void touchDown(UIEvent event) {
        int entity = event.listenerEntity;
        ScrollPaneComponent pane = paneCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);
        if (pane == null || widget == null || !pane.isTouchEnabled || viewPortCM.has(entity)) return;
        if (!flag(widget, WidgetTypes.PROPERTY_SCROLL_BAR_TOUCH, true)) return;
        if (pane.touchPointer != -1 && !pane.dragging) return;

        if (pressBar(entity, pane, widget, event, true) || pressBar(entity, pane, widget, event, false)) {
            resetFade(pane, widget);
            event.handle();
        }
    }

    /**
     * A press on a scrollbar: on the knob it starts dragging it, beside it moves by one windowful.
     *
     * @return true when the press belonged to that bar
     */
    private boolean pressBar(int entity, ScrollPaneComponent pane, WidgetComponent widget, UIEvent event, boolean horizontal) {
        if (!barAt(entity, pane, widget, horizontal, event.localX, event.localY)) return false;

        if (knob.contains(event.localX, event.localY)) {
            catchPane(pane);
            pane.touchPointer = event.pointer;
            if (horizontal) {
                pane.draggingKnobX = true;
                pane.knobGrab = event.localX - knob.x;
            } else {
                pane.draggingKnobY = true;
                pane.knobGrab = event.localY - knob.y;
            }
            return true;
        }

        if (horizontal) {
            pane.scrollX = MathUtils.clamp(pane.scrollX
                    + (event.localX < knob.x ? -pane.areaWidth : pane.areaWidth), 0, pane.maxX);
        } else {
            //higher up the bar is nearer the top of the content
            pane.scrollY = MathUtils.clamp(pane.scrollY
                    + (event.localY > knob.y ? -pane.areaHeight : pane.areaHeight), 0, pane.maxY);
        }
        stopFling(pane);
        return true;
    }

    @Override
    public void touchDragged(UIEvent event) {
        int entity = event.listenerEntity;
        ScrollPaneComponent pane = paneCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);
        if (pane == null || widget == null || pane.touchPointer != event.pointer) return;
        if (!pane.draggingKnobX && !pane.draggingKnobY) return;

        boolean horizontal = pane.draggingKnobX;
        if (!barAt(entity, pane, widget, horizontal, Float.NaN, Float.NaN)) return;

        float travel = horizontal ? bar.width - knob.width : bar.height - knob.height;
        if (travel > 0) {
            float percent = horizontal
                    ? (event.localX - pane.knobGrab - bar.x) / travel
                    : 1 - (event.localY - pane.knobGrab - bar.y) / travel;
            if (horizontal) pane.setScrollPercentX(percent);
            else pane.setScrollPercentY(percent);
        }
        resetFade(pane, widget);
    }

    @Override
    public void touchUp(UIEvent event) {
        ScrollPaneComponent pane = paneCM.get(event.listenerEntity);
        if (pane == null || pane.touchPointer != event.pointer) return;
        if (!pane.draggingKnobX && !pane.draggingKnobY) return;

        pane.draggingKnobX = false;
        pane.draggingKnobY = false;
        pane.touchPointer = -1;
    }

    @Override
    public void enter(UIEvent event) {
        ScrollPaneComponent pane = paneCM.get(event.listenerEntity);
        if (pane != null) pane.isHovered = true;
    }

    @Override
    public void exit(UIEvent event) {
        ScrollPaneComponent pane = paneCM.get(event.listenerEntity);
        if (pane != null) pane.isHovered = false;
    }

    // ------------------------------------------------------------------ dragging the content

    private void flickDown(UIEvent event) {
        int entity = event.listenerEntity;
        ScrollPaneComponent pane = paneCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);
        if (pane == null || widget == null || !pane.isTouchEnabled || viewPortCM.has(entity)) return;
        if (!flickScrolls(widget)) return;
        if (pane.touchPointer != -1) return;
        //a press on a scrollbar belongs to the bar, not to the content
        if (barAt(entity, pane, widget, true, event.localX, event.localY)) return;
        if (barAt(entity, pane, widget, false, event.localX, event.localY)) return;

        pane.touchPointer = event.pointer;
        pane.dragging = false;
        pane.dragIdle = 0;
        pane.pressX = pane.lastX = event.localX;
        pane.pressY = pane.lastY = event.localY;
        catchPane(pane);
        event.followPointer();
    }

    /** Whether a press on the content itself scrolls the pane. */
    protected boolean flickScrolls(WidgetComponent widget) {
        return flag(widget, WidgetTypes.PROPERTY_FLICK_SCROLL, true);
    }

    private void flickDragged(UIEvent event) {
        int entity = event.listenerEntity;
        ScrollPaneComponent pane = paneCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);
        if (pane == null || widget == null || pane.touchPointer != event.pointer) return;
        if (pane.draggingKnobX || pane.draggingKnobY) return;

        if (!pane.dragging) {
            float tapSquare = number(widget, WidgetTypes.PROPERTY_FLICK_TAP_SQUARE, 8);
            if (Math.abs(event.localX - pane.pressX) < tapSquare
                    && Math.abs(event.localY - pane.pressY) < tapSquare) return;

            //the press is a scroll now: whatever it started on is let go of, unpressed
            pane.dragging = true;
            if (inputSystem != null) inputSystem.cancelTouchFocus(event.pointer, entity);
        }

        float dx = event.localX - pane.lastX;
        float dy = event.localY - pane.lastY;
        pane.lastX = event.localX;
        pane.lastY = event.localY;
        pane.dragIdle = 0;

        float invDelta = 1f / Math.max(engine.getDelta(), 0.0001f);
        pane.velocityX = dx * invDelta;
        pane.velocityY = dy * invDelta;

        //the content follows the pointer, so the window moves the other way
        pane.scrollX -= dx;
        pane.scrollY += dy;
        resetFade(pane, widget);
    }

    private void flickUp(UIEvent event) {
        int entity = event.listenerEntity;
        ScrollPaneComponent pane = paneCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);
        if (pane == null || widget == null || pane.touchPointer != event.pointer) return;

        boolean flung = pane.dragging && !event.cancelled && pane.dragIdle < FLING_IDLE
                && (Math.abs(pane.velocityX) > FLING_SPEED || Math.abs(pane.velocityY) > FLING_SPEED);
        pane.flingTimer = flung ? Math.max(0, number(widget, WidgetTypes.PROPERTY_FLING_TIME, 1)) : 0;
        if (!flung) stopFling(pane);

        pane.touchPointer = -1;
        pane.dragging = false;
    }

    /**
     * A pointer landing on the pane stops it dead where it is drawn: the fling ends and whatever
     * was still gliding towards its target gives up there, so the drag carries on from what the
     * eye sees rather than from somewhere the pane was still heading.
     */
    private void catchPane(ScrollPaneComponent pane) {
        stopFling(pane);
        pane.scrollX = pane.visualScrollX;
        pane.scrollY = pane.visualScrollY;
    }

    private void stopFling(ScrollPaneComponent pane) {
        pane.flingTimer = 0;
        pane.velocityX = 0;
        pane.velocityY = 0;
    }

    private void releaseSilently(int entity, ScrollPaneComponent pane) {
        if (pane.touchPointer != -1 && inputSystem != null) inputSystem.cancelTouchFocus(entity);
        pane.touchPointer = -1;
        pane.dragging = false;
        pane.draggingKnobX = false;
        pane.draggingKnobY = false;
        pane.isHovered = false;
        stopFling(pane);
    }

    // ------------------------------------------------------------------ parts and helpers

    /**
     * Measures a scrollbar and its knob into {@link #bar} and {@link #knob}.
     *
     * @param localX pass NaN to measure the bar without asking whether a point is on it
     * @return true when the bar is there, and the point with it
     */
    private boolean barAt(int entity, ScrollPaneComponent pane, WidgetComponent widget, boolean horizontal,
                          float localX, float localY) {
        if (!showsBar(pane, widget, horizontal)) return false;

        int barPart = findPart(entity, horizontal ? WidgetTypes.ROLE_SCROLL_BAR_X : WidgetTypes.ROLE_SCROLL_BAR_Y);
        int knobPart = findPart(entity, horizontal ? WidgetTypes.ROLE_KNOB_X : WidgetTypes.ROLE_KNOB_Y);
        if (knobPart == -1) return false;
        if (!trackOf(entity, pane, widget, horizontal, bar) || !rectOf(knobPart, knob)) return false;
        if (Float.isNaN(localX)) return true;

        //without a bar of its own there is no strip to click in, only the knob to grab
        return barPart != -1 ? bar.contains(localX, localY) : knob.contains(localX, localY);
    }

    /** @return the direct child playing that role, -1 if none does */
    private int findPart(int widget, String role) {
        NodeComponent node = nodeCM.get(widget);
        if (node == null) return -1;

        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            WidgetPartComponent part = partCM.get(child);
            if (part != null && role.equals(part.role)) return child;
        }
        return -1;
    }

    /** A part's rectangle as drawn, in the pane's own coordinates. */
    private boolean rectOf(int part, Rectangle out) {
        TransformComponent transform = transformOf(part);
        DimensionsComponent dimensions = dimensionsCM.get(part);
        if (transform == null || dimensions == null) return false;

        out.x = transform.x + transform.originX * (1 - transform.scaleX);
        out.y = transform.y + transform.originY * (1 - transform.scaleY);
        out.width = dimensions.width * transform.scaleX;
        out.height = dimensions.height * transform.scaleY;
        return true;
    }

    /** @param width NaN to leave that side as it is */
    private void resize(DimensionsComponent dimensions, TransformComponent transform, float width, float height) {
        if (!Float.isNaN(width)) dimensions.width = width / (transform.scaleX == 0 ? 1 : transform.scaleX);
        if (!Float.isNaN(height)) dimensions.height = height / (transform.scaleY == 0 ? 1 : transform.scaleY);
        if (dimensions.boundBox != null) {
            dimensions.boundBox.width = dimensions.width;
            dimensions.boundBox.height = dimensions.height;
        }
    }

    /** Puts a part's drawn rectangle exactly there, compensating for its own scale. */
    private void place(TransformComponent transform, float x, float y) {
        if (transform == null) return;

        transform.x = x - transform.originX * (1 - transform.scaleX);
        transform.y = y - transform.originY * (1 - transform.scaleY);
    }

    private void show(int part, boolean visible, float alpha) {
        if (part == -1) return;

        MainItemComponent mainItem = mainItemCM.get(part);
        if (mainItem != null) mainItem.visible = visible;

        TintComponent tint = tintCM.get(part);
        if (tint != null) tint.color.a = alpha;
    }

    /**
     * A part's real transform. The editor zeroes the transform of the composite it shows from the
     * inside, keeping the real one aside: writing into the zeroed one would move the view itself.
     */
    private TransformComponent transformOf(int part) {
        TransformComponent transform = transformCM.get(part);
        return transform == null ? null : transform.getRealComponent();
    }

    private static boolean flag(WidgetComponent widget, String key, boolean fallback) {
        String value = widget.properties.get(key);
        return value == null || value.isEmpty() ? fallback : Boolean.parseBoolean(value);
    }

    private static float number(WidgetComponent widget, String key, float fallback) {
        return ProgressBarSystem.parse(widget.properties.get(key), fallback);
    }
}
