package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.additional.OverlayComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.components.widget.WindowComponent;
import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.input.Touchable;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.utils.TransformMathUtils;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * Runs windows: the title bar drags them, a press brings them in front of their siblings, the close
 * part, Escape or a tap on the backdrop closes them, and a modal one is opened over everything else
 * and keeps all input to itself while it is open.
 *
 * A window that is neither modal nor has a backdrop is drawn where it sits in the scene, as any
 * composite is. One with a backdrop is opened over everything, like a modal one, and the backdrop is
 * stretched over the whole viewport behind it, the way scene2d draws a window's stage background;
 * whether what lies behind can still be reached is up to the modal setting.
 */
@All({WindowComponent.class, WidgetComponent.class})
public class WindowSystem extends BaseEntitySystem implements UIInputListener {

    private static final int MAX_DEPTH = 128;

    protected ComponentMapper<WindowComponent> windowCM;
    protected ComponentMapper<WidgetComponent> widgetCM;
    protected ComponentMapper<WidgetPartComponent> partCM;
    protected ComponentMapper<ViewPortComponent> viewPortCM;
    protected ComponentMapper<InputTargetComponent> inputTargetCM;
    protected ComponentMapper<NodeComponent> nodeCM;
    protected ComponentMapper<ParentNodeComponent> parentCM;
    protected ComponentMapper<MainItemComponent> mainItemCM;
    protected ComponentMapper<TransformComponent> transformCM;
    protected ComponentMapper<DimensionsComponent> dimensionsCM;
    protected ComponentMapper<CompositeTransformComponent> compositeCM;
    protected ComponentMapper<LabelComponent> labelCM;
    protected ComponentMapper<ButtonComponent> buttonCM;
    protected ComponentMapper<ZIndexComponent> zIndexCM;
    protected ComponentMapper<OverlayComponent> overlayCM;

    protected UIInputSystem inputSystem;

    private final Vector2 tmp = new Vector2();
    private final Vector2 tmp2 = new Vector2();

    /** Watches every press on the way down, whatever takes it in the end, to bring the window in front. */
    private final UIInputListener frontWatcher = new UIInputListener() {
        @Override
        public void touchDown(UIEvent event) {
            int entity = event.listenerEntity;
            WindowComponent window = windowCM.get(entity);
            if (window == null || !window.isOpen || !interactive(entity)) return;
            toFront(entity);
        }
    };

    /** The close part when it is no button: a click on it closes the window. */
    private final UIInputListener closeWatcher = new UIInputListener() {
        @Override
        public void touchDown(UIEvent event) {
            int entity = windowOf(event.listenerEntity);
            WindowComponent window = entity == -1 ? null : windowCM.get(entity);
            if (window == null || !interactive(entity) || window.closePointer != -1) return;

            window.closePointer = event.pointer;
            event.handle();
        }

        @Override
        public void touchUp(UIEvent event) {
            int entity = windowOf(event.listenerEntity);
            WindowComponent window = entity == -1 ? null : windowCM.get(entity);
            if (window == null || window.closePointer != event.pointer) return;

            window.closePointer = -1;
            if (event.cancelled || !interactive(entity)) return;
            if (inputSystem != null && !inputSystem.isOver(event.listenerEntity, event.screenX, event.screenY)) return;
            window.close();
        }
    };

    /** The close part when it is a button: its click closes the window, its looks stay its own. */
    private final ButtonComponent.ButtonListener closeClicks = new ButtonComponent.ButtonListener() {
        @Override
        public void touchUp(int entity) {
        }

        @Override
        public void touchDown(int entity) {
        }

        @Override
        public void clicked(int entity) {
            int window = windowOf(entity);
            if (window != -1 && interactive(window)) windowCM.get(window).close();
        }
    };

