package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.additional.ButtonComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.widget.ScrollPaneComponent;
import games.rednblack.editor.renderer.components.widget.SelectBoxComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.data.CompositeItemVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.ecs.Aspect;
import games.rednblack.editor.renderer.ecs.BaseEntitySystem;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.input.OverlayListener;
import games.rednblack.editor.renderer.input.UIEvent;
import games.rednblack.editor.renderer.input.UIInputListener;
import games.rednblack.editor.renderer.utils.TransformMathUtils;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * Runs select boxes: a press on the box opens its list over everything else, a press on a row picks
 * that item, a press anywhere else closes it; with the keyboard focus the arrows move through the
 * items, Enter or Space opens the list and Escape closes it.
 *
 * The list's rows are copies of the widget's row part, one per item, made again whenever the items
 * change. They go into the list, or into its content when the list is a scroll pane, stacked from the
 * top; the list is then made as tall as the rows it shows. A row that is a button or a check box has
 * its checked flag set on the item the list points at, so that look is authored like any other state.
 *
 * The list opens below the box, or above it when there is no room below and more above, the way
 * scene2d places it.
 */
@All({SelectBoxComponent.class, WidgetComponent.class})
public class SelectBoxSystem extends BaseEntitySystem implements UIInputListener, OverlayListener {

    private static final float KEY_REPEAT_DELAY = 0.4f;
    private static final float KEY_REPEAT_INTERVAL = 0.05f;
    /** How deep a row may nest before the search for its label gives up. */
    private static final int MAX_DEPTH = 32;

    protected ComponentMapper<SelectBoxComponent> selectBoxCM;
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
    protected ComponentMapper<ScrollPaneComponent> scrollPaneCM;

    protected UIInputSystem inputSystem;
    /** Resolved by the scene loader; without one - in tests - {@link #cloneRow} has to be overridden. */
    protected EntityFactory entityFactory;

    private final Vector2 tmp = new Vector2();

    /**
     * Watches the list on the way down, before a row that is a button takes the press: it only
     * follows the pointer, so the row still looks pressed and a scroll pane can still turn the press
     * into a drag.
     */
    private final UIInputListener rowWatcher = new UIInputListener() {
        @Override
        public void touchDown(UIEvent event) {
            int entity = selectBoxOf(event.listenerEntity);
            SelectBoxComponent selectBox = entity == -1 ? null : selectBoxCM.get(entity);
            if (selectBox == null || !selectBox.isOpen) return;

            int row = rowIndexOf(selectBox, event.target, event.listenerEntity);
            if (row == -1) return;

            selectBox.pressedRow = row;
            selectBox.rowPointer = event.pointer;
            event.followPointer();
            //a button row takes the press itself, to look pressed; a plain one has nobody else to
            if (!buttonCM.has(selectBox.rows.get(row))) event.handle();
        }

        @Override
        public void touchDragged(UIEvent event) {
            int entity = selectBoxOf(event.listenerEntity);
            SelectBoxComponent selectBox = entity == -1 ? null : selectBoxCM.get(entity);
            if (selectBox == null || selectBox.rowPointer != event.pointer) return;

            //the press became a scroll of the list: letting go ends the scroll, it picks nothing
            ScrollPaneComponent pane = scrollPaneCM.get(event.listenerEntity);
            if (pane != null && pane.dragging) selectBox.pressedRow = -1;
        }

        @Override
        public void touchUp(UIEvent event) {
            int entity = selectBoxOf(event.listenerEntity);
            SelectBoxComponent selectBox = entity == -1 ? null : selectBoxCM.get(entity);
            if (selectBox == null || selectBox.rowPointer != event.pointer) return;

            int row = selectBox.pressedRow;
            selectBox.pressedRow = -1;
            selectBox.rowPointer = -1;
            if (event.cancelled || row == -1 || row >= selectBox.rows.size) return;
            if (inputSystem != null && !inputSystem.isOver(selectBox.rows.get(row), event.screenX, event.screenY)) return;

            select(selectBox, row);
        }

        @Override
        public void mouseMoved(UIEvent event) {
            int entity = selectBoxOf(event.listenerEntity);
            SelectBoxComponent selectBox = entity == -1 ? null : selectBoxCM.get(entity);
            if (selectBox == null || !selectBox.isOpen) return;

            int row = rowIndexOf(selectBox, event.target, event.listenerEntity);
            if (row != -1) selectBox.highlighted = row;
        }
    };

