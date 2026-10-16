package games.rednblack.editor.renderer.systems.render;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * The overlays met while drawing the scene, kept aside to be drawn once the scene is done.
 *
 * Each one remembers what it would have been drawn with where it was met - the batch matrix, the
 * offset its parent gives its children, the alpha of the composites above it - so it lands exactly
 * where it would have been, only in front of everything and outside any clipping.
 *
 * Drawing an overlay can meet another one inside it, a list open in a window, so they are handed
 * out in rounds: what one round defers comes out in the next.
 */
public class OverlayQueue {

    public static class Entry implements Pool.Poolable {
        public int entity = -1;
        public int order;
        public final Matrix4 matrix = new Matrix4();
        public float offsetX, offsetY;
        public float parentAlpha;

        @Override
        public void reset() {
            entity = -1;
            order = 0;
            matrix.idt();
            offsetX = offsetY = 0;
            parentAlpha = 1;
        }
    }

    private final Pool<Entry> pool = new Pool<Entry>() {
        @Override
        protected Entry newObject() {
            return new Entry();
        }
    };

    private Array<Entry> pending = new Array<>(false, 4, Entry[]::new);
    private Array<Entry> round = new Array<>(false, 4, Entry[]::new);

    public void defer(int entity, int order, Matrix4 matrix, float offsetX, float offsetY, float parentAlpha) {
        Entry entry = pool.obtain();
        entry.entity = entity;
        entry.order = order;
        entry.matrix.set(matrix);
        entry.offsetX = offsetX;
        entry.offsetY = offsetY;
        entry.parentAlpha = parentAlpha;
        pending.add(entry);
    }

    /**
     * Hands out what has been deferred since the last call, the first opened first so the last one
     * ends up in front. The entries of the round before go back to the pool.
     *
     * @return the next round, empty once there is nothing left
     */
    public Array<Entry> next() {
        pool.freeAll(round);
        round.clear();

        Array<Entry> swap = round;
        round = pending;
        pending = swap;
        round.sort((a, b) -> Integer.compare(a.order, b.order));
        return round;
    }

    public boolean isEmpty() {
        return pending.size == 0;
    }
}
