package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.LayerMapComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.input.Touchable;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

/**
 * Brings real input to the scene: it finds the entity under the pointer and takes the event to it.
 *
 * It is an {@link InputProcessor} and it is not registered anywhere by itself, so a game decides
 * where the UI sits among its own input:
 * <pre>
 * multiplexer.addProcessor(sceneLoader.getEngine().getSystem(UIInputSystem.class));
 * </pre>
 * Put it before the game's own processors: whatever the UI takes never reaches them.
 *
 * Events are handled as they arrive, the way scene2d does it, so that a touch down can say straight
 * away whether the UI took it. What the system does every frame is keep the hover right, since an
 * entity may well move under a pointer that never moved.
 */
@All(ViewPortComponent.class)
public class UIInputSystem extends BaseEntitySystem implements InputProcessor {

    /** How deep the scene tree may go before input gives up walking it. */
    private static final int MAX_DEPTH = 128;

    protected ComponentMapper<ViewPortComponent> viewPortMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<ParentNodeComponent> parentMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<MainItemComponent> mainItemMapper;
    protected ComponentMapper<ZIndexComponent> zIndexMapper;
    protected ComponentMapper<LayerMapComponent> layerMapMapper;
    protected ComponentMapper<InputTargetComponent> inputTargetMapper;
    protected ComponentMapper<CompositeTransformComponent> compositeMapper;

    /** A pointer whose touch down was taken by a listener: it is that listener's until released. */
    private static class TouchFocus implements Pool.Poolable {
        int pointer = -1;
        int button = -1;
        int entity = -1;
        UIInputListener listener;

        @Override
        public void reset() {
            pointer = -1;
            button = -1;
            entity = -1;
            listener = null;
        }
    }

    private final Pool<UIEvent> eventPool = new Pool<UIEvent>() {
        @Override
        protected UIEvent newObject() {
            return new UIEvent();
        }
    };
    private final Pool<TouchFocus> focusPool = new Pool<TouchFocus>() {
        @Override
        protected TouchFocus newObject() {
            return new TouchFocus();
        }
    };

    /** Ancestor chains, lent out while an event travels one, so nesting never shares an array. */
    private final Pool<IntArray> chainPool = new Pool<IntArray>() {
        @Override
        protected IntArray newObject() {
            return new IntArray(8);
        }

        @Override
        protected void reset(IntArray chain) {
            chain.clear();
        }
    };

    private final SnapshotArray<TouchFocus> touchFocuses = new SnapshotArray<>(true, 4, TouchFocus[]::new);

    private final Vector2 tmp = new Vector2();
    /** One point per depth of the tree, so hit testing allocates nothing. */
    private final Array<Vector2> coordinates = new Array<>(true, 8, Vector2[]::new);

    private final IntArray downPointers = new IntArray(4);

    private float mouseScreenX, mouseScreenY;
    private boolean mouseKnown = false;

    private int hovered = -1;
    private final IntArray hoverChain = new IntArray(8);

    private int keyboardFocus = -1;
    /** True when a touch down moves the keyboard focus to whatever was clicked. */
    private boolean focusOnTouch = true;
    /** True when tab walks the keyboard from one focusable to the next. */
    private boolean focusTraversal = true;
    /** The focusables of the scene, gathered afresh every time tab is pressed. */
    private final IntArray focusStops = new IntArray(8);

    @Override
    protected void processSystem() {
        updateHover();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!downPointers.contains(pointer)) downPointers.add(pointer);
        pointerAt(screenX, screenY);

        int target = hit(screenX, screenY);
        if (focusOnTouch) updateKeyboardFocus(target);
        //a pointer on its way somewhere is not hovering anything
        updateHover();

        if (target == -1) return false;

        UIEvent event = obtain(UIEvent.Type.touchDown, screenX, screenY, target);
        event.pointer = pointer;
        event.button = button;
        boolean handled = dispatch(event, target, true);
        eventPool.free(event);
        return handled;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        pointerAt(screenX, screenY);
        return toFocused(UIEvent.Type.touchDragged, screenX, screenY, pointer, -1, false);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        downPointers.removeValue(pointer);
        pointerAt(screenX, screenY);