    public SelectBoxSystem(Aspect.Builder aspect) {
        super(aspect);
    }

    public SelectBoxSystem() {
    }

    @Override
    protected void inserted(int entityId) {
        InputTargetComponent target = inputTargetCM.create(entityId);
        target.addListener(this);
        target.focusable = true;
    }

    @Override
    protected void removed(int entityId) {
        InputTargetComponent target = inputTargetCM.get(entityId);
        if (target != null) target.removeListener(this);
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
        SelectBoxComponent selectBox = selectBoxCM.get(entity);
        WidgetComponent widget = widgetCM.get(entity);

        //the list hangs below the box, which a composite growing to its children would undo
        CompositeTransformComponent composite = compositeCM.get(entity);
        if (composite != null) composite.automaticResize = false;

        followSettings(widget, selectBox);

        //the composite being viewed from the inside in the editor is opened, not used
        if (viewPortCM.has(entity) || !selectBox.isTouchEnabled || !interactive(entity)) closeSilently(selectBox);

        InputTargetComponent target = inputTargetCM.get(entity);
        if (target != null) target.focusable = selectBox.isTouchEnabled;
        repeatKey(entity, selectBox);

        int list = findPart(entity, WidgetTypes.ROLE_LIST);
        int row = findPart(entity, WidgetTypes.ROLE_ROW);
        int text = findPart(entity, WidgetTypes.ROLE_TEXT);

        updateWidgetState(entity, widget, selectBox);

        rows(entity, selectBox, widget, list, row);
        watchList(list);
        showSelected(text, selectBox);
        placeList(entity, widget, selectBox, list);
        markRows(selectBox);

        notifySelection(entity, selectBox);
    }

    /**
     * Whether the box answers the pointer and the keys at all. The editor keeps the ones it shows
     * closed, until the scene is being tried out.
     */
    protected boolean interactive(int entity) {
        return true;
    }

    /**
     * Whether the rows are made. The editor shows the row part itself, where it was drawn, so it
     * can be designed; the copies only exist while the scene is being tried out.
     */
    protected boolean buildsRows(int entity) {
        return true;
    }

    /** The items and the choice follow their settings when those change, and only then. */
    private void followSettings(WidgetComponent widget, SelectBoxComponent selectBox) {
        String items = widget.properties.get(WidgetTypes.PROPERTY_ITEMS);
        if (items == null ? selectBox.itemsSetting != null : !items.equals(selectBox.itemsSetting)) {
            selectBox.itemsSetting = items;
            SelectBoxComponent.parseItems(items, selectBox.items);
            selectBox.itemsChanged();
        }

        String selected = widget.properties.get(WidgetTypes.PROPERTY_SELECTED);
        if (selected == null ? selectBox.selectedSetting != null : !selected.equals(selectBox.selectedSetting)) {
            selectBox.selectedSetting = selected;
            selectBox.setSelected((int) WidgetComponent.parse(selected, 0));
        }
    }

    /** Picks the most relevant state the widget declares: disabled, open, hover, default. */
    protected void updateWidgetState(int entity, WidgetComponent widget, SelectBoxComponent selectBox) {
        if (!selectBox.isTouchEnabled && widget.setState(WidgetTypes.STATE_DISABLED)) return;
        if (selectBox.isOpen && widget.setState(WidgetTypes.STATE_OPEN)) return;
        if (selectBox.isHovered && widget.setState(WidgetTypes.STATE_HOVER)) return;
        widget.currentState = null; //back to the default state
    }

    // ------------------------------------------------------------------ the rows

