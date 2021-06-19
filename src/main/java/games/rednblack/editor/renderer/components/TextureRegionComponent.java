package games.rednblack.editor.renderer.components;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.PolygonUtils;
import games.rednblack.editor.renderer.utils.RepeatablePolygonSprite;

public class TextureRegionComponent extends RefreshableComponent {
    protected boolean needsRefresh = false;

    public String regionName = "";
    public TextureRegion region = null;
    public boolean isRepeat = false;
    public boolean isPolygon = false;

    // optional
    public RepeatablePolygonSprite repeatablePolygonSprite = null;

    public void setPolygonSprite(PolygonComponent polygonComponent) {
        Vector2[] verticesArray = PolygonUtils.mergeTouchingPolygonsToOne(polygonComponent.vertices);
        float[] vertices = new float[verticesArray.length * 2];
        for (int i = 0; i < verticesArray.length; i++) {
            vertices[i * 2] = verticesArray[i].x;
            vertices[i * 2 + 1] = verticesArray[i].y;
        }

        if (repeatablePolygonSprite == null)
            repeatablePolygonSprite = new RepeatablePolygonSprite();
        repeatablePolygonSprite.clear();
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
        PolygonComponent polygonComponent = ComponentRetriever.get(entity, PolygonComponent.class);

        if (isPolygon && polygonComponent != null) {
            DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class);
            dimensionsComponent.setPolygon(polygonComponent);
            setPolygonSprite(polygonComponent);
        }
    }
}
