package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import games.rednblack.editor.renderer.utils.value.DynamicValue;

public class ABRepeatablePolygonSprite extends RepeatablePolygonSprite {

    private final RepeatablePolygonSprite regionB;

    private DynamicValue<Boolean> useRegionB;

    public ABRepeatablePolygonSprite(RepeatablePolygonSprite regionB, DynamicValue<Boolean> useRegionB) {
        this.regionB = regionB;
        this.useRegionB = useRegionB;
    }

    public void setUseRegionB(DynamicValue<Boolean> useRegionB) {
        this.useRegionB = useRegionB;
    }

    @Override
    public void draw(PolygonSpriteBatch batch) {
        if (useRegionB != null && useRegionB.get())
            regionB.draw(batch);
        else
            super.draw(batch);
    }

    @Override
    public void drawDebug(ShapeRenderer shapes, Color color) {
        if (useRegionB != null && useRegionB.get())
            regionB.drawDebug(shapes, color);
        else
            super.drawDebug(shapes, color);
    }

    @Override
    public void dispose() {
        super.dispose();
        regionB.dispose();
    }

    @Override
    public void clear() {
        super.clear();
        regionB.clear();
    }

    @Override
    public void setVertices(float[] vertices) {
        super.setVertices(vertices);
        regionB.setVertices(vertices);
    }

    @Override
    public void setTextureOffset(float x, float y) {
        super.setTextureOffset(x, y);
        regionB.setTextureOffset(x, y);
    }

    @Override
    public void setTextureSize(float width, float height) {
        super.setTextureSize(width, height);
        regionB.setTextureSize(width, height);
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        regionB.setPosition(x, y);
    }

    @Override
    public void setOrigin(float x, float y) {
        super.setOrigin(x, y);
        regionB.setOrigin(x, y);
    }

    @Override
    public void setScale(float scaleX, float scaleY) {
        super.setScale(scaleX, scaleY);
        regionB.setScale(scaleX, scaleY);
    }

    @Override
    public void setRotation(float degrees) {
        super.setRotation(degrees);
        regionB.setRotation(degrees);
    }

    @Override
    public void setWrapTypeX(WrapType wrapType) {
        super.setWrapTypeX(wrapType);
        regionB.setWrapTypeX(wrapType);
    }

    @Override
    public void setWrapTypeY(WrapType wrapType) {
        super.setWrapTypeY(wrapType);
        regionB.setWrapTypeY(wrapType);
    }

    @Override
    public void setColor(Color color) {
        super.setColor(color);
        regionB.setColor(color);
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        super.setColor(r, g, b, a);
        regionB.setColor(r, g, b, a);
    }

    @Override
    public void setWorldMultiplier(float worldMultiplier) {
        super.setWorldMultiplier(worldMultiplier);
        regionB.setWorldMultiplier(worldMultiplier);
    }
}
