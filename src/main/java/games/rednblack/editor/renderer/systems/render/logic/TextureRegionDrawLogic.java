package games.rednblack.editor.renderer.systems.render.logic;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.RepeatablePolygonSprite;

public class TextureRegionDrawLogic implements Drawable {

    private final ComponentMapper<TintComponent> tintComponentComponentMapper;
    private final ComponentMapper<TextureRegionComponent> textureRegionMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<DimensionsComponent> dimensionsComponentComponentMapper;

    private final Color batchColor = new Color();

    public TextureRegionDrawLogic() {
        tintComponentComponentMapper = ComponentMapper.getFor(TintComponent.class);
        textureRegionMapper = ComponentMapper.getFor(TextureRegionComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        dimensionsComponentComponentMapper = ComponentMapper.getFor(DimensionsComponent.class);
    }

    @Override
    public void draw(Batch batch, Entity entity, float parentAlpha) {
        TextureRegionComponent entityTextureRegionComponent = textureRegionMapper.get(entity);
        ShaderComponent shaderComponent = ComponentRetriever.get(entity, ShaderComponent.class);

        entityTextureRegionComponent.executeRefresh(entity);

        batchColor.set(batch.getColor());

        if (entityTextureRegionComponent.isPolygon &&
                entityTextureRegionComponent.repeatablePolygonSprite != null &&
                (shaderComponent == null || shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN)) {
            drawRepeatablePolygonSprite(batch, entity, parentAlpha);
        } else {
            drawSprite(batch, entity, parentAlpha);
        }

        batch.setColor(batchColor);
    }

    public void drawRepeatablePolygonSprite (Batch batch, Entity entity, float parentAlpha) {
        TintComponent tintComponent = tintComponentComponentMapper.get(entity);
        TextureRegionComponent textureRegionComponent = textureRegionMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);

        RepeatablePolygonSprite repeatablePolygonSprite = textureRegionComponent.repeatablePolygonSprite;
        boolean isRepeat = textureRegionComponent.isRepeat;

        repeatablePolygonSprite.setPosition(entityTransformComponent.x, entityTransformComponent.y);
        repeatablePolygonSprite.setOrigin(entityTransformComponent.originX, entityTransformComponent.originY);
        repeatablePolygonSprite.setScale(entityTransformComponent.scaleX, entityTransformComponent.scaleY);
        repeatablePolygonSprite.setRotation(entityTransformComponent.rotation);
        repeatablePolygonSprite.setColor(tintComponent.color.r, tintComponent.color.g, tintComponent.color.b, tintComponent.color.a * parentAlpha);
        repeatablePolygonSprite.setWrapTypeY(isRepeat ? RepeatablePolygonSprite.WrapType.REPEAT : RepeatablePolygonSprite.WrapType.STRETCH);
        repeatablePolygonSprite.setWrapTypeX(isRepeat ? RepeatablePolygonSprite.WrapType.REPEAT : RepeatablePolygonSprite.WrapType.STRETCH);

        repeatablePolygonSprite.draw((PolygonSpriteBatch) batch);
    }

    public void drawSprite(Batch batch, Entity entity, float parentAlpha) {
        TintComponent tintComponent = tintComponentComponentMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);
        TextureRegionComponent entityTextureRegionComponent = textureRegionMapper.get(entity);
        DimensionsComponent dimensionsComponent = dimensionsComponentComponentMapper.get(entity);
        batch.setColor(tintComponent.color.r, tintComponent.color.g, tintComponent.color.b, tintComponent.color.a * parentAlpha);

        batch.draw(entityTextureRegionComponent.region,
                entityTransformComponent.x, entityTransformComponent.y,
                entityTransformComponent.originX, entityTransformComponent.originY,
                dimensionsComponent.width, dimensionsComponent.height,
                entityTransformComponent.scaleX, entityTransformComponent.scaleY,
                entityTransformComponent.rotation);
    }
}
