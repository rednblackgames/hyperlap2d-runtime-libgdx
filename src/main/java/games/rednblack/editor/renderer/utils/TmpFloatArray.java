package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.utils.IntMap;

public class TmpFloatArray {
    private final IntMap<float[]> cache = new IntMap<>();

    public float[] getTemporaryArray(int size) {
        if (!cache.containsKey(size))
            cache.put(size, new float[size]);
        return cache.get(size);
    }
}
