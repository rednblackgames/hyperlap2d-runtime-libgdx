package games.rednblack.editor.renderer.utils.poly;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class PolygonRuntimeUtils {
    public static float getPolygonSignedArea(Vector2[] points) {
        if (points.length < 3)
            return 0;

        float sum = 0;
        for (int i = 0; i < points.length; i++) {
            Vector2 p1 = points[i];
            Vector2 p2 = i != points.length-1 ? points[i+1] : points[0];
            sum += (p1.x * p2.y) - (p1.y * p2.x);
        }
        return 0.5f * sum;
    }

    public static float getPolygonArea(Vector2[] points) {
        return Math.abs(getPolygonSignedArea(points));
    }

    public static boolean isPolygonCCW(Vector2[] points) {
        return getPolygonSignedArea(points) > 0;
    }

    public static Vector2[][] polygonize(Vector2[] vertices) {
        return Clipper.polygonize(Clipper.Polygonizer.EWJORDAN, vertices);
    }

    public static Array<Vector2> cloneData(Array<Vector2> data) {
        Array<Vector2> clone = new Array<>(true, data.size, Vector2.class);
        for (Vector2 vector2 : data) {
            clone.add(vector2.cpy());
        }
        return clone;
    }
}