    private void rows(int entity, SelectBoxComponent selectBox, WidgetComponent widget, int list, int template) {
        boolean build = buildsRows(entity);
        if (template != -1) setVisible(template, !build);

        if (!build || list == -1 || template == -1) {
            removeRows(selectBox);
            return;
        }

        int parent = rowsParentOf(list);
        if (selectBox.rowsDirty || parent != selectBox.rowsParent || template != selectBox.rowTemplate) {
            removeRows(selectBox);
            for (int i = 0; i < selectBox.items.size; i++) {
                int row = cloneRow(template, parent);
                if (row == -1) break;

                setVisible(row, true);
                //a row that is no button still has to be found under the pointer
                if (!inputTargetCM.has(row)) inputTargetCM.create(row);
                int label = findLabel(row, 0);
                if (label != -1) labelCM.get(label).setText(selectBox.items.get(i));
                selectBox.rows.add(row);
            }
            selectBox.rowsParent = parent;
            selectBox.rowTemplate = template;
            selectBox.rowsDirty = false;
        }

        layoutRows(widget, selectBox, list, parent, template);
    }

    /** Where the rows go: the list, or its content when the list is a scroll pane. */
    private int rowsParentOf(int list) {
        if (!scrollPaneCM.has(list)) return list;
        int content = findPart(list, WidgetTypes.ROLE_CONTENT);
        return content == -1 ? list : content;
    }

    /**
     * Stacks the rows from the top down, and makes the list as tall as the rows it shows: all of
     * them, or as many as the setting allows when the list is a scroll pane, which then scrolls.
     */
    private void layoutRows(WidgetComponent widget, SelectBoxComponent selectBox, int list, int parent, int template) {
        TransformComponent templateTransform = transformCM.get(template);
        DimensionsComponent templateDimensions = dimensionsCM.get(template);
        if (templateTransform == null || templateDimensions == null) return;

        float rowWidth = templateDimensions.width * templateTransform.scaleX;
        float rowHeight = templateDimensions.height * templateTransform.scaleY;
        int count = selectBox.rows.size;

        for (int i = 0; i < count; i++) {
            TransformComponent transform = transformCM.get(selectBox.rows.get(i));
            if (transform == null) continue;
            transform.x = 0;
            transform.y = (count - 1 - i) * rowHeight;
        }

        int shown = count;
        if (parent != list) {
            DimensionsComponent content = dimensionsCM.get(parent);
            if (content != null) {
                content.width = rowWidth;
                content.height = count * rowHeight;
            }
            int max = (int) widget.number(WidgetTypes.PROPERTY_MAX_LIST_COUNT, 0);
            if (max > 0) shown = Math.min(count, max);
        }

        DimensionsComponent listDimensions = dimensionsCM.get(list);
        TransformComponent listTransform = transformCM.get(list);
        if (listDimensions != null && listTransform != null && listTransform.scaleY != 0) {
            listDimensions.height = shown * rowHeight / listTransform.scaleY;
        }

        //the item the list points at stays in sight
        ScrollPaneComponent pane = scrollPaneCM.get(list);
        if (pane != null && selectBox.isOpen && selectBox.highlighted != selectBox.shownRow && selectBox.highlighted >= 0) {
            pane.scrollTo(0, selectBox.highlighted * rowHeight, rowWidth, rowHeight);
            selectBox.shownRow = selectBox.highlighted;
        }
    }

    /**
     * Makes a copy of the row part inside the given parent. The copy is an ordinary item with a new
     * id, playing no role, so it is never mistaken for the part it was copied from.
     *
     * @return the copy, -1 when it could not be made
     */
    protected int cloneRow(int template, int parent) {
        if (entityFactory == null) return -1;

        MainItemComponent mainItem = mainItemCM.get(template);
        if (mainItem == null) return -1;

        try {
            MainItemVO vo = entityFactory.instantiateEmptyVO(mainItem.entityType);
            vo.loadFromEntity(template, engine, entityFactory);
            forgetIds(vo);
            if (vo.widgetPart != null) vo.widgetPart.role = "";

            int row = entityFactory.createEntity(parent, vo);
            if (vo instanceof CompositeItemVO) entityFactory.initAllChildren(row, (CompositeItemVO) vo);
            return row;
        } catch (ReflectionException e) {
            return -1;
        }
    }

    /** Clears the ids of an item and of everything in it, so the copies are given their own. */
    private static void forgetIds(MainItemVO vo) {
        vo.uniqueId = null;
        if (!(vo instanceof CompositeItemVO)) return;

        for (ObjectMap.Entry<String, Array<MainItemVO>> entry : ((CompositeItemVO) vo).content) {
            for (MainItemVO child : entry.value) forgetIds(child);
        }
    }

