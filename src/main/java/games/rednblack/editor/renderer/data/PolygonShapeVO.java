package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class PolygonShapeVO {
    public Array<Vector2> vertices;
    public Vector2[][] polygonizedVertices;
    @Deprecated public Vector2 [][] polygons;

    public PolygonShapeVO clone() {
        PolygonShapeVO newVo = new PolygonShapeVO();
        Vector2[][] target = new Vector2[polygonizedVertices.length][];
        Array<Vector2> targetVertices = new Array<>(true, vertices.size, Vector2.class);

        for (int i = 0; i < polygonizedVertices.length; i++) {
            target[i] = new Vector2[polygonizedVertices[i].length];
            for (int j = 0; j < polygonizedVertices[i].length; j++) {
                target[i][j] = polygonizedVertices[i][j].cpy();
            }
        }
        newVo.polygonizedVertices = target;
        for (Vector2 vertex : vertices)
            targetVertices.add(vertex.cpy());
        newVo.vertices = targetVertices;

        return newVo;
    }

    public static PolygonShapeVO createRect(float width, float height) {
        PolygonShapeVO vo = new PolygonShapeVO();
        vo.vertices = new Array<>(true, 4, Vector2.class);
        vo.polygonizedVertices = new Vector2[1][];
        vo.vertices.add(new Vector2(0, 0), new Vector2(0, height), new Vector2(width, height), new Vector2(width, 0));

        vo.polygonizedVertices[0] = vo.vertices.toArray();

        return vo;
    }
}
