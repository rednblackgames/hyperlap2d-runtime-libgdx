package games.rednblack.editor.renderer.components.widget;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.Transient;
import games.rednblack.editor.renderer.widget.WidgetBehaviour;
import games.rednblack.editor.renderer.widget.WidgetTypes;

/**
 * What a select box holds: its items, the one chosen, and whether its list is open.
 *
 * The items and the choice start from the widget's settings and follow them when they change;
 * game code sets them here directly. The rows of the list are copies of the widget's row part,
 * made by the system whenever the items change.
 */
@Transient
public class SelectBoxComponent extends PooledComponent implements WidgetBehaviour {

    public final Array<String> items = new Array<>(true, 4, String[]::new);
    /** The item shown in the box, -1 for none. */
    public int selected = -1;
    /** The item the list points at while open: the pointer over it, or where the arrow keys took it. */
    public int highlighted = -1;
    public boolean isOpen = false;

    public boolean isTouchEnabled = true;
    public boolean isHovered = false;

    /** The settings last followed, so a change of setting is followed but a choice is not undone. */
    public String itemsSetting = null;
    public String selectedSetting = null;
    /** Set when the items changed and the rows have to be made again. */
    public boolean rowsDirty = true;

    /** The copies of the row part, one per item, in the order of the items. */
    public final IntArray rows = new IntArray(4);
    /** Where the rows were put and what they were copied from, to notice either changing. */
    public int rowsParent = -1;
    public int rowTemplate = -1;
    /** The row a scroll pane list was last scrolled to, so it is scrolled only when that changes. */
    public int shownRow = -1;

    /** The row a pointer pressed in the list and the pointer, -1 if none. */
    public int pressedRow = -1;
    public int rowPointer = -1;

    /** The arrow key being held, -1 for none, and the time left before it moves again. */
    public int repeatKey = -1;
    public float repeatIn = 0;

    /** Last choice the listeners were told about, and whether they have been told at all. */
    public int notifiedIndex = -1;
    public boolean notified = false;

    public final Array<SelectBoxListener> listeners = new Array<>(true, 1, SelectBoxListener[]::new);

    public interface SelectBoxListener {
        /**
         * The box shows another item, whoever chose it: the pointer, the keys, the settings or game code.
         *
         * @param item the item chosen, null when none is
         */
        void selectionChanged(int entity, int index, String item);
    }

    @Override
    public void initialise(WidgetComponent widget) {
        itemsSetting = widget.properties.get(WidgetTypes.PROPERTY_ITEMS);
        parseItems(itemsSetting, items);
        selectedSetting = widget.properties.get(WidgetTypes.PROPERTY_SELECTED);
        setSelected((int) WidgetComponent.parse(selectedSetting, 0));
        notifiedIndex = selected;
        notified = true;
    }

    /** Splits a list setting into its items, one per line; blank lines are left out. */
    public static void parseItems(String setting, Array<String> out) {
        out.clear();
        if (setting == null) return;

        int start = 0;
        for (int i = 0; i <= setting.length(); i++) {
            if (i < setting.length() && setting.charAt(i) != '\n') continue;

            int end = i;
            if (end > start && setting.charAt(end - 1) == '\r') end--;
            if (end > start) {
                String item = setting.substring(start, end);
                if (!item.trim().isEmpty()) out.add(item);
            }
            start = i + 1;
        }
    }

    public void setItems(String... newItems) {
        items.clear();
        items.addAll(newItems);
        itemsChanged();
    }

    public void setItems(Array<String> newItems) {
        items.clear();
        items.addAll(newItems);
        itemsChanged();
    }

    /** Keeps the choice where it can after the items changed, and has the rows made again. */
    public void itemsChanged() {
        rowsDirty = true;
        if (items.size == 0) selected = -1;
        else if (selected < 0) selected = 0;
        else if (selected >= items.size) selected = items.size - 1;
        highlighted = selected;
    }

    /** @param index kept within the items; -1, or anything with no items, chooses none */
    public void setSelected(int index) {
        if (items.size == 0 || index < 0) selected = -1;
        else selected = Math.min(index, items.size - 1);
        highlighted = selected;
    }

    /** Chooses the first item equal to the given one, and nothing when there is none. */
    public void setSelected(String item) {
        setSelected(items.indexOf(item, false));
    }

    /** @return the item shown in the box, null for none */
    public String getSelected() {
        return selected < 0 || selected >= items.size ? null : items.get(selected);
    }

    public void addListener(SelectBoxListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SelectBoxListener listener) {
        listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public void reset() {
        items.clear();
        selected = -1;
        highlighted = -1;
        isOpen = false;
        isTouchEnabled = true;
        isHovered = false;
        itemsSetting = null;
        selectedSetting = null;
        rowsDirty = true;
        rows.clear();
        rowsParent = -1;
        rowTemplate = -1;
        shownRow = -1;
        pressedRow = -1;
        rowPointer = -1;
        repeatKey = -1;
        repeatIn = 0;
        notifiedIndex = -1;
        notified = false;
        listeners.clear();
    }
}
