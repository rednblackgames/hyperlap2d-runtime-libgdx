package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.widget.TooltipComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.input.OverlayListener;
import games.rednblack.editor.renderer.input.Touchable;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.utils.TransformMathUtils;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * Runs tooltips: one opened on an entity is drawn over everything else, above or below that entity
 * and following it as it moves, with its pointer turned to it; a press anywhere else closes it and
 * goes on to what it landed on, so a tap on something else is never lost.
 *
 * The pointer is drawn once, on the side of the box the tooltip would open on, and is turned to the
 * other side when there is no room there. Its height goes in the gap between the box and the entity.
 *
 * A press on the tooltip itself stays there, so nothing behind it is reached through it. A press on
 * the entity it points at is left alone: that is the game's to answer, with {@link TooltipComponent#showAt}
 * closing a tooltip already open on it.
 */
@All({TooltipComponent.class, WidgetComponent.class})
public class TooltipSystem extends BaseEntitySystem implements UIInputListener, OverlayListener {

    private static final int MAX_DEPTH = 128;

    protected ComponentMapper<TooltipComponent> tooltipCM;
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

    protected UIInputSystem inputSystem;

    private final Vector2 tmp = new Vector2();
    private final Vector2 tmp2 = new Vector2();

    public TooltipSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public TooltipSystem() {
    }

    @Override
    protected void inserted(int entityId) {
        inputTargetCM.create(entityId).addListener(this);
    }

    @Override
    protected void removed(int entityId) {
        InputTargetComponent target = inputTargetCM.get(entityId);
        if (target != null) target.removeListener(this);
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
        TooltipComponent tooltip = tooltipCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);

        //the pointer reaches out of the box, which a composite growing to its children would follow
        CompositeTransformComponent composite = compositeCM.get(entity);
        if (composite != null) composite.automaticResize = false;

        followSettings(widget, tooltip);

        boolean live = interactive(entity);
        if (live && tooltip.isOpen && tooltip.target != -1 && !present(tooltip.target)) tooltip.isOpen = false;

        //a tooltip that starts closed is closed at once, never animated
        boolean startsClosed = !tooltip.notified && !tooltip.isOpen;
        if (startsClosed) snapParts(entity, 0);

        //outside of the game it is something being designed and always shows, where it was drawn
        boolean shown = !live || tooltip.isOpen || !startsClosed && !closedLookPlayed(entity, widget);
        setVisible(entity, shown);
        InputTargetComponent target = inputTargetCM.get(entity);
        if (target != null) target.touchable = live && tooltip.isOpen ? Touchable.ENABLED : Touchable.DISABLED;

        followOverlay(entity, live && shown);
        int pointer = findPart(entity, WidgetTypes.ROLE_POINTER);
        readPointer(tooltip, pointer, entity);
        if (live && tooltip.isOpen && tooltip.target != -1) {
            place(entity, widget, tooltip);
            aimPointer(entity, tooltip, pointer);
        }

