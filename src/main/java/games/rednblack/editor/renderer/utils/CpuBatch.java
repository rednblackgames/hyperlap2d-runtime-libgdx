package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.math.Affine2;

public interface CpuBatch {
    void flushAndSyncTransformMatrix();
    void setTransformMatrix(Affine2 transform);
}