    /** Takes the rows out of the list at once, so new ones never show beside them, then deletes them. */
    private void removeRows(SelectBoxComponent selectBox) {
        for (int i = 0; i < selectBox.rows.size; i++) {
            int row = selectBox.rows.get(i);
            ParentNodeComponent parent = parentCM.get(row);
            NodeComponent node = parent == null || parent.parentEntity == -1 ? null : nodeCM.get(parent.parentEntity);
            if (node != null) node.removeChild(row);
            engine.delete(row);
        }
        selectBox.rows.clear();
        selectBox.rowsParent = -1;
        selectBox.rowTemplate = -1;
        selectBox.rowsDirty = true;
        selectBox.shownRow = -1;
    }

    /** @return the first label in the row, the row itself included, -1 if it has none */
    private int findLabel(int entity, int depth) {
        if (depth > MAX_DEPTH) return -1;
        if (labelCM.has(entity)) return entity;

        NodeComponent node = nodeCM.get(entity);
        if (node == null) return -1;
        for (int i = 0; i < node.children.size; i++) {
            int label = findLabel(node.children.get(i), depth + 1);
            if (label != -1) return label;
        }
        return -1;
    }

    /** A row that is a button or a check box shows as checked on the item the list points at. */
    private void markRows(SelectBoxComponent selectBox) {
        int marked = selectBox.isOpen ? selectBox.highlighted : selectBox.selected;
        for (int i = 0; i < selectBox.rows.size; i++) {
            ButtonComponent button = buttonCM.get(selectBox.rows.get(i));
            if (button != null) button.isChecked = i == marked;
        }
    }

    // ------------------------------------------------------------------ the box and the list

    /** Writes the chosen item into the box's label, only when it differs, so nothing is laid out for nothing. */
    private void showSelected(int text, SelectBoxComponent selectBox) {
        LabelComponent label = text == -1 ? null : labelCM.get(text);
        if (label == null) return;

        String selected = selectBox.getSelected();
        if (selected == null) selected = "";
        if (!sameText(label.getText(), selected)) label.setText(selected);
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
     * Shows the list in the open state only, drawn and hit in front of everything while it shows,
     * and hangs it from the box: below it, or above when there is no room below and more above.
     */
    private void placeList(int entity, WidgetComponent widget, SelectBoxComponent selectBox, int list) {
        if (list == -1) return;

        boolean open = WidgetTypes.STATE_OPEN.equals(widget.getState());
        setVisible(list, open);

        if (inputSystem != null) {
            boolean overlay = inputSystem.isOverlayOpen(list);
            if (open && !overlay) inputSystem.openOverlay(list, false, this);
            else if (!open && overlay) inputSystem.closeOverlay(list);
        }
        if (!open) return;

        TransformComponent listTransform = transformCM.get(list);
        DimensionsComponent listDimensions = dimensionsCM.get(list);
        DimensionsComponent boxDimensions = dimensionsCM.get(entity);
        if (listTransform == null || listDimensions == null || boxDimensions == null) return;

        float listHeight = listDimensions.height * listTransform.scaleY;
        listTransform.x = 0;
        listTransform.y = opensAbove(entity, boxDimensions.height, listHeight) ? boxDimensions.height : -listHeight;
    }

    /** Whether the list fits below the box on screen, and if not, whether there is more room above. */
    private boolean opensAbove(int entity, float boxHeight, float listHeight) {
        Camera camera = cameraOf(entity);
        if (camera == null) return false;

        float zoom = camera instanceof OrthographicCamera ? ((OrthographicCamera) camera).zoom : 1;
        float bottom = camera.position.y - camera.viewportHeight * zoom / 2;
        float top = camera.position.y + camera.viewportHeight * zoom / 2;

        TransformMathUtils.localToSceneCoordinates(entity, tmp.set(0, 0), transformCM, parentCM);
        float below = tmp.y - bottom;
        if (below >= listHeight) return false;

        TransformMathUtils.localToSceneCoordinates(entity, tmp.set(0, boxHeight), transformCM, parentCM);
        float above = top - tmp.y;
        return above > below;
    }

    private Camera cameraOf(int entity) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH * 4; depth++) {
            ViewPortComponent viewPort = viewPortCM.get(entity);
            if (viewPort != null) return viewPort.viewPort == null ? null : viewPort.viewPort.getCamera();
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return null;
    }

