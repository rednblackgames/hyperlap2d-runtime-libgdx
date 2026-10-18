package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.AutoLayoutComponent.Align;

/**
 * The arithmetic of an auto layout, apart from the entities: given the children as rectangles, it
 * says how large they make the composite and where each one goes.
 *
 * Everything is in the composite's own coordinates, y up; a child's rectangle is what it takes in
 * its parent, its footprint, whatever its rotation and scale.
 */
public final class AutoLayoutSolver {

    private AutoLayoutSolver() {
    }

    /** A child as the layout sees it. */
    public static class Item {
        /** Its footprint as it is. */
        public float width, height;
        /**
         * What it needs on an axis the layout sizes (it grows, or it is stretched), since its size
         * there is the layout's doing: its own measure when it is an auto layout fitting that axis,
         * else the size of its own the layout keeps for it.
         */
        public float contentWidth, contentHeight;
        public float grow;
        /** null for the layout's own alignment. */
        public Align alignSelf;

        /** Where the layout puts it, and the size it gives it. */
        public float x, y, outWidth, outHeight;

        /** Working length along a row or a column. */
        float main;

        public Item set(float width, float height) {
            this.width = width;
            this.height = height;
            contentWidth = contentHeight = 0;
            grow = 0;
            alignSelf = null;
            return this;
        }
    }

    /** @return out, the size of the children plus padding */
    public static Vector2 measure(AutoLayoutComponent layout, Array<Item> items, Vector2 out) {
        float padX = layout.padLeft + layout.padRight;
        float padY = layout.padTop + layout.padBottom;
        int n = items.size;
        if (n == 0) return out.set(padX, padY);

        if (layout.mode == AutoLayoutComponent.Mode.GRID) {
            float cellW = cellWidth(layout, items);
            float cellH = cellHeight(layout, items);
            int columns = usedColumns(layout, n);
            int rows = rows(layout, n);
            return out.set(padX + columns * cellW + (columns - 1) * layout.gap,
                    padY + rows * cellH + (rows - 1) * layout.rowGap);
        }

        boolean row = layout.mode == AutoLayoutComponent.Mode.ROW;
        float main = (n - 1) * layout.gap;
        float cross = 0;
        for (int i = 0; i < n; i++) {
            Item item = items.get(i);
            main += basis(item, row);
            cross = Math.max(cross, crossNeed(layout, item, row));
        }
        return row ? out.set(padX + main, padY + cross) : out.set(padX + cross, padY + main);
    }

    /** Places the children in a composite of the given size. */
    public static void arrange(AutoLayoutComponent layout, float width, float height, Array<Item> items) {
        if (items.size == 0) return;
        if (layout.mode == AutoLayoutComponent.Mode.GRID) arrangeGrid(layout, width, height, items);
        else arrangeLinear(layout, width, height, items);
    }

    private static void arrangeLinear(AutoLayoutComponent layout, float width, float height, Array<Item> items) {
        boolean row = layout.mode == AutoLayoutComponent.Mode.ROW;
        float mainRoom = row ? width - layout.padLeft - layout.padRight : height - layout.padTop - layout.padBottom;
        float crossRoom = row ? height - layout.padTop - layout.padBottom : width - layout.padLeft - layout.padRight;
        int n = items.size;

        float used = (n - 1) * layout.gap;
        float totalGrow = 0;
        for (int i = 0; i < n; i++) {
            Item item = items.get(i);
            item.main = basis(item, row);
            used += item.main;
            if (item.grow > 0) totalGrow += item.grow;
        }

        float free = mainRoom - used;
        if (totalGrow > 0 && free > 0) {
            for (int i = 0; i < n; i++) {
                Item item = items.get(i);
                if (item.grow > 0) item.main += free * item.grow / totalGrow;
            }
            free = 0;
        }

        float cursor = 0;
        float between = layout.gap;
        switch (layout.justify) {
            case CENTER:
                cursor = free / 2;
                break;
            case END:
                cursor = free;
                break;
            case SPACE_BETWEEN:
                if (n > 1 && free > 0) between += free / (n - 1);
                break;
            default:
                break;
        }

        for (int k = 0; k < n; k++) {
            Item item = items.get(layout.reverse ? n - 1 - k : k);

            Align align = item.alignSelf != null ? item.alignSelf : layout.align;
            float cross = row ? item.height : item.width;
            float offset = 0;
            switch (align) {
                case STRETCH:
                    cross = Math.max(0, crossRoom);
                    break;
                case CENTER:
                    offset = (crossRoom - cross) / 2;
                    break;
                case END:
                    offset = crossRoom - cross;
                    break;
                default:
                    break;
            }

            if (row) {
                item.x = layout.padLeft + cursor;
                item.outWidth = item.main;
                item.outHeight = cross;
                item.y = height - layout.padTop - offset - cross;
            } else {
                item.y = height - layout.padTop - cursor - item.main;
                item.outHeight = item.main;
                item.outWidth = cross;
                item.x = layout.padLeft + offset;
            }
            cursor += item.main + between;
        }
    }