    /**
     * The backdrop, input target only while a tap on it closes the window. It lies under the whole
     * window too, so a press that is really on the window is let through to it.
     */
    private final UIInputListener backdropWatcher = new UIInputListener() {
        @Override
        public void touchDown(UIEvent event) {
            int entity = windowOf(event.listenerEntity);
            WindowComponent window = entity == -1 ? null : windowCM.get(entity);
            if (window == null || !interactive(entity) || window.backdropPointer != -1) return;
            if (inputSystem != null && inputSystem.isOver(entity, event.screenX, event.screenY)) return;

            window.backdropPointer = event.pointer;
            event.handle();
        }

        @Override
        public void touchUp(UIEvent event) {
            int entity = windowOf(event.listenerEntity);
            WindowComponent window = entity == -1 ? null : windowCM.get(entity);
            if (window == null || window.backdropPointer != event.pointer) return;

            window.backdropPointer = -1;
            if (event.cancelled || !interactive(entity)) return;
            if (inputSystem != null && inputSystem.isOver(entity, event.screenX, event.screenY)) return;
            window.close();
        }
    };

    public WindowSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public WindowSystem() {
    }

    @Override
    protected void inserted(int entityId) {
        InputTargetComponent target = inputTargetCM.create(entityId);
        target.addListener(this);
        target.addCaptureListener(frontWatcher);
        //clicking the window gives it the keyboard, which is where Escape goes
        target.focusable = true;
    }

    @Override
    protected void removed(int entityId) {
        InputTargetComponent target = inputTargetCM.get(entityId);
        if (target != null) {
            target.removeListener(this);
            target.removeCaptureListener(frontWatcher);
        }
        if (inputSystem != null) {
            inputSystem.cancelTouchFocus(entityId);
            inputSystem.closeOverlay(entityId);
        }
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
        WindowComponent window = windowCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);

        //the backdrop reaches far outside the window, which a composite growing to its children would follow
        CompositeTransformComponent composite = compositeCM.get(entity);
        if (composite != null) composite.automaticResize = false;

        followSettings(widget, window);

        boolean live = interactive(entity);
        //the composite being viewed from the inside in the editor is opened, not dragged
        if (!live || viewPortCM.has(entity) || !window.isOpen) window.dragPointer = -1;
        if (!live) {
            window.closePointer = -1;
            window.backdropPointer = -1;
        }

        //a window that starts closed, or is put back by the editor, is closed at once, never animated
        boolean startsClosed = !window.notified && !window.isOpen;
        if (startsClosed) snapParts(entity, 0);

        //outside of the game the window is something being designed, it always shows; a closing one
        //shows until its closed look has finished playing
        boolean shown = !live || window.isOpen || !startsClosed && !closedLookPlayed(entity, widget);
        setVisible(entity, shown);
        //a closing window is only a picture now: it takes no input and blocks nothing
        InputTargetComponent target = inputTargetCM.get(entity);
        if (target != null) target.touchable = !live || window.isOpen ? Touchable.ENABLED : Touchable.DISABLED;

        markTitleBar(findPart(entity, WidgetTypes.ROLE_TITLE_BAR));
        watchClose(findPart(entity, WidgetTypes.ROLE_CLOSE));
        showTitle(widget, findPart(entity, WidgetTypes.ROLE_TITLE));
        int backdrop = findPart(entity, WidgetTypes.ROLE_BACKDROP);
        followOverlay(entity, widget, window, live && shown, backdrop != -1);
        placeBackdrop(entity, widget, window, backdrop, live && shown);
        if (live && window.isOpen && flag(widget, WidgetTypes.PROPERTY_KEEP_WITHIN_VIEWPORT, true)) keepWithinViewport(entity);