    /** The list listens on the way down, so it knows which row a press or the pointer is on. */
    private void watchList(int list) {
        if (list == -1) return;
        InputTargetComponent target = inputTargetCM.get(list);
        if (target == null) target = inputTargetCM.create(list);
        target.addCaptureListener(rowWatcher);
    }

    // ------------------------------------------------------------------ choosing

    private void open(SelectBoxComponent selectBox) {
        if (selectBox.items.size == 0) return;
        selectBox.isOpen = true;
        selectBox.highlighted = selectBox.selected;
        selectBox.shownRow = -1;
    }

    private void close(SelectBoxComponent selectBox) {
        selectBox.isOpen = false;
        selectBox.highlighted = selectBox.selected;
        selectBox.pressedRow = -1;
        selectBox.rowPointer = -1;
    }

    private void select(SelectBoxComponent selectBox, int index) {
        selectBox.setSelected(index);
        close(selectBox);
    }

    /** Closes the list and forgets any key or pointer, without a word to the listeners. */
    private void closeSilently(SelectBoxComponent selectBox) {
        close(selectBox);
        selectBox.isHovered = false;
        selectBox.repeatKey = -1;
    }

    /** Tells the listeners once per change, whoever made it. */
    private void notifySelection(int entity, SelectBoxComponent selectBox) {
        if (!selectBox.notified) {
            selectBox.notified = true;
            selectBox.notifiedIndex = selectBox.selected;
            return;
        }
        if (selectBox.selected == selectBox.notifiedIndex) return;

        selectBox.notifiedIndex = selectBox.selected;
        String item = selectBox.getSelected();
        for (int i = 0; i < selectBox.listeners.size; i++) {
            selectBox.listeners.get(i).selectionChanged(entity, selectBox.selected, item);
        }
    }

    // ------------------------------------------------------------------ the pointer on the box

    @Override
    public void touchDown(UIEvent event) {
        int entity = event.listenerEntity;
        SelectBoxComponent selectBox = selectBoxCM.get(entity);
        if (selectBox == null || !selectBox.isTouchEnabled || !interactive(entity)) return;
        if (viewPortCM.has(entity)) return;

        //the rows are the list's business
        int list = findPart(entity, WidgetTypes.ROLE_LIST);
        if (list != -1 && isInside(event.target, list)) return;

        if (selectBox.isOpen) close(selectBox);
        else open(selectBox);
        event.handle();
    }

    @Override
    public void enter(UIEvent event) {
        SelectBoxComponent selectBox = selectBoxCM.get(event.listenerEntity);
        if (selectBox != null) selectBox.isHovered = true;
    }

    @Override
    public void exit(UIEvent event) {
        SelectBoxComponent selectBox = selectBoxCM.get(event.listenerEntity);
        if (selectBox != null) selectBox.isHovered = false;
    }

    /** A press missed the open list: it closes, unless the press was on its own box, which toggles it. */
    @Override
    public void touchedOutside(int overlay, UIEvent event) {
        int entity = selectBoxOf(overlay);
        SelectBoxComponent selectBox = entity == -1 ? null : selectBoxCM.get(entity);
        if (selectBox == null) return;

        if (event.relatedEntity != -1 && isInside(event.relatedEntity, entity)) return;
        close(selectBox);
    }

    // ------------------------------------------------------------------ the keys

    @Override
    public void keyDown(UIEvent event) {
        int entity = event.listenerEntity;
        if (event.target != entity) return;
        SelectBoxComponent selectBox = selectBoxCM.get(entity);
        if (selectBox == null || !selectBox.isTouchEnabled || !interactive(entity)) return;

        if (!apply(selectBox, event.keyCode)) return;
        event.handle();

        //the desktop backend says a held key once, so the box keeps its own clock
        if (repeats(event.keyCode)) {
            selectBox.repeatKey = event.keyCode;
            selectBox.repeatIn = KEY_REPEAT_DELAY;
        }
    }

    @Override
    public void keyUp(UIEvent event) {
        SelectBoxComponent selectBox = selectBoxCM.get(event.listenerEntity);
        if (selectBox != null && selectBox.repeatKey == event.keyCode) selectBox.repeatKey = -1;
    }