    private static void arrangeGrid(AutoLayoutComponent layout, float width, float height, Array<Item> items) {
        int n = items.size;
        float cellW = cellWidth(layout, items);
        float cellH = cellHeight(layout, items);
        int columns = Math.max(1, layout.columns);
        int rows = rows(layout, n);

        for (int i = 0; i < n; i++) {
            Item item = items.get(i);
            int column = layout.columnMajor ? i / rows : i % columns;
            int row = layout.columnMajor ? i % rows : i / columns;

            float left = layout.padLeft + column * (cellW + layout.gap);
            float top = height - layout.padTop - row * (cellH + layout.rowGap);

            item.outWidth = item.width;
            item.x = left;
            switch (layout.cellAlignX) {
                case STRETCH:
                    item.outWidth = cellW;
                    break;
                case CENTER:
                    item.x = left + (cellW - item.width) / 2;
                    break;
                case END:
                    item.x = left + cellW - item.width;
                    break;
                default:
                    break;
            }

            item.outHeight = item.height;
            item.y = top - item.height;
            switch (layout.cellAlignY) {
                case STRETCH:
                    item.outHeight = cellH;
                    item.y = top - cellH;
                    break;
                case CENTER:
                    item.y = top - cellH + (cellH - item.height) / 2;
                    break;
                case END:
                    item.y = top - cellH;
                    break;
                default:
                    break;
            }
        }
    }

    /** Length a child starts from along a row or a column, before the room left is shared. */
    private static float basis(Item item, boolean row) {
        if (item.grow > 0) return row ? item.contentWidth : item.contentHeight;
        return row ? item.width : item.height;
    }

    private static float crossNeed(AutoLayoutComponent layout, Item item, boolean row) {
        Align align = item.alignSelf != null ? item.alignSelf : layout.align;
        if (align == Align.STRETCH) return row ? item.contentHeight : item.contentWidth;
        return row ? item.height : item.width;
    }

    /** @return out, the size of a grid cell for these children */
    public static Vector2 cellSize(AutoLayoutComponent layout, Array<Item> items, Vector2 out) {
        return out.set(cellWidth(layout, items), cellHeight(layout, items));
    }

    /** @return how many rows a grid needs for n children */
    public static int gridRows(AutoLayoutComponent layout, int n) {
        return rows(layout, n);
    }

    /** @return how many columns of a grid hold a child */
    public static int gridColumns(AutoLayoutComponent layout, int n) {
        return usedColumns(layout, n);
    }

    private static float cellWidth(AutoLayoutComponent layout, Array<Item> items) {
        if (layout.cellWidth > 0) return layout.cellWidth;
        boolean stretch = layout.cellAlignX == Align.STRETCH;
        float size = 0;
        for (int i = 0; i < items.size; i++) {
            Item item = items.get(i);
            size = Math.max(size, stretch ? item.contentWidth : item.width);
        }
        return size;
    }

    private static float cellHeight(AutoLayoutComponent layout, Array<Item> items) {
        if (layout.cellHeight > 0) return layout.cellHeight;
        boolean stretch = layout.cellAlignY == Align.STRETCH;
        float size = 0;
        for (int i = 0; i < items.size; i++) {
            Item item = items.get(i);
            size = Math.max(size, stretch ? item.contentHeight : item.height);
        }
        return size;
    }

    private static int rows(AutoLayoutComponent layout, int n) {
        int columns = Math.max(1, layout.columns);
        return (n + columns - 1) / columns;
    }

    /** Columns that hold a child: fewer than asked when there are not enough children to fill a row. */
    private static int usedColumns(AutoLayoutComponent layout, int n) {
        int columns = Math.max(1, layout.columns);
        if (!layout.columnMajor) return Math.min(columns, n);
        int rows = rows(layout, n);
        return (n + rows - 1) / rows;
    }

    /** Whether the layout sizes this child on the given axis, so its own size there is not its need. */
    public static boolean sizesWidth(AutoLayoutComponent layout, float grow, Align alignSelf) {
        switch (layout.mode) {
            case ROW:
                return grow > 0;
            case COLUMN:
                return (alignSelf != null ? alignSelf : layout.align) == Align.STRETCH;
            default:
                return layout.cellAlignX == Align.STRETCH;
        }
    }

    public static boolean sizesHeight(AutoLayoutComponent layout, float grow, Align alignSelf) {
        switch (layout.mode) {
            case COLUMN:
                return grow > 0;
            case ROW:
                return (alignSelf != null ? alignSelf : layout.align) == Align.STRETCH;
            default:
                return layout.cellAlignY == Align.STRETCH;
        }
    }
}
