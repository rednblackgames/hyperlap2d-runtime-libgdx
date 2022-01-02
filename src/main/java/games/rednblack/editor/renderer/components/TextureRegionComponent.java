package games.rednblack.editor.renderer.components;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.utils.RepeatablePolygonSprite;

public class TextureRegionComponent extends RefreshableComponent {

    protected transient ComponentMapper<DimensionsComponent> dimensionsCM;
    protected transient ComponentMapper<PolygonShapeComponent> polygonCM;

    protected boolean needsRefresh = false;

    public String regionName = "";
    public transient TextureRegion region = null;
    public boolean isRepeat = false;
    public boolean isPolygon = false;

    public float ppwu = 1;

    // optional
    public transient RepeatablePolygonSprite repeatablePolygonSprite = null;

    public void setPolygonSprite(PolygonShapeComponent polygonShapeComponent) {
        Array<Vector2> verticesArray = polygonShapeComponent.vertices;
        if (verticesArray == null) return;
        //TODO Another buddy that should be pooled
        float[] vertices = new float[verticesArray.size * 2];
        for (int i = 0; i < verticesArray.size; i++) {
            vertices[i * 2] = verticesArray.get(i).x;
            vertices[i * 2 + 1] = verticesArray.get(i).y;
        }

        if (repeatablePolygonSprite == null)
            repeatablePolygonSprite = new RepeatablePolygonSprite();
        repeatablePolygonSprite.clear();
        repeatablePolygonSprite.setWorldMultiplier(1f / ppwu);
        repeatablePolygonSprite.setVertices(vertices);
        repeatablePolygonSprite.setTextureRegion(region);
    }

    @Override
    public void reset() {
        regionName = "";
        region = null;
        repeatablePolygonSprite = null;
        isRepeat = false;
        isPolygon = false;
        needsRefresh = false;
    }

    @Override
    public void scheduleRefresh() {
        needsRefresh = true;
    }

    @Override
    public void executeRefresh(int entity) {
        if (needsRefresh) {
            refresh(entity);
            needsRefresh = false;
        }
    }

    protected void refresh(int entity) {
        PolygonShapeComponent polygonShapeComponent = polygonCM.get(entity);

        if (isPolygon && polygonShapeComponent != null && polygonShapeComponent.vertices != null) {
            DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);
            dimensionsComponent.setPolygon(polygonShapeComponent);
            setPolygonSprite(polygonShapeComponent);
        }
    }
}
