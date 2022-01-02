package games.rednblack.editor.renderer.components.shape;

import com.artemis.PooledComponent;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class PolygonShapeComponent extends PooledComponent {
    public Array<Vector2> vertices;
    public Vector2[][] polygonizedVertices;

    public boolean openEnded = false;

    public void makeRectangle(float width, float height) {
        vertices = new Array<>(true, 4, Vector2.class);
        vertices.add(new Vector2(0, 0), new Vector2(0, height), new Vector2(width, height), new Vector2(width, 0));

        polygonizedVertices = new Vector2[1][4];
        polygonizedVertices[0] = vertices.toArray();
    }

    public void makeRectangle(float x, float y, float width, float height) {
        vertices = new Array<>(true, 4, Vector2.class);
        vertices.add(new Vector2(x, y), new Vector2(x, y + height), new Vector2(x + width, y + height), new Vector2(x + width, y));

        polygonizedVertices = new Vector2[1][4];
        polygonizedVertices[0] = vertices.toArray();
    }

    @Override
    public void reset() {
        vertices = null;
        polygonizedVertices = null;
        openEnded = false;
    }
}