    /**
     * Closed, the arrows choose the item before or after; open, they move through the list and
     * Enter takes the item. Tab closes an open list and goes on to move the focus.
     *
     * @return true when the key meant something here
     */
    private boolean apply(SelectBoxComponent selectBox, int keyCode) {
        switch (keyCode) {
            case Input.Keys.UP:
                return step(selectBox, -1);
            case Input.Keys.DOWN:
                return step(selectBox, 1);
            case Input.Keys.ENTER:
            case Input.Keys.NUMPAD_ENTER:
            case Input.Keys.SPACE:
                if (!selectBox.isOpen) {
                    open(selectBox);
                    return selectBox.isOpen;
                }
                if (selectBox.highlighted >= 0) select(selectBox, selectBox.highlighted);
                else close(selectBox);
                return true;
            case Input.Keys.ESCAPE:
                if (!selectBox.isOpen) return false;
                close(selectBox);
                return true;
            case Input.Keys.TAB:
                if (selectBox.isOpen) close(selectBox);
                return false;
            default:
                return false;
        }
    }

    private boolean step(SelectBoxComponent selectBox, int direction) {
        if (selectBox.items.size == 0) return false;
        int last = selectBox.items.size - 1;

        if (selectBox.isOpen) {
            int from = selectBox.highlighted < 0 ? (direction > 0 ? -1 : selectBox.items.size) : selectBox.highlighted;
            selectBox.highlighted = MathUtils.clamp(from + direction, 0, last);
        } else {
            int from = selectBox.selected < 0 ? (direction > 0 ? -1 : selectBox.items.size) : selectBox.selected;
            selectBox.setSelected(MathUtils.clamp(from + direction, 0, last));
        }
        return true;
    }

    private static boolean repeats(int keyCode) {
        return keyCode == Input.Keys.UP || keyCode == Input.Keys.DOWN;
    }

    /** Says the held key again, as long as it is held and the box still has the keyboard. */
    private void repeatKey(int entity, SelectBoxComponent selectBox) {
        if (selectBox.repeatKey == -1) return;
        boolean focused = inputSystem != null && inputSystem.getKeyboardFocus() == entity;
        if (!focused || !selectBox.isTouchEnabled) {
            selectBox.repeatKey = -1;
            return;
        }

        selectBox.repeatIn -= engine.getDelta();
        //a frame that took a long while says the key a few times, not a hundred
        for (int i = 0; i < 10 && selectBox.repeatIn <= 0; i++) {
            selectBox.repeatIn += KEY_REPEAT_INTERVAL;
            apply(selectBox, selectBox.repeatKey);
        }
        if (selectBox.repeatIn <= 0) selectBox.repeatIn = KEY_REPEAT_INTERVAL;
    }

    // ------------------------------------------------------------------ the tree

    /** @return the direct child playing that role, -1 if none does */
    protected int findPart(int widget, String role) {
        NodeComponent node = nodeCM.get(widget);
        if (node == null) return -1;

        for (int i = 0; i < node.children.size; i++) {
            int child = node.children.get(i);
            WidgetPartComponent part = partCM.get(child);
            if (part != null && role.equals(part.role)) return child;
        }
        return -1;
    }

    /** @return the select box the entity sits in, -1 if none */
    private int selectBoxOf(int entity) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            if (selectBoxCM.has(entity)) return entity;
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return -1;
    }

    /** @return which row the entity is, or sits in, looking no further up than the list; -1 if none */
    private int rowIndexOf(SelectBoxComponent selectBox, int entity, int list) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH; depth++) {
            int index = selectBox.rows.indexOf(entity);
            if (index != -1) return index;
            if (entity == list) return -1;
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return -1;
    }

    private boolean isInside(int entity, int ancestor) {
        for (int depth = 0; entity != -1 && depth < MAX_DEPTH * 4; depth++) {
            if (entity == ancestor) return true;
            ParentNodeComponent parent = parentCM.get(entity);
            entity = parent == null ? -1 : parent.parentEntity;
        }
        return false;
    }

    private void setVisible(int entity, boolean visible) {
        MainItemComponent mainItem = mainItemCM.get(entity);
        if (mainItem != null) mainItem.visible = visible;
    }
}
