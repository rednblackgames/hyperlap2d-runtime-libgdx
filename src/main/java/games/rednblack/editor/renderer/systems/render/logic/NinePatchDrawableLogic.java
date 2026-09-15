package games.rednblack.editor.renderer.systems.render.logic;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.tenpatch.TenPatchDrawable;

public class NinePatchDrawableLogic implements DrawableLogic {

    protected ComponentMapper<TintComponent> tintComponentComponentMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<NinePatchComponent> ninePatchMapper;
    protected ComponentMapper<TextureRegionComponent> textureRegionMapper;

    private final Color batchColor = new Color();

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE_MINUS_DST_ALPHA, GL20.GL_ONE);
        batchColor.set(batch.getColor());

        TintComponent tintComponent = tintComponentComponentMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);
        DimensionsComponent entityDimensionsComponent = dimensionsMapper.get(entity);
        NinePatchComponent entityNinePatchComponent = ninePatchMapper.get(entity);
        batch.setColor(tintComponent.color);
        batch.getColor().a *= parentAlpha;

        TenPatchDrawable tenPatch = entityNinePatchComponent.tenPatch;

        // animated 9-patches carry the frame picked by SpriteAnimationSystem in their texture region component
        if (textureRegionMapper.has(entity)) {
            TextureRegionComponent textureRegionComponent = textureRegionMapper.get(entity);
            if (textureRegionComponent.region != null && textureRegionComponent.region != tenPatch.getRegion()) {
                tenPatch.setRegion(textureRegionComponent.region);
            }
        }

        tenPatch.draw(batch, entityTransformComponent.x, entityTransformComponent.y,
                entityTransformComponent.originX, entityTransformComponent.originY,
                entityDimensionsComponent.width, entityDimensionsComponent.height,
                entityTransformComponent.scaleX, entityTransformComponent.scaleY, entityTransformComponent.rotation);

        batch.setColor(batchColor);
    }

    @Override
    public void beginPipeline() {

    }

    @Override
    public void endPipeline() {

    }

}
