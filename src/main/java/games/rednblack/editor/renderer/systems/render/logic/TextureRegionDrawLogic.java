package games.rednblack.editor.renderer.systems.render.logic;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.utils.RepeatablePolygonSprite;

public class TextureRegionDrawLogic implements DrawableLogic {

    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<NormalMapRendering> normalMapRenderingMapper;
    protected ComponentMapper<ShaderComponent> shaderMapper;
    protected ComponentMapper<TextureRegionComponent> textureRegionMapper;
    protected ComponentMapper<TintComponent> tintMapper;
    protected ComponentMapper<TransformComponent> transformMapper;

    private final Color batchColor = new Color();

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE_MINUS_DST_ALPHA, GL20.GL_ONE);

        TextureRegionComponent entityTextureRegionComponent = textureRegionMapper.get(entity);
        ShaderComponent shaderComponent = shaderMapper.get(entity);

        entityTextureRegionComponent.executeRefresh(entity);

        batchColor.set(batch.getColor());

        NormalMapRendering normalMapRendering = normalMapRenderingMapper.get(entity);
        if (normalMapRendering != null)
            normalMapRendering.useNormalMap = renderingType == RenderingType.NORMAL_MAP;

        if (entityTextureRegionComponent.isPolygon &&
                entityTextureRegionComponent.repeatablePolygonSprite != null &&
                (shaderComponent == null || shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN)) {
            drawRepeatablePolygonSprite(batch, entity, parentAlpha);
        } else {
            drawSprite(batch, entity, parentAlpha);
        }

        batch.setColor(batchColor);
    }

    @Override
    public void beginPipeline() {

    }

    @Override
    public void endPipeline() {

    }

    public void drawRepeatablePolygonSprite(Batch batch, int entity, float parentAlpha) {
        TintComponent tintComponent = tintMapper.get(entity);
        TextureRegionComponent textureRegionComponent = textureRegionMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);

        RepeatablePolygonSprite repeatablePolygonSprite = textureRegionComponent.repeatablePolygonSprite;
        boolean isRepeat = textureRegionComponent.isRepeat;

        float scaleX = entityTransformComponent.scaleX * (entityTransformComponent.flipX ? -1 : 1);
        float scaleY = entityTransformComponent.scaleY * (entityTransformComponent.flipY ? -1 : 1);
        repeatablePolygonSprite.setOrigin(entityTransformComponent.originX, entityTransformComponent.originY);
        repeatablePolygonSprite.setPosition(entityTransformComponent.x, entityTransformComponent.y);
        repeatablePolygonSprite.setRotation(entityTransformComponent.rotation);
        repeatablePolygonSprite.setScale(scaleX, scaleY);
        repeatablePolygonSprite.setColor(tintComponent.color.r, tintComponent.color.g, tintComponent.color.b, tintComponent.color.a * parentAlpha);
        repeatablePolygonSprite.setWrapType(isRepeat ? RepeatablePolygonSprite.WrapType.REPEAT : RepeatablePolygonSprite.WrapType.STRETCH);

        repeatablePolygonSprite.draw((PolygonSpriteBatch) batch);
    }

    public void drawSprite(Batch batch, int entity, float parentAlpha) {
        TintComponent tintComponent = tintMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);
        TextureRegionComponent entityTextureRegionComponent = textureRegionMapper.get(entity);
        DimensionsComponent dimensionsComponent = dimensionsMapper.get(entity);
        ShaderComponent shaderComponent = shaderMapper.get(entity);
        boolean screenReading = shaderComponent != null && shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN_READING;

        batch.setColor(tintComponent.color.r, tintComponent.color.g, tintComponent.color.b, tintComponent.color.a * parentAlpha);

        float scaleX = entityTransformComponent.scaleX * (entityTransformComponent.flipX ? -1 : 1);
        float scaleY = entityTransformComponent.scaleY * (entityTransformComponent.flipY ? -1 : 1);

        TextureRegion region = entityTextureRegionComponent.region;
        if (screenReading && entityTextureRegionComponent.screenRegion != null)
            region = entityTextureRegionComponent.screenRegion;
        batch.draw(region,
                entityTransformComponent.x, entityTransformComponent.y,
                entityTransformComponent.originX, entityTransformComponent.originY,
                dimensionsComponent.width, dimensionsComponent.height,
                scaleX, scaleY,
                entityTransformComponent.rotation);
    }
}
