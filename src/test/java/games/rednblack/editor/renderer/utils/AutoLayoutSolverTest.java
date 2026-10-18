package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.AutoLayoutComponent;
import games.rednblack.editor.renderer.components.AutoLayoutComponent.Align;
import games.rednblack.editor.renderer.components.AutoLayoutComponent.Justify;
import games.rednblack.editor.renderer.components.AutoLayoutComponent.Mode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** A composite 200 by 100 unless said otherwise; positions are of the children's bottom left corner. */
public class AutoLayoutSolverTest {

    private AutoLayoutComponent layout;
    private Array<AutoLayoutSolver.Item> items;

    @Before
    public void setUp() {
        layout = new AutoLayoutComponent();
        items = new Array<>();
    }

    private AutoLayoutSolver.Item item(float width, float height) {
        AutoLayoutSolver.Item item = new AutoLayoutSolver.Item().set(width, height);
        items.add(item);
        return item;
    }

    private void arrange(float width, float height) {
        AutoLayoutSolver.arrange(layout, width, height, items);
    }

    private static void assertPlaced(AutoLayoutSolver.Item item, float x, float y, float width, float height) {
        assertEquals("x", x, item.x, 0.001f);
        assertEquals("y", y, item.y, 0.001f);
        assertEquals("width", width, item.outWidth, 0.001f);
        assertEquals("height", height, item.outHeight, 0.001f);
    }

    @Test
    public void aRowStartsTopLeftWithPaddingAndGaps() {
        layout.padLeft = 5;
        layout.padTop = 10;
        layout.gap = 4;
        AutoLayoutSolver.Item a = item(20, 30);
        AutoLayoutSolver.Item b = item(40, 10);
        arrange(200, 100);

        assertPlaced(a, 5, 60, 20, 30);
        assertPlaced(b, 29, 80, 40, 10);
    }

    @Test
    public void aRowSpreadsWhatIsLeft() {
        AutoLayoutSolver.Item a = item(20, 10);
        AutoLayoutSolver.Item b = item(30, 10);

        layout.justify = Justify.CENTER;
        arrange(200, 100);
        assertEquals(75, a.x, 0.001f);
        assertEquals(95, b.x, 0.001f);

        layout.justify = Justify.END;
        arrange(200, 100);
        assertEquals(150, a.x, 0.001f);
        assertEquals(170, b.x, 0.001f);

        layout.justify = Justify.SPACE_BETWEEN;
        arrange(200, 100);
        assertEquals(0, a.x, 0.001f);
        assertEquals(170, b.x, 0.001f);
    }

    @Test
    public void growingChildrenShareTheRoomByWeightAndDropTheirOwnLength() {
        layout.gap = 10;
        AutoLayoutSolver.Item fixed = item(50, 10);
        AutoLayoutSolver.Item one = item(500, 10);
        one.grow = 1;
        AutoLayoutSolver.Item two = item(0, 10);
        two.grow = 3;
        arrange(200, 100);

        // 200 - 50 - 2 gaps = 130 shared 1 : 3
        assertPlaced(fixed, 0, 90, 50, 10);
        assertPlaced(one, 60, 90, 32.5f, 10);
        assertPlaced(two, 102.5f, 90, 97.5f, 10);
    }

    @Test
    public void growingTakesNothingWhenThereIsNoRoom() {
        AutoLayoutSolver.Item fixed = item(250, 10);
        AutoLayoutSolver.Item grows = item(40, 10);
        grows.grow = 1;
        arrange(200, 100);

        assertEquals(250, fixed.outWidth, 0.001f);
        assertEquals(0, grows.outWidth, 0.001f);
    }

    @Test
    public void childrenLineUpAcrossARow() {
        AutoLayoutSolver.Item a = item(20, 20);
        AutoLayoutSolver.Item b = item(20, 20);
        AutoLayoutSolver.Item c = item(20, 20);
        layout.align = Align.CENTER;
        b.alignSelf = Align.END;
        c.alignSelf = Align.STRETCH;
        layout.padBottom = 10;
        arrange(200, 100);

        assertPlaced(a, 0, 45, 20, 20);
        assertPlaced(b, 20, 10, 20, 20);
        assertPlaced(c, 40, 10, 20, 90);
    }

    @Test
    public void aColumnGoesDownFromTheTop() {
        layout.mode = Mode.COLUMN;
        layout.gap = 5;
        layout.align = Align.END;
        AutoLayoutSolver.Item a = item(20, 30);
        AutoLayoutSolver.Item b = item(40, 10);
        arrange(200, 100);

        assertPlaced(a, 180, 70, 20, 30);
        assertPlaced(b, 160, 55, 40, 10);
    }