        updateWidgetState(entity, widget, tooltip);
        notifyListeners(entity, tooltip, shown);
    }

    /** Whether it opens, closes and places itself. The editor keeps the ones it shows where they were drawn. */
    protected boolean interactive(int entity) {
        return true;
    }

    private void followSettings(WidgetComponent widget, TooltipComponent tooltip) {
        String open = widget.properties.get(WidgetTypes.PROPERTY_OPEN);
        if (open == null ? tooltip.openSetting != null : !open.equals(tooltip.openSetting)) {
            tooltip.openSetting = open;
            tooltip.isOpen = open != null && Boolean.parseBoolean(open);
        }
    }

    protected void updateWidgetState(int entity, WidgetComponent widget, TooltipComponent tooltip) {
        if (!tooltip.isOpen && interactive(entity) && widget.setState(WidgetTypes.STATE_CLOSED)) return;
        widget.currentState = null; //back to the default state
    }

    /** Open, or still closing: drawn over everything and told of the presses that miss it. */
    private void followOverlay(int entity, boolean wanted) {
        if (inputSystem == null) return;
        boolean open = inputSystem.isOverlayOpen(entity);
        if (wanted && !open) inputSystem.openOverlay(entity, false, this);
        else if (!wanted && open) inputSystem.closeOverlay(entity);
    }

    // ------------------------------------------------------------------ placing it

    /**
     * Above or below the target, as the placement setting says, the box centred on it and pushed
     * sideways to stay a margin inside what the camera sees. Worked out in the tooltip's parent, so
     * it follows the target wherever the two sit in the tree.
     */
    private void place(int entity, WidgetComponent widget, TooltipComponent tooltip) {
        TransformComponent transform = transformCM.get(entity);
        DimensionsComponent dimensions = dimensionsCM.get(entity);
        DimensionsComponent targetSize = dimensionsCM.get(tooltip.target);
        if (transform == null || dimensions == null || targetSize == null) return;

        ParentNodeComponent parentNode = parentCM.get(entity);
        int parent = parentNode == null ? -1 : parentNode.parentEntity;

        //the target's box, in the parent
        toParent(tooltip.target, parent, tmp.set(0, 0));
        float ax = tmp.x, ay = tmp.y;
        toParent(tooltip.target, parent, tmp.set(targetSize.width, targetSize.height));
        float targetLeft = Math.min(ax, tmp.x), targetRight = Math.max(ax, tmp.x);
        float targetBottom = Math.min(ay, tmp.y), targetTop = Math.max(ay, tmp.y);

        float width = dimensions.width * Math.abs(transform.scaleX);
        float height = dimensions.height * Math.abs(transform.scaleY);
        float gap = number(widget, WidgetTypes.PROPERTY_GAP, 0);
        float margin = number(widget, WidgetTypes.PROPERTY_MARGIN, 0);

        boolean hasView = viewInParent(entity, parent);
        float viewLeft = tmp.x, viewBottom = tmp.y, viewRight = tmp2.x, viewTop = tmp2.y;

        String placement = widget.properties.get(WidgetTypes.PROPERTY_PLACEMENT, WidgetTypes.PLACEMENT_AUTO);
        boolean below;
        if (WidgetTypes.PLACEMENT_BELOW.equals(placement)) {
            below = true;
        } else if (WidgetTypes.PLACEMENT_ABOVE.equals(placement) || !hasView) {
            below = false;
        } else {
            //above when it fits there, below when only that fits, above when neither does
            boolean fitsAbove = targetTop + gap + height <= viewTop - margin;
            boolean fitsBelow = targetBottom - gap - height >= viewBottom + margin;
            below = !fitsAbove && fitsBelow;
        }
        tooltip.below = below;

        float left = (targetLeft + targetRight) / 2 - width / 2;
        if (hasView && flag(widget, WidgetTypes.PROPERTY_KEEP_WITHIN_VIEWPORT, true)) {
            //one too wide for the view keeps its left edge in sight
            left = Math.min(left, viewRight - margin - width);
            left = Math.max(left, viewLeft + margin);
        }
        float bottom = below ? targetBottom - gap - height : targetTop + gap;

        //the box's corner is where the transform puts it once scaled about its origin
        float originX = originX(transform), originY = originY(transform);
        transform.x = left - originX + originX * Math.abs(transform.scaleX);
        transform.y = bottom - originY + originY * Math.abs(transform.scaleY);
    }

    /** Puts what the camera sees, in the parent, in tmp (bottom left) and tmp2 (top right); false without a camera. */
    private boolean viewInParent(int entity, int parent) {
        Camera camera = cameraOf(entity);
        if (camera == null) return false;

        float zoom = camera instanceof OrthographicCamera ? ((OrthographicCamera) camera).zoom : 1;
        float halfWidth = camera.viewportWidth * zoom / 2, halfHeight = camera.viewportHeight * zoom / 2;
        tmp.set(camera.position.x - halfWidth, camera.position.y - halfHeight);
        tmp2.set(camera.position.x + halfWidth, camera.position.y + halfHeight);
        if (parent != -1) {
            TransformMathUtils.sceneToLocalCoordinates(parent, tmp, transformCM, parentCM);
            TransformMathUtils.sceneToLocalCoordinates(parent, tmp2, transformCM, parentCM);
        }
        float minX = Math.min(tmp.x, tmp2.x), maxX = Math.max(tmp.x, tmp2.x);
        float minY = Math.min(tmp.y, tmp2.y), maxY = Math.max(tmp.y, tmp2.y);
        tmp.set(minX, minY);
        tmp2.set(maxX, maxY);
        return true;
    }

    private void toParent(int entity, int parent, Vector2 point) {
        TransformMathUtils.localToSceneCoordinates(entity, point, transformCM, parentCM);
        if (parent != -1) TransformMathUtils.sceneToLocalCoordinates(parent, point, transformCM, parentCM);
    }

    // ------------------------------------------------------------------ the pointer

    /** Notes once how the pointer was drawn: that is the side it belongs to, and the look it is turned from. */
    private void readPointer(TooltipComponent tooltip, int pointer, int entity) {
        if (tooltip.pointerRead || pointer == -1) return;
        TransformComponent transform = transformCM.get(pointer);
        DimensionsComponent size = dimensionsCM.get(pointer);
        DimensionsComponent box = dimensionsCM.get(entity);
        if (transform == null || size == null || box == null) return;

        tooltip.pointerRead = true;
        tooltip.pointerY = transform.y;
        tooltip.pointerScaleY = transform.scaleY;
        tooltip.pointerDrawnBelow = centreY(transform.y, transform, size) < box.height / 2;
    }

    /**
     * Under the target's centre, kept on the box, and on the side the target is: drawn under the box,
     * it is turned over to the top of it for a tooltip that opened below its target.
     */
    private void aimPointer(int entity, TooltipComponent tooltip, int pointer) {
        if (pointer == -1 || !tooltip.pointerRead) return;
        TransformComponent transform = transformCM.get(pointer);
        DimensionsComponent size = dimensionsCM.get(pointer);
        TransformComponent box = transformCM.get(entity);
        DimensionsComponent boxSize = dimensionsCM.get(entity);
        DimensionsComponent targetSize = dimensionsCM.get(tooltip.target);
        if (transform == null || size == null || box == null || boxSize == null || targetSize == null) return;

        //the target's centre, in the tooltip
        TransformMathUtils.localToSceneCoordinates(tooltip.target, tmp.set(targetSize.width / 2, targetSize.height / 2), transformCM, parentCM);
        TransformMathUtils.sceneToLocalCoordinates(entity, tmp, transformCM, parentCM);

        float halfWidth = size.width * Math.abs(transform.scaleX) / 2;
        float centreX = boxSize.width < 2 * halfWidth ? boxSize.width / 2 : MathUtils.clamp(tmp.x, halfWidth, boxSize.width - halfWidth);
        float originX = originX(transform), originY = originY(transform);
        transform.x = centreX - originX - (size.width / 2 - originX) * transform.scaleX;

        //below the box when the tooltip is above its target, above it when below
        boolean turned = tooltip.below == tooltip.pointerDrawnBelow;
        transform.scaleY = turned ? -tooltip.pointerScaleY : tooltip.pointerScaleY;
        float drawnCentre = centreY(tooltip.pointerY, tooltip.pointerScaleY, originY, size.height);
        float centreY = turned ? boxSize.height - drawnCentre : drawnCentre;
        transform.y = centreY - originY - (size.height / 2 - originY) * transform.scaleY;
    }

    private static float centreY(float y, TransformComponent transform, DimensionsComponent size) {
        return centreY(y, transform.scaleY, originY(transform), size.height);
    }

    /** An origin never set is the corner, as the bounds see it; the loader sets it to the centre. */
    private static float originX(TransformComponent transform) {
        return Float.isNaN(transform.originX) ? 0 : transform.originX;
    }

    private static float originY(TransformComponent transform) {
        return Float.isNaN(transform.originY) ? 0 : transform.originY;
    }

    /** The middle of a part, in its parent, scaled about its origin. */
    private static float centreY(float y, float scaleY, float originY, float height) {
        return y + originY + (height / 2 - originY) * scaleY;
    }

    // ------------------------------------------------------------------ the pointer of the player

    /** A press on the tooltip is taken, so it reaches nothing behind it. */
    @Override
    public void touchDown(UIEvent event) {
        TooltipComponent tooltip = tooltipCM.get(event.listenerEntity);
        if (tooltip == null || !tooltip.isOpen || !interactive(event.listenerEntity)) return;
        event.handle();
    }

    /** A press elsewhere closes it, unless it lands on its target, which answers it on its own. */
    @Override
    public void touchedOutside(int overlay, UIEvent event) {
        TooltipComponent tooltip = tooltipCM.get(overlay);
        if (tooltip == null || !tooltip.isOpen || !interactive(overlay)) return;
        if (!flag(widgetCM.get(overlay), WidgetTypes.PROPERTY_CLOSE_ON_TOUCH_OUTSIDE, true)) return;
        if (tooltip.target != -1 && event.relatedEntity != -1 && isInside(event.relatedEntity, tooltip.target)) return;
        tooltip.hide();
    }

    // ------------------------------------------------------------------ listeners

    /** Tells the listeners once per change, whoever made it; a closed tooltip lets go of the pointer. */
    private void notifyListeners(int entity, TooltipComponent tooltip, boolean shown) {
        if (!tooltip.notified) {
            tooltip.notified = true;
            tooltip.notifiedOpen = tooltip.isOpen;
            tooltip.notifiedShown = shown;
            tooltip.notifiedTarget = tooltip.target;
            return;
        }

        boolean opened = tooltip.isOpen && (!tooltip.notifiedOpen || tooltip.target != tooltip.notifiedTarget);
        boolean closed = !tooltip.isOpen && tooltip.notifiedOpen;
        tooltip.notifiedOpen = tooltip.isOpen;
        tooltip.notifiedTarget = tooltip.target;
        if (closed && inputSystem != null) inputSystem.cancelTouchFocus(entity);
        for (int i = 0; i < tooltip.listeners.size; i++) {
            if (opened) tooltip.listeners.get(i).opened(entity, tooltip.target);
            if (closed) tooltip.listeners.get(i).closed(entity);
        }

        //after closed, once the closed look has played: shown again only by opening
        if (shown != tooltip.notifiedShown) {
            tooltip.notifiedShown = shown;
            if (!shown) {
                for (int i = 0; i < tooltip.listeners.size; i++) tooltip.listeners.get(i).hidden(entity);
            }
        }
    }

    // ------------------------------------------------------------------ the closed look

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

    /** Whether the entity still exists and shows, with everything it sits in. */
    private boolean present(int entity) {
        if (!mainItemCM.has(entity)) return false;
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            MainItemComponent mainItem = mainItemCM.get(entity);
            if (mainItem != null && !mainItem.visible) return false;
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return true;
    }

    /** @return the part playing that role in the tooltip, leaving out what a nested widget holds; -1 if none */
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

    private static float number(WidgetComponent widget, String key, float fallback) {
        String value = widget == null ? null : widget.properties.get(key);
        if (value == null) return fallback;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void setVisible(int entity, boolean visible) {
        MainItemComponent mainItem = mainItemCM.get(entity);
        if (mainItem != null) mainItem.visible = visible;
    }
}
