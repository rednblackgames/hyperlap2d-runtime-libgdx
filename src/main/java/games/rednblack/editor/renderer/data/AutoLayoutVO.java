package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.AutoLayoutComponent;

import java.util.Objects;

public class AutoLayoutVO {
    public AutoLayoutComponent.Mode mode = AutoLayoutComponent.Mode.ROW;
    public float padLeft, padRight, padTop, padBottom;
    public float gap;
    public float rowGap;
    public AutoLayoutComponent.Justify justify = AutoLayoutComponent.Justify.START;
    public AutoLayoutComponent.Align align = AutoLayoutComponent.Align.START;
    public boolean reverse;
    public int columns = 2;
    public float cellWidth, cellHeight;
    public AutoLayoutComponent.Align cellAlignX = AutoLayoutComponent.Align.START;
    public AutoLayoutComponent.Align cellAlignY = AutoLayoutComponent.Align.START;
    public boolean columnMajor;
    public boolean fitWidth, fitHeight;
    public boolean skipInvisible = true;

    public AutoLayoutVO() {
    }

    public AutoLayoutVO(AutoLayoutVO vo) {
        if (vo == null) return;
        mode = vo.mode;
        padLeft = vo.padLeft;
        padRight = vo.padRight;
        padTop = vo.padTop;
        padBottom = vo.padBottom;
        gap = vo.gap;
        rowGap = vo.rowGap;
        justify = vo.justify;
        align = vo.align;
        reverse = vo.reverse;
        columns = vo.columns;
        cellWidth = vo.cellWidth;
        cellHeight = vo.cellHeight;
        cellAlignX = vo.cellAlignX;
        cellAlignY = vo.cellAlignY;
        columnMajor = vo.columnMajor;
        fitWidth = vo.fitWidth;
        fitHeight = vo.fitHeight;
        skipInvisible = vo.skipInvisible;
    }

    public void loadFromComponent(AutoLayoutComponent component) {
        mode = component.mode;
        padLeft = component.padLeft;
        padRight = component.padRight;
        padTop = component.padTop;
        padBottom = component.padBottom;
        gap = component.gap;
        rowGap = component.rowGap;
        justify = component.justify;
        align = component.align;
        reverse = component.reverse;
        columns = component.columns;
        cellWidth = component.cellWidth;
        cellHeight = component.cellHeight;
        cellAlignX = component.cellAlignX;
        cellAlignY = component.cellAlignY;
        columnMajor = component.columnMajor;
        fitWidth = component.fitWidth;
        fitHeight = component.fitHeight;
        skipInvisible = component.skipInvisible;
    }

    public void applyToComponent(AutoLayoutComponent component) {
        component.mode = mode != null ? mode : AutoLayoutComponent.Mode.ROW;
        component.padLeft = padLeft;
        component.padRight = padRight;
        component.padTop = padTop;
        component.padBottom = padBottom;
        component.gap = gap;
        component.rowGap = rowGap;
        component.justify = justify != null ? justify : AutoLayoutComponent.Justify.START;
        component.align = align != null ? align : AutoLayoutComponent.Align.START;
        component.reverse = reverse;
        component.columns = Math.max(1, columns);
        component.cellWidth = cellWidth;
        component.cellHeight = cellHeight;
        component.cellAlignX = cellAlignX != null ? cellAlignX : AutoLayoutComponent.Align.START;
        component.cellAlignY = cellAlignY != null ? cellAlignY : AutoLayoutComponent.Align.START;
        component.columnMajor = columnMajor;
        component.fitWidth = fitWidth;
        component.fitHeight = fitHeight;
        component.skipInvisible = skipInvisible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoLayoutVO that = (AutoLayoutVO) o;
        return Float.compare(padLeft, that.padLeft) == 0 && Float.compare(padRight, that.padRight) == 0
                && Float.compare(padTop, that.padTop) == 0 && Float.compare(padBottom, that.padBottom) == 0
                && Float.compare(gap, that.gap) == 0 && Float.compare(rowGap, that.rowGap) == 0
                && reverse == that.reverse && columns == that.columns
                && Float.compare(cellWidth, that.cellWidth) == 0 && Float.compare(cellHeight, that.cellHeight) == 0
                && columnMajor == that.columnMajor && fitWidth == that.fitWidth && fitHeight == that.fitHeight
                && skipInvisible == that.skipInvisible && mode == that.mode && justify == that.justify
                && align == that.align && cellAlignX == that.cellAlignX && cellAlignY == that.cellAlignY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, padLeft, padRight, padTop, padBottom, gap, rowGap, justify, align, reverse,
                columns, cellWidth, cellHeight, cellAlignX, cellAlignY, columnMajor, fitWidth, fitHeight, skipInvisible);
    }
}
