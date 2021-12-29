package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;

public class DimensionsComponent extends PooledComponent {
    public float width = 0;
    public float height = 0;

    public Rectangle boundBox;
    public Polygon polygon;

    public boolean hit(float x, float y) {
        if (polygon != null) {
            return polygon.contains(x, y);
        } else if (boundBox != null) {
            return (x >= boundBox.x && x < boundBox.x + boundBox.width && y >= boundBox.y && y < boundBox.y + boundBox.height);
        } else {
            return (x >= 0 && x < width && y >= 0 && y < height);
        }
    }

    public void setPolygon(PolygonShapeComponent polygonShapeComponent) {
        Array<Vector2> verticesArray = polygonShapeComponent.vertices;
        if (verticesArray == null) return;
        float[] vertices = new float[verticesArray.size * 2];
        for (int i = 0; i < verticesArray.size; i++) {
            vertices[i * 2] = (verticesArray.get(i).x);
            vertices[i * 2 + 1] = (verticesArray.get(i).y);
        }
        polygon = new Polygon(vertices);
    }

    @Override
    public void reset() {
        width = 0;
        height = 0;
        boundBox = null;
        polygon = null;
    }
}
