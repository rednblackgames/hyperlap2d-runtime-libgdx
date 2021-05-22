package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.normal.NormalTextureRegionComponent;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.RepeatablePolygonSprite;

public class TextureRegionDrawLogic implements Drawable {

    private final ComponentMapper<TintComponent> tintComponentComponentMapper;
    private final ComponentMapper<TextureRegionComponent> textureRegionMapper;
    private final ComponentMapper<NormalTextureRegionComponent> normalTextureRegionMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<DimensionsComponent> dimensionsComponentComponentMapper;

    private final Color batchColor = new Color();

    public TextureRegionDrawLogic() {
        tintComponentComponentMapper = ComponentMapper.getFor(TintComponent.class);
        textureRegionMapper = ComponentMapper.getFor(TextureRegionComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        dimensionsComponentComponentMapper = ComponentMapper.getFor(DimensionsComponent.class);
        normalTextureRegionMapper = ComponentMapper.getFor(NormalTextureRegionComponent.class);
    }

    @Override
    public void draw(Batch batch, Entity entity, float parentAlpha, RenderingType renderingType) {
        TextureRegionComponent entityTextureRegionComponent = textureRegionMapper.get(entity);
        ShaderComponent shaderComponent = ComponentRetriever.get(entity, ShaderComponent.class);

        entityTextureRegionComponent.executeRefresh(entity);

        batchColor.set(batch.getColor());

        if (entityTextureRegionComponent.isPolygon &&
                entityTextureRegionComponent.repeatablePolygonSprite != null &&
                (shaderComponent == null || shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN)) {
            drawRepeatablePolygonSprite(batch, entity, parentAlpha);
        } else {
            drawSprite(batch, entity, parentAlpha, renderingType);
        }

        batch.setColor(batchColor);
    }

    public void drawRepeatablePolygonSprite (Batch batch, Entity entity, float parentAlpha) {
        TintComponent tintComponent = tintComponentComponentMapper.get(entity);
        TextureRegionComponent textureRegionComponent = textureRegionMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);
        DimensionsComponent dimensionsComponent = dimensionsComponentComponentMapper.get(entity);

        RepeatablePolygonSprite repeatablePolygonSprite = textureRegionComponent.repeatablePolygonSprite;
        boolean isRepeat = textureRegionComponent.isRepeat;

        repeatablePolygonSprite.setPosition(entityTransformComponent.x, entityTransformComponent.y);
        float scaleX = entityTransformComponent.scaleX * (entityTransformComponent.flipX ? -1 : 1);
        float scaleY = entityTransformComponent.scaleY * (entityTransformComponent.flipY ? -1 : 1);
        Rectangle b = dimensionsComponent.polygon.getBoundingRectangle();
        repeatablePolygonSprite.setOrigin(entityTransformComponent.originX + b.x * scaleX,
                entityTransformComponent.originY + b.y * scaleY);
        repeatablePolygonSprite.setScale(scaleX, scaleY);
        repeatablePolygonSprite.setRotation(entityTransformComponent.rotation);
        repeatablePolygonSprite.setColor(tintComponent.color.r, tintComponent.color.g, tintComponent.color.b, tintComponent.color.a * parentAlpha);
        repeatablePolygonSprite.setWrapType(isRepeat ? RepeatablePolygonSprite.WrapType.REPEAT : RepeatablePolygonSprite.WrapType.STRETCH);

        repeatablePolygonSprite.draw((PolygonSpriteBatch) batch);
    }

    public void drawSprite(Batch batch, Entity entity, float parentAlpha, RenderingType renderingType) {
        TintComponent tintComponent = tintComponentComponentMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);
        TextureRegionComponent entityTextureRegionComponent = textureRegionMapper.get(entity);
        DimensionsComponent dimensionsComponent = dimensionsComponentComponentMapper.get(entity);

        batch.setColor(tintComponent.color.r, tintComponent.color.g, tintComponent.color.b, tintComponent.color.a * parentAlpha);

        float scaleX = entityTransformComponent.scaleX * (entityTransformComponent.flipX ? -1 : 1);
        float scaleY = entityTransformComponent.scaleY * (entityTransformComponent.flipY ? -1 : 1);

        TextureRegion region;
        NormalTextureRegionComponent normalComponent = normalTextureRegionMapper.get(entity);
        if (normalComponent != null && renderingType == RenderingType.NORMAL_MAP)
            region = normalComponent.textureRegion;
        else
            region = entityTextureRegionComponent.region;

        batch.draw(region,
                entityTransformComponent.x, entityTransformComponent.y,
                entityTransformComponent.originX, entityTransformComponent.originY,
                dimensionsComponent.width, dimensionsComponent.height,
                scaleX, scaleY,
                entityTransformComponent.rotation);
    }
}