        boolean handled = toFocused(UIEvent.Type.touchUp, screenX, screenY, pointer, button, false);
        //the pointer is free again, so whatever is under it is hovered once more
        updateHover();
        return handled;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        downPointers.removeValue(pointer);
        boolean handled = toFocused(UIEvent.Type.touchUp, screenX, screenY, pointer, button, true);
        updateHover();
        return handled;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        setMousePosition(screenX, screenY);

        updateHover();

        if (hovered == -1) return false;

        UIEvent event = obtain(UIEvent.Type.mouseMoved, screenX, screenY, hovered);
        boolean handled = dispatch(event, hovered, false);
        eventPool.free(event);
        return handled;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (!mouseKnown) return false;

        int target = hit(mouseScreenX, mouseScreenY);
        if (target == -1) return false;

        UIEvent event = obtain(UIEvent.Type.scrolled, mouseScreenX, mouseScreenY, target);
        event.amountX = amountX;
        event.amountY = amountY;
        boolean handled = dispatch(event, target, false);
        eventPool.free(event);
        return handled;
    }

    @Override
    public boolean keyDown(int keycode) {
        boolean handled = toFocusedKey(UIEvent.Type.keyDown, keycode, (char) 0);
        //whoever holds the keys has the first word on tab: only a key nobody wanted moves the focus
        if (handled || keycode != Input.Keys.TAB || !focusTraversal) return handled;

        return moveFocus(shift() ? -1 : 1);
    }

    @Override
    public boolean keyUp(int keycode) {
        return toFocusedKey(UIEvent.Type.keyUp, keycode, (char) 0);
    }

    @Override
    public boolean keyTyped(char character) {
        return toFocusedKey(UIEvent.Type.keyTyped, -1, character);
    }

    private boolean toFocusedKey(UIEvent.Type type, int keycode, char character) {
        if (keyboardFocus == -1) return false;

        UIEvent event = obtain(type, mouseScreenX, mouseScreenY, keyboardFocus);
        event.keyCode = keycode;
        event.character = character;
        boolean handled = dispatch(event, keyboardFocus, false);
        eventPool.free(event);
        return handled;
    }

    /**
     * Takes a drag or a release straight to the listeners holding that pointer, wherever it has
     * travelled since the press.
     */
    private boolean toFocused(UIEvent.Type type, float screenX, float screenY, int pointer, int button, boolean cancelled) {
        if (touchFocuses.size == 0) return false;

        boolean release = type == UIEvent.Type.touchUp;
        boolean handled = false;
        boolean held = false;

        TouchFocus[] focuses = touchFocuses.begin();
        for (int i = 0, n = touchFocuses.size; i < n; i++) {
            TouchFocus focus = focuses[i];
            if (focus.pointer != pointer) continue;
            if (release && button != -1 && focus.button != button) continue;
            if (!stillListening(focus)) {
                touchFocuses.removeValue(focus, true);
                focusPool.free(focus);
                continue;
            }

            held = true;
            UIEvent event = obtain(type, screenX, screenY, focus.entity);
            event.pointer = pointer;
            event.button = focus.button;
            event.cancelled = cancelled;
            event.listenerEntity = focus.entity;
            toLocal(focus.entity, event);
            call(focus.listener, event);
            handled |= event.isHandled();
            eventPool.free(event);

            if (release) {
                touchFocuses.removeValue(focus, true);
                focusPool.free(focus);
            }
        }
        touchFocuses.end();
        //the pointer belongs to whoever took the press: the whole gesture is the interface's
        return held || handled;
    }

    // ------------------------------------------------------------ hit testing

    /**
     * @return the entity under the given screen point, -1 if none takes input there
     */
    public int hit(float screenX, float screenY) {
        IntBag roots = subscription.getEntities();
        int[] ids = roots.getData();
        for (int i = roots.size() - 1; i >= 0; i--) {
            int hit = hitRoot(ids[i], screenX, screenY);
            if (hit != -1) return hit;
        }
        return -1;
    }

    private int hitRoot(int root, float screenX, float screenY) {
        ViewPortComponent viewPort = viewPortMapper.get(root);
        if (viewPort == null || viewPort.viewPort == null) return -1;

        Vector2 coords = coordinatesAt(0).set(screenX, screenY);
        viewPort.viewPort.unproject(coords);

        //the root's own transform never moves its children, the same way the renderer draws them
        int hit = hitChildren(root, coords.x, coords.y, 1);
        if (hit != -1) return hit;

        return hits(root, coords.x, coords.y) ? root : -1;
    }

    private int hitChildren(int parent, float x, float y, int depth) {
        if (depth > MAX_DEPTH) return -1;

        NodeComponent node = nodeMapper.get(parent);
        if (node == null) return -1;

        LayerMapComponent layers = layerMapMapper.get(parent);
        //children are kept sorted back to front, so the last one drawn is the first one hit
        for (int i = node.children.size - 1; i >= 0; i--) {
            int child = node.children.get(i);

            MainItemComponent mainItem = mainItemMapper.get(child);
            if (mainItem != null && (!mainItem.visible || mainItem.culled)) continue;

            ZIndexComponent zIndex = zIndexMapper.get(child);
            if (layers != null && zIndex != null && !layers.isVisible(zIndex.layerHash)) continue;

            InputTargetComponent target = inputTargetMapper.get(child);
            if (target != null && target.touchable == Touchable.DISABLED) continue;

            if (!transformMapper.has(child)) continue;
            Vector2 local = coordinatesAt(depth).set(x, y);
            TransformMathUtils.parentToLocalCoordinates(child, local, transformMapper);
            float localX = local.x, localY = local.y;

            if (clipsAway(child, localX, localY)) continue;

            int inside = hitChildren(child, localX, localY, depth + 1);
            if (inside != -1) return inside;

            if (hits(child, localX, localY)) return child;
        }
        return -1;
    }

    /**
     * Whether a clipping composite leaves the point outside: what it does not draw there, it does
     * not hand to anybody either, itself included.
     */
    private boolean clipsAway(int entity, float localX, float localY) {
        CompositeTransformComponent composite = compositeMapper.get(entity);
        if (composite == null || !composite.scissorsEnabled) return false;

        DimensionsComponent dimensions = dimensionsMapper.get(entity);
        if (dimensions == null) return false;

        return localX < 0 || localY < 0 || localX >= dimensions.width || localY >= dimensions.height;
    }

    /** Whether the entity itself takes a point given in its own coordinates. */
    private boolean hits(int entity, float localX, float localY) {
        InputTargetComponent target = inputTargetMapper.get(entity);
        if (target == null || target.touchable != Touchable.ENABLED) return false;

        DimensionsComponent dimensions = dimensionsMapper.get(entity);
        return dimensions != null && dimensions.hit(localX, localY);
    }

    /** Whether the given screen point is over the entity, whatever else may be in front of it. */
    public boolean isOver(int entity, float screenX, float screenY) {
        if (!transformMapper.has(entity)) return false;

        tmp.set(screenX, screenY);
        TransformMathUtils.globalToLocalCoordinates(entity, tmp, transformMapper, parentMapper, viewPortMapper);
        DimensionsComponent dimensions = dimensionsMapper.get(entity);
        return dimensions != null && dimensions.hit(tmp.x, tmp.y);
    }

    // -------------------------------------------------------------- dispatching

    /**
     * Takes the event down the chain from the root to the target, calling the capture listeners,
     * and then back up from the target to the root, calling the ordinary ones. The first listener
     * that handles the event ends the journey, and on a touch down it is given the pointer.
     */
    private boolean dispatch(UIEvent event, int target, boolean takesPointer) {
        IntArray chain = chainPool.obtain();
        for (int entity = target, depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            chain.add(entity);
            ParentNodeComponent parent = parentMapper.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }

        for (int i = chain.size - 1; i >= 0 && !event.isHandled(); i--) {
            fireListeners(chain.get(i), event, true, takesPointer);
        }
        for (int i = 0; i < chain.size && !event.isHandled(); i++) {
            fireListeners(chain.get(i), event, false, takesPointer);
        }

        boolean handled = event.isHandled();
        chainPool.free(chain);
        return handled;
    }

    private void fireListeners(int entity, UIEvent event, boolean capture, boolean takesPointer) {
        InputTargetComponent target = inputTargetMapper.get(entity);
        if (target == null || target.touchable == Touchable.DISABLED) return;

        Array<UIInputListener> listeners = capture ? target.captureListeners : target.listeners;
        if (listeners.size == 0) return;

        event.listenerEntity = entity;
        event.capture = capture;
        toLocal(entity, event);

        for (int i = 0; i < listeners.size; i++) {
            UIInputListener listener = listeners.get(i);
            call(listener, event);

            //taking the event, or only asking to follow the pointer, both earn the pointer
            if (takesPointer && (event.isHandled() || event.follow)) {
                addTouchFocus(event.pointer, event.button, entity, listener);
            }
            event.follow = false;
            if (event.isHandled()) break;
        }
    }

    private void call(UIInputListener listener, UIEvent event) {
        switch (event.type) {
            case touchDown:
                listener.touchDown(event);
                break;
            case touchDragged:
                listener.touchDragged(event);
                break;
            case touchUp:
                listener.touchUp(event);
                break;
            case mouseMoved:
                listener.mouseMoved(event);
                break;
            case enter:
                listener.enter(event);
                break;
            case exit:
                listener.exit(event);
                break;
            case scrolled:
                listener.scrolled(event);
                break;
            case keyDown:
                listener.keyDown(event);
                break;
            case keyUp:
                listener.keyUp(event);
                break;
            case keyTyped:
                listener.keyTyped(event);
                break;
        }
    }

    // ------------------------------------------------------------------- hover

    /**
     * Finds what the pointer is over now and tells whoever it left and whoever it reached. Only one
     * entity is hovered at a time, the one in front, together with the entities it sits in.
     */
    private void updateHover() {
        if (!mouseKnown) return;

        //a pointer held down is busy with what it pressed, it hovers nothing
        int over = downPointers.size > 0 ? -1 : hit(mouseScreenX, mouseScreenY);
        if (over == hovered) return;

        IntArray next = chainPool.obtain();
        for (int entity = over, depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            next.add(entity);
            ParentNodeComponent parent = parentMapper.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }

        //out of the innermost first, then into the outermost first
        for (int i = 0; i < hoverChain.size; i++) {
            int entity = hoverChain.get(i);
            if (!next.contains(entity)) fireCrossing(entity, UIEvent.Type.exit, over);
        }
        for (int i = next.size - 1; i >= 0; i--) {
            int entity = next.get(i);
            if (!hoverChain.contains(entity)) fireCrossing(entity, UIEvent.Type.enter, hovered);
        }

        hovered = over;
        hoverChain.clear();
        hoverChain.addAll(next);
        chainPool.free(next);
    }

    private void fireCrossing(int entity, UIEvent.Type type, int related) {
        InputTargetComponent target = inputTargetMapper.get(entity);
        if (target == null || target.listeners.size == 0) return;

        UIEvent event = obtain(type, mouseScreenX, mouseScreenY, entity);
        event.relatedEntity = related;
        event.listenerEntity = entity;
        toLocal(entity, event);
        for (int i = 0; i < target.listeners.size; i++) {
            call(target.listeners.get(i), event);
        }
        eventPool.free(event);
    }

    /** Forgets where the pointer is, so nothing stays hovered: the pointer has left the scene. */
    public void clearHover() {
        mouseKnown = false;
        if (hovered == -1 && hoverChain.size == 0) return;

        for (int i = 0; i < hoverChain.size; i++) {
            fireCrossing(hoverChain.get(i), UIEvent.Type.exit, -1);
        }
        hovered = -1;
        hoverChain.clear();
    }

    public int getHoveredEntity() {
        return hovered;
    }

    public boolean isAnyPointerDown() {
        return downPointers.size > 0;
    }

    public float getMouseScreenX() {
        return mouseScreenX;
    }

    public float getMouseScreenY() {
        return mouseScreenY;
    }

    /**
     * Says where the mouse is, which is what hovering follows. A touch screen never calls this, so
     * nothing is ever hovered on one: a finger is either pressing something or away from the scene.
     */
    public void setMousePosition(float screenX, float screenY) {
        mouseScreenX = screenX;
        mouseScreenY = screenY;
        mouseKnown = true;
    }

    /** Remembers where a pointer is without making it a mouse that hovers. */
    private void pointerAt(float screenX, float screenY) {
        mouseScreenX = screenX;
        mouseScreenY = screenY;
    }

    // ------------------------------------------------------------------- focus

    /**
     * @param entity the entity keys are sent to, -1 for none
     */
    public void setKeyboardFocus(int entity) {
        keyboardFocus = entity;
    }

    public int getKeyboardFocus() {
        return keyboardFocus;
    }

    /** Whether a click moves the keyboard focus to what was clicked. On by default. */
    public void setFocusOnTouch(boolean focusOnTouch) {
        this.focusOnTouch = focusOnTouch;
    }

    /** Whether tab walks from one focusable to the next. On by default. */
    public void setFocusTraversal(boolean focusTraversal) {
        this.focusTraversal = focusTraversal;
    }

    public boolean isFocusTraversal() {
        return focusTraversal;
    }

    /**
     * Hands the keyboard to the focusable after the one that holds it, in the order the scene tree
     * reads: what tab does, and what a game does to move on by itself. The ends wrap around, and
     * with nothing focused the first one is taken.
     *
     * @param direction 1 for the next one, -1 for the one before
     * @return whether there was somewhere for the keyboard to go
     */
    public boolean moveFocus(int direction) {
        focusStops.clear();
        if (keyboardFocus == -1) {
            IntBag roots = subscription.getEntities();
            int[] ids = roots.getData();
            for (int i = 0, n = roots.size(); i < n; i++) collectStops(ids[i], 0);
        } else {
            collectStops(rootOf(keyboardFocus), 0);
        }
        if (focusStops.size == 0) return false;

        int current = focusStops.indexOf(keyboardFocus);
        int next;
        if (current == -1) {
            next = direction > 0 ? 0 : focusStops.size - 1;
        } else {
            next = (current + direction + focusStops.size) % focusStops.size;
        }

        setKeyboardFocus(focusStops.get(next));
        return true;
    }

    /**
     * Gathers the focusables of a branch, the entity itself before the ones inside it and children
     * in the order they are kept. What input cannot reach is left out, together with everything it
     * holds: the keyboard does not land where a click could not.
     */
    private void collectStops(int entity, int depth) {
        if (depth > MAX_DEPTH) return;

        MainItemComponent mainItem = mainItemMapper.get(entity);
        if (mainItem != null && (!mainItem.visible || mainItem.culled)) return;

        InputTargetComponent target = inputTargetMapper.get(entity);
        if (target != null && target.touchable == Touchable.DISABLED) return;
        if (target != null && target.focusable && target.touchable == Touchable.ENABLED) focusStops.add(entity);

        NodeComponent node = nodeMapper.get(entity);
        if (node == null) return;

        LayerMapComponent layers = layerMapMapper.get(entity);
        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);

            ZIndexComponent zIndex = zIndexMapper.get(child);
            if (layers != null && zIndex != null && !layers.isVisible(zIndex.layerHash)) continue;

            collectStops(child, depth + 1);
        }
    }

    /** Whether a shift key is down. Tests have no {@link Gdx#input}, so they say it themselves. */
    protected boolean shift() {
        if (Gdx.input == null) return false;
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    private void updateKeyboardFocus(int target) {
        for (int entity = target, depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            InputTargetComponent input = inputTargetMapper.get(entity);
            if (input != null && input.focusable && input.touchable != Touchable.DISABLED) {
                setKeyboardFocus(entity);
                return;
            }
            ParentNodeComponent parent = parentMapper.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        setKeyboardFocus(-1);
    }

    /**
     * Takes a pointer away from everybody but one entity, telling the others the press was
     * cancelled. This is how something that follows a pointer takes it over: a scroll pane turning
     * a press into a drag leaves the button underneath unpressed rather than clicked.
     */
    public void cancelTouchFocus(int pointer, int keepEntity) {
        TouchFocus[] focuses = touchFocuses.begin();
        for (int i = 0, n = touchFocuses.size; i < n; i++) {
            TouchFocus focus = focuses[i];
            if (focus.pointer != pointer || focus.entity == keepEntity) continue;

            drop(focus);
        }
        touchFocuses.end();
    }

    /**
     * Lets go of everything: the pointers being followed, whatever is hovered and whatever has the
     * keys. A host taking its input back calls this, so a gesture it will not deliver the rest of
     * ends as a cancelled press instead of hanging half done.
     */
    public void clearFocus() {
        TouchFocus[] focuses = touchFocuses.begin();
        for (int i = 0, n = touchFocuses.size; i < n; i++) drop(focuses[i]);
        touchFocuses.end();

        downPointers.clear();
        keyboardFocus = -1;
        clearHover();
    }

    /** Takes the pointer away from the focus, telling its listener the press was cancelled. */
    private void drop(TouchFocus focus) {
        touchFocuses.removeValue(focus, true);
        if (stillListening(focus)) {
            UIEvent event = obtain(UIEvent.Type.touchUp, mouseScreenX, mouseScreenY, focus.entity);
            event.pointer = focus.pointer;
            event.button = focus.button;
            event.cancelled = true;
            event.listenerEntity = focus.entity;
            toLocal(focus.entity, event);
            call(focus.listener, event);
            eventPool.free(event);
        }
        focusPool.free(focus);
    }

    /** Drops the pointers pressed on the entity, and the keyboard focus if it holds it. */
    public void cancelTouchFocus(int entity) {
        for (int i = touchFocuses.size - 1; i >= 0; i--) {
            TouchFocus focus = touchFocuses.get(i);
            if (focus.entity != entity) continue;

            touchFocuses.removeIndex(i);
            focusPool.free(focus);
        }
        if (keyboardFocus == entity) keyboardFocus = -1;
    }

    private void addTouchFocus(int pointer, int button, int entity, UIInputListener listener) {
        if (pointer == -1) return;

        TouchFocus focus = focusPool.obtain();
        focus.pointer = pointer;
        focus.button = button;
        focus.entity = entity;
        focus.listener = listener;
        touchFocuses.add(focus);
    }

    /** False once the entity has dropped the listener, or the entity itself is gone. */
    private boolean stillListening(TouchFocus focus) {
        InputTargetComponent target = inputTargetMapper.get(focus.entity);
        if (target == null) return false;
        return target.listeners.contains(focus.listener, true) || target.captureListeners.contains(focus.listener, true);
    }

    // ------------------------------------------------------------------ plumbing

    private UIEvent obtain(UIEvent.Type type, float screenX, float screenY, int target) {
        UIEvent event = eventPool.obtain();
        event.type = type;
        event.screenX = screenX;
        event.screenY = screenY;
        event.target = target;
        toScene(event);
        return event;
    }

    /** Puts the pointer in the coordinates of the scene the event's target belongs to. */
    private void toScene(UIEvent event) {
        int root = rootOf(event.target);
        if (root == -1) return;

        ViewPortComponent viewPort = viewPortMapper.get(root);
        if (viewPort == null || viewPort.viewPort == null) return;

        tmp.set(event.screenX, event.screenY);
        viewPort.viewPort.unproject(tmp);
        event.sceneX = tmp.x;
        event.sceneY = tmp.y;
    }

    /** The entity at the top of the branch the given one hangs from, -1 if it has none. */
    private int rootOf(int entity) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            ParentNodeComponent parent = parentMapper.get(entity);
            int parentEntity = parent == null ? -1 : parent.parentEntity;
            if (parentEntity == -1) return entity;
            entity = parentEntity;
        }
        return entity;
    }

    private void toLocal(int entity, UIEvent event) {
        tmp.set(event.screenX, event.screenY);

        ViewPortComponent viewPort = viewPortMapper.get(entity);
        if (viewPort != null && viewPort.viewPort != null) {
            viewPort.viewPort.unproject(tmp);
        } else if (transformMapper.has(entity)) {
            TransformMathUtils.globalToLocalCoordinates(entity, tmp, transformMapper, parentMapper, viewPortMapper);
        }
        event.localX = tmp.x;
        event.localY = tmp.y;
    }

    private Vector2 coordinatesAt(int depth) {
        while (coordinates.size <= depth) coordinates.add(new Vector2());
        return coordinates.get(depth);
    }
}
