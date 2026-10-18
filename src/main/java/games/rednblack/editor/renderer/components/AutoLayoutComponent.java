package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;

/**
 * Makes a composite place its children itself: in a row, in a column, or in a grid of equal cells.
 *
 * Children are taken in the composite's order. What a child asks of the layout (to grow, to line up
 * differently, or to be left alone) is in its {@link AutoLayoutItemComponent}.
 */
public class AutoLayoutComponent extends PooledComponent {

    public enum Mode {ROW, COLUMN, GRID}

    /** How the children are spread along a row or a column when there is room left. */
    public enum Justify {START, CENTER, END, SPACE_BETWEEN}

    /**
     * How a child sits across a row or a column, or within its grid cell. START is the left or the
     * top, END the right or the bottom; STRETCH makes it as wide or as tall as there is room.
     */
    public enum Align {START, CENTER, END, STRETCH}

    public Mode mode = Mode.ROW;

    public float padLeft, padRight, padTop, padBottom;
    /** Between the children of a row or a column, or between the columns of a grid. */
    public float gap;
    /** Between the rows of a grid. */
    public float rowGap;

    public Justify justify = Justify.START;
    public Align align = Align.START;
    /** Lays a row out from the right, or a column from the bottom. */
    public boolean reverse;

    public int columns = 2;
    /** Size of a grid cell, 0 for as large as the largest child. */
    public float cellWidth, cellHeight;
    public Align cellAlignX = Align.START;
    public Align cellAlignY = Align.START;
    /** Fills a grid down the columns instead of along the rows. */
    public boolean columnMajor;

    /** Makes the composite as large as what it holds, plus padding, instead of keeping its size. */
    public boolean fitWidth, fitHeight;
    public boolean skipInvisible = true;

    /** The size of what it holds plus padding, measured every frame whether it fits it or not. */
    public transient float measuredWidth, measuredHeight;

    public boolean isLinear() {
        return mode != Mode.GRID;
    }

    @Override
    public void reset() {
        mode = Mode.ROW;
        padLeft = padRight = padTop = padBottom = 0;
        gap = rowGap = 0;
        justify = Justify.START;
        align = Align.START;
        reverse = false;
        columns = 2;
        cellWidth = cellHeight = 0;
        cellAlignX = cellAlignY = Align.START;
        columnMajor = false;
        fitWidth = fitHeight = false;
        skipInvisible = true;
        measuredWidth = measuredHeight = 0;
    }
}