        updateWidgetState(entity, widget, window);
        notifyListeners(entity, window);
    }

    /**
     * Whether the window answers the pointer and the keys, and closes. The editor keeps the ones it
     * shows still and open, until the scene is being tried out.
     */
    protected boolean interactive(int entity) {
        return true;
    }

    private void followSettings(WidgetComponent widget, WindowComponent window) {
        String open = widget.properties.get(WidgetTypes.PROPERTY_OPEN);
        if (open == null ? window.openSetting != null : !open.equals(window.openSetting)) {
            window.openSetting = open;
            window.isOpen = open == null || Boolean.parseBoolean(open);
        }
    }

    /**
     * Picks the most relevant state the widget declares: closed, dragged, default. Outside a game a
     * window is designed in its open look, whatever it starts as.
     */
    protected void updateWidgetState(int entity, WidgetComponent widget, WindowComponent window) {
        if (!window.isOpen && interactive(entity) && widget.setState(WidgetTypes.STATE_CLOSED)) return;
        if (window.isDragging() && widget.setState(WidgetTypes.STATE_DRAGGED)) return;
        widget.currentState = null; //back to the default state
    }

    // ------------------------------------------------------------------ the parts

    /** The title bar is a handle: the press goes up to the window, and a host knows it drags. */
    private void markTitleBar(int titleBar) {
        if (titleBar == -1) return;
        InputTargetComponent target = inputTargetCM.get(titleBar);
        if (target == null) target = inputTargetCM.create(titleBar);
        target.dragHandle = true;
    }

    private void watchClose(int close) {
        if (close == -1) return;

        ButtonComponent button = buttonCM.get(close);
        if (button != null) {
            if (!button.listeners.contains(closeClicks, true)) button.addListener(closeClicks);
            return;
        }

        InputTargetComponent target = inputTargetCM.get(close);
        if (target == null) target = inputTargetCM.create(close);
        target.addListener(closeWatcher);
    }

    /** Writes the title into its label, only when it differs, so nothing is laid out for nothing. */
    private void showTitle(WidgetComponent widget, int title) {
        LabelComponent label = title == -1 ? null : labelCM.get(title);
        if (label == null) return;

        String text = widget.properties.get(WidgetTypes.PROPERTY_TITLE, "");
        if (!sameText(label.getText(), text)) label.setText(text);
    }

    private static boolean sameText(CharSequence shown, String text) {
        if (shown == null) return text.isEmpty();
        if (shown.length() != text.length()) return false;
        for (int i = 0; i < text.length(); i++) {
            if (shown.charAt(i) != text.charAt(i)) return false;
        }
        return true;
    }

    /**
     * An open window that is modal, or has a backdrop, is an overlay: drawn after the whole scene, so
     * its backdrop covers everything. Modal or not then only decides whether what is behind it can
     * still be reached.
     */
    private void followOverlay(int entity, WidgetComponent widget, WindowComponent window, boolean live, boolean backdrop) {
        //live here means shown in a game: open, or still closing
        if (inputSystem == null) return;

        //modal only while open: a closing window still draws over everything, but blocks nothing
        boolean modal = window.isOpen && flag(widget, WidgetTypes.PROPERTY_MODAL, false);
        boolean wanted = live && (modal || backdrop || inputSystem.isOverlayOpen(entity) && !window.isOpen);
        boolean open = inputSystem.isOverlayOpen(entity);
        if (wanted && !open) {
            inputSystem.openOverlay(entity, modal, null);
        } else if (!wanted && open) {
            inputSystem.closeOverlay(entity);
        } else if (open) {
            //the setting changed while it was open
            OverlayComponent overlay = overlayCM.get(entity);
            if (overlay != null) overlay.modal = modal;
        }
    }

    /**
     * Stretches the backdrop over what the camera sees, behind everything else in the window, while
     * the window is open in a game; in the editor it would only hide the scene being designed.
     */
    private void placeBackdrop(int entity, WidgetComponent widget, WindowComponent window, int backdrop, boolean live) {
        if (backdrop == -1) return;

        //live here means shown in a game: open, or still closing
        boolean shown = live;
        setVisible(backdrop, shown);

        InputTargetComponent target = inputTargetCM.get(backdrop);
        if (target == null) target = inputTargetCM.create(backdrop);
        target.addListener(backdropWatcher);
        //no tap to close it: the backdrop takes nothing, and what is behind a window that is not modal is reachable
        target.touchable = shown && window.isOpen && flag(widget, WidgetTypes.PROPERTY_CLOSE_ON_BACKDROP_TAP, false)
                ? Touchable.ENABLED : Touchable.DISABLED;

        ZIndexComponent zIndex = zIndexCM.get(backdrop);
        if (zIndex != null && zIndex.getZIndex() != 0) zIndex.setZIndex(0);

        if (!shown) return;

        Camera camera = cameraOf(entity);
        TransformComponent transform = transformCM.get(backdrop);
        DimensionsComponent dimensions = dimensionsCM.get(backdrop);
        if (camera == null || transform == null || dimensions == null) return;

        float zoom = camera instanceof OrthographicCamera ? ((OrthographicCamera) camera).zoom : 1;
        float halfWidth = camera.viewportWidth * zoom / 2, halfHeight = camera.viewportHeight * zoom / 2;
        TransformMathUtils.sceneToLocalCoordinates(entity,
                tmp.set(camera.position.x - halfWidth, camera.position.y - halfHeight), transformCM, parentCM);
        TransformMathUtils.sceneToLocalCoordinates(entity,
                tmp2.set(camera.position.x + halfWidth, camera.position.y + halfHeight), transformCM, parentCM);

        transform.x = Math.min(tmp.x, tmp2.x);
        transform.y = Math.min(tmp.y, tmp2.y);
        if (transform.scaleX != 0) dimensions.width = Math.abs(tmp2.x - tmp.x) / Math.abs(transform.scaleX);
        if (transform.scaleY != 0) dimensions.height = Math.abs(tmp2.y - tmp.y) / Math.abs(transform.scaleY);
    }

    // ------------------------------------------------------------------ moving

    /** Pushes the window back inside what the camera sees; one too big for it keeps its top left in sight. */
    private void keepWithinViewport(int entity) {
        Camera camera = cameraOf(entity);
        DimensionsComponent dimensions = dimensionsCM.get(entity);
        TransformComponent transform = transformCM.get(entity);
        if (camera == null || dimensions == null || transform == null) return;

        float zoom = camera instanceof OrthographicCamera ? ((OrthographicCamera) camera).zoom : 1;
        float left = camera.position.x - camera.viewportWidth * zoom / 2;
        float right = camera.position.x + camera.viewportWidth * zoom / 2;
        float bottom = camera.position.y - camera.viewportHeight * zoom / 2;
        float top = camera.position.y + camera.viewportHeight * zoom / 2;

        TransformMathUtils.localToSceneCoordinates(entity, tmp.set(0, 0), transformCM, parentCM);
        TransformMathUtils.localToSceneCoordinates(entity, tmp2.set(dimensions.width, dimensions.height), transformCM, parentCM);
        float minX = Math.min(tmp.x, tmp2.x), maxX = Math.max(tmp.x, tmp2.x);
        float minY = Math.min(tmp.y, tmp2.y), maxY = Math.max(tmp.y, tmp2.y);

        float dx = 0, dy = 0;
        if (maxX > right) dx = right - maxX;
        if (minX + dx < left) dx = left - minX;
        if (minY < bottom) dy = bottom - minY;
        if (maxY + dy > top) dy = top - maxY;
        if (dx == 0 && dy == 0) return;

        //the shift is worked out on the scene and made in the parent, whatever its scale
        ParentNodeComponent parent = parentCM.get(entity);
        int parentEntity = parent == null ? -1 : parent.parentEntity;
        tmp.set(minX, minY);
        tmp2.set(minX + dx, minY + dy);
        if (parentEntity != -1) {
            TransformMathUtils.sceneToLocalCoordinates(parentEntity, tmp, transformCM, parentCM);
            TransformMathUtils.sceneToLocalCoordinates(parentEntity, tmp2, transformCM, parentCM);
        }
        transform.x += tmp2.x - tmp.x;
        transform.y += tmp2.y - tmp.y;
    }

    /**
     * Puts the window in front of its siblings on its layer, the way scene2d's toFront does. A modal
     * one is an overlay, so it is brought in front of the other overlays instead.
     */
    private void toFront(int entity) {
        if (inputSystem != null && inputSystem.isOverlayOpen(entity)) {
            inputSystem.openOverlay(entity, flag(widgetCM.get(entity), WidgetTypes.PROPERTY_MODAL, false), null);
            return;
        }

        ParentNodeComponent parent = parentCM.get(entity);
        NodeComponent siblings = parent == null || parent.parentEntity == -1 ? null : nodeCM.get(parent.parentEntity);
        ZIndexComponent mine = zIndexCM.get(entity);
        if (siblings == null || mine == null) return;

        int highest = Integer.MIN_VALUE;
        for (int i = 0; i < siblings.children.size; i++) {
            int sibling = siblings.children.get(i);
            if (sibling == entity) continue;
            ZIndexComponent zIndex = zIndexCM.get(sibling);
            if (zIndex != null && zIndex.layerHash == mine.layerHash) highest = Math.max(highest, zIndex.getZIndex());
        }
        if (highest != Integer.MIN_VALUE && mine.getZIndex() <= highest) mine.setZIndex(highest + 1);
    }

    // ------------------------------------------------------------------ the pointer and the keys

    /**
     * A press on the window stays there: on the title bar of a movable window it starts a drag, and
     * anywhere else it is only kept from what lies behind the window.
     */
    @Override
    public void touchDown(UIEvent event) {
        int entity = event.listenerEntity;
        WindowComponent window = windowCM.get(entity);
        if (window == null || !window.isOpen || !interactive(entity) || viewPortCM.has(entity)) return;

        int titleBar = findPart(entity, WidgetTypes.ROLE_TITLE_BAR);
        if (titleBar != -1 && window.dragPointer == -1 && isInside(event.target, titleBar)
                && flag(widgetCM.get(entity), WidgetTypes.PROPERTY_MOVABLE, true)) {
            window.dragPointer = event.pointer;
            window.grabX = event.localX;
            window.grabY = event.localY;
        }
        event.handle();
    }

    @Override
    public void touchDragged(UIEvent event) {
        int entity = event.listenerEntity;
        WindowComponent window = windowCM.get(entity);
        if (window == null || window.dragPointer != event.pointer) return;

        //the pointer in the window as it is now, so what is left over is how far it moved
        TransformComponent transform = transformCM.get(entity);
        transform.x += (event.localX - window.grabX) * transform.scaleX;
        transform.y += (event.localY - window.grabY) * transform.scaleY;
    }

    @Override
    public void touchUp(UIEvent event) {
        WindowComponent window = windowCM.get(event.listenerEntity);
        if (window != null && window.dragPointer == event.pointer) window.dragPointer = -1;
    }

    @Override
    public void keyDown(UIEvent event) {
        if (event.keyCode != Input.Keys.ESCAPE) return;

        int entity = event.listenerEntity;
        WindowComponent window = windowCM.get(entity);
        if (window == null || !window.isOpen || !interactive(entity)) return;
        if (!flag(widgetCM.get(entity), WidgetTypes.PROPERTY_CLOSE_ON_ESCAPE, false)) return;

        window.close();
        event.handle();
    }

    // ------------------------------------------------------------------ listeners

    /** Tells the listeners once per change, whoever made it; a closed window lets go of every input. */
    private void notifyListeners(int entity, WindowComponent window) {
        TransformComponent transform = transformCM.get(entity);
        float x = transform == null ? 0 : transform.x, y = transform == null ? 0 : transform.y;

        if (!window.notified) {
            window.notified = true;
            window.notifiedOpen = window.isOpen;
            window.notifiedX = x;
            window.notifiedY = y;
            return;
        }

        if (window.isOpen != window.notifiedOpen) {
            window.notifiedOpen = window.isOpen;
            if (!window.isOpen) letGo(entity, window);
            for (int i = 0; i < window.listeners.size; i++) {
                if (window.isOpen) window.listeners.get(i).opened(entity);
                else window.listeners.get(i).closed(entity);
            }
        }

        if (x != window.notifiedX || y != window.notifiedY) {
            window.notifiedX = x;
            window.notifiedY = y;
            for (int i = 0; i < window.listeners.size; i++) window.listeners.get(i).moved(entity, x, y);
        }
    }

    /** A closed window holds no pointer and no keys, nor does anything inside it. */
    private void letGo(int entity, WindowComponent window) {
        window.dragPointer = -1;
        window.closePointer = -1;
        window.backdropPointer = -1;
        if (inputSystem == null) return;

        inputSystem.cancelTouchFocus(entity);
        int focus = inputSystem.getKeyboardFocus();
        if (focus != -1 && isInside(focus, entity)) inputSystem.setKeyboardFocus(-1);
    }

    // ------------------------------------------------------------------ the closed look

    /**
     * Whether every part of the window shows the state it is in, with nothing still on its way:
     * a closing window is hidden only then. Parts of a nested widget follow that widget, not this one.
     */
    private boolean closedLookPlayed(int entity, WidgetComponent widget) {
        if (!WidgetTypes.STATE_CLOSED.equals(widget.getState())) return false;
        return settled(entity, widget.getState(), 0);
    }

    private boolean settled(int parent, String state, int depth) {
        if (depth > MAX_DEPTH) return true;
        NodeComponent node = nodeCM.get(parent);
        if (node == null) return true;

        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            WidgetPartComponent part = partCM.get(child);
            if (part != null && (part.tweens.size > 0 || !state.equals(part.appliedState))) return false;
            if (!widgetCM.has(child) && !settled(child, state, depth + 1)) return false;
        }
        return true;
    }

    /** Has every part of the window take its next state at once. */
    private void snapParts(int parent, int depth) {
        if (depth > MAX_DEPTH) return;
        NodeComponent node = nodeCM.get(parent);
        if (node == null) return;

        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            WidgetPartComponent part = partCM.get(child);
            if (part != null) part.snapNext = true;
            if (!widgetCM.has(child)) snapParts(child, depth + 1);
        }
    }

    // ------------------------------------------------------------------ the tree

    /**
     * @return the part playing that role anywhere in the window - the title and the close button sit
     * in the title bar - leaving out what a nested widget holds, which plays roles of its own; -1 if
     * none does
     */
    protected int findPart(int widget, String role) {
        return findPart(widget, role, 0);
    }

    private int findPart(int parent, String role, int depth) {
        if (depth > MAX_DEPTH) return -1;
        NodeComponent node = nodeCM.get(parent);
        if (node == null) return -1;

        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            WidgetPartComponent part = partCM.get(child);
            if (part != null && role.equals(part.role)) return child;
        }
        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            if (widgetCM.has(child)) continue;
            int found = findPart(child, role, depth + 1);
            if (found != -1) return found;
        }
        return -1;
    }

    /** @return the window the entity sits in, itself included, -1 if none */
    private int windowOf(int entity) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            if (windowCM.has(entity)) return entity;
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return -1;
    }

    private boolean isInside(int entity, int ancestor) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            if (entity == ancestor) return true;
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return false;
    }

    private Camera cameraOf(int entity) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            ViewPortComponent viewPort = viewPortCM.get(entity);
            if (viewPort != null) return viewPort.viewPort == null ? null : viewPort.viewPort.getCamera();
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return null;
    }

    private static boolean flag(WidgetComponent widget, String key, boolean fallback) {
        String value = widget == null ? null : widget.properties.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private void setVisible(int entity, boolean visible) {
        MainItemComponent mainItem = mainItemCM.get(entity);
        if (mainItem != null) mainItem.visible = visible;
    }
}
