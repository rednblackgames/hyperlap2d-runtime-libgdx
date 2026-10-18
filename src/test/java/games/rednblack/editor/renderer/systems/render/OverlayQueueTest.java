package games.rednblack.editor.renderer.systems.render;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OverlayQueueTest {

    private final Matrix4 matrix = new Matrix4();

    @Test
    public void aRoundComesOutInTheOrderTheOverlaysWereOpened() {
        OverlayQueue queue = new OverlayQueue();
        //met in tree order, which is not the order they were opened in
        queue.defer(10, 3, matrix, 0, 0, 1);
        queue.defer(11, 1, matrix, 0, 0, 1);
        queue.defer(12, 2, matrix, 0, 0, 1);

        Array<OverlayQueue.Entry> round = queue.next();

        assertEquals(3, round.size);
        assertEquals(11, round.get(0).entity);
        assertEquals(12, round.get(1).entity);
        assertEquals(10, round.get(2).entity);
    }

    @Test
    public void whatARoundDefersComesOutInTheNext() {
        OverlayQueue queue = new OverlayQueue();
        queue.defer(10, 1, matrix, 0, 0, 1);

        Array<OverlayQueue.Entry> first = queue.next();
        assertEquals(1, first.size);
        //drawing the window meets the list open inside it
        queue.defer(20, 2, matrix, 0, 0, 1);

        Array<OverlayQueue.Entry> second = queue.next();
        assertEquals(1, second.size);
        assertEquals(20, second.get(0).entity);

        assertEquals(0, queue.next().size);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void anEntryKeepsWhatItWouldHaveBeenDrawnWith() {
        OverlayQueue queue = new OverlayQueue();
        Matrix4 moved = new Matrix4().translate(30, 40, 0);
        queue.defer(10, 1, moved, 5, 6, 0.5f);
        //the batch goes on changing its matrix after the overlay was met
        moved.idt();

        OverlayQueue.Entry entry = queue.next().first();

        assertEquals(30, entry.matrix.val[Matrix4.M03], 0.001f);
        assertEquals(40, entry.matrix.val[Matrix4.M13], 0.001f);
        assertEquals(5, entry.offsetX, 0.001f);
        assertEquals(6, entry.offsetY, 0.001f);
        assertEquals(0.5f, entry.parentAlpha, 0.001f);
    }
}