    @Test
    public void reverseStartsFromTheOtherEnd() {
        layout.reverse = true;
        AutoLayoutSolver.Item a = item(20, 10);
        AutoLayoutSolver.Item b = item(30, 10);
        arrange(200, 100);

        assertEquals(30, a.x, 0.001f);
        assertEquals(0, b.x, 0.001f);
    }

    @Test
    public void aLinearLayoutMeasuresItsChildren() {
        layout.padLeft = 1;
        layout.padRight = 2;
        layout.padTop = 3;
        layout.padBottom = 4;
        layout.gap = 10;
        item(20, 30);
        item(40, 10);
        AutoLayoutSolver.Item grows = item(999, 5);
        grows.grow = 1;
        grows.contentWidth = 7;

        Vector2 size = AutoLayoutSolver.measure(layout, items, new Vector2());
        assertEquals(1 + 20 + 40 + 7 + 2 * 10 + 2, size.x, 0.001f);
        assertEquals(3 + 30 + 4, size.y, 0.001f);

        layout.mode = Mode.COLUMN;
        size = AutoLayoutSolver.measure(layout, items, new Vector2());
        assertEquals(1 + 999 + 2, size.x, 0.001f);
        // the growing child takes no height of its own
        assertEquals(3 + 30 + 10 + 0 + 2 * 10 + 4, size.y, 0.001f);
    }

    @Test
    public void stretchedChildrenAskOnlyForWhatTheyHold() {
        layout.align = Align.STRETCH;
        item(20, 300);
        AutoLayoutSolver.Item holds = item(20, 300);
        holds.contentHeight = 40;

        assertEquals(40, AutoLayoutSolver.measure(layout, items, new Vector2()).y, 0.001f);
    }

    @Test
    public void aGridFillsRowsOfEqualCells() {
        layout.mode = Mode.GRID;
        layout.columns = 2;
        layout.gap = 10;
        layout.rowGap = 5;
        AutoLayoutSolver.Item a = item(20, 10);
        AutoLayoutSolver.Item b = item(40, 30);
        AutoLayoutSolver.Item c = item(10, 10);
        arrange(200, 100);

        // cells are 40 by 30
        assertPlaced(a, 0, 90, 20, 10);
        assertPlaced(b, 50, 70, 40, 30);
        assertPlaced(c, 0, 55, 10, 10);

        Vector2 size = AutoLayoutSolver.measure(layout, items, new Vector2());
        assertEquals(40 + 10 + 40, size.x, 0.001f);
        assertEquals(30 + 5 + 30, size.y, 0.001f);
    }

    @Test
    public void gridCellsAlignAndStretchTheirChildren() {
        layout.mode = Mode.GRID;
        layout.columns = 2;
        layout.cellWidth = 50;
        layout.cellHeight = 40;
        layout.cellAlignX = Align.CENTER;
        layout.cellAlignY = Align.STRETCH;
        AutoLayoutSolver.Item a = item(20, 10);
        AutoLayoutSolver.Item b = item(30, 10);
        arrange(200, 100);

        assertPlaced(a, 15, 60, 20, 40);
        assertPlaced(b, 60, 60, 30, 40);
    }

    @Test
    public void aGridCanFillDownTheColumns() {
        layout.mode = Mode.GRID;
        layout.columns = 2;
        layout.columnMajor = true;
        AutoLayoutSolver.Item a = item(10, 10);
        AutoLayoutSolver.Item b = item(10, 10);
        AutoLayoutSolver.Item c = item(10, 10);
        arrange(200, 100);

        // two rows, so the third child starts the second column
        assertPlaced(a, 0, 90, 10, 10);
        assertPlaced(b, 0, 80, 10, 10);
        assertPlaced(c, 10, 90, 10, 10);
    }

    @Test
    public void aGridWithFewChildrenIsOnlyAsWideAsThey() {
        layout.mode = Mode.GRID;
        layout.columns = 4;
        item(10, 10);
        item(10, 10);

        assertEquals(20, AutoLayoutSolver.measure(layout, items, new Vector2()).x, 0.001f);
    }

    @Test
    public void nothingToLayOutIsOnlyPadding() {
        layout.padLeft = 3;
        layout.padTop = 4;
        Vector2 size = AutoLayoutSolver.measure(layout, items, new Vector2());
        assertEquals(3, size.x, 0.001f);
        assertEquals(4, size.y, 0.001f);
    }
}
