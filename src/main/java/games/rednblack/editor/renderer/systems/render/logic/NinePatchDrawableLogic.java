package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;

public class NinePatchDrawableLogic implements DrawableLogic {

    protected ComponentMapper<TintComponent> tintComponentComponentMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<NinePatchComponent> ninePatchMapper;

    private final Color batchColor = new Color();

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        batchColor.set(batch.getColor());

        TintComponent tintComponent = tintComponentComponentMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);
        DimensionsComponent entityDimensionsComponent = dimensionsMapper.get(entity);
        NinePatchComponent entityNinePatchComponent = ninePatchMapper.get(entity);
        batch.setColor(tintComponent.color);
        batch.getColor().a *= parentAlpha;

        entityNinePatchComponent.ninePatch.draw(batch, entityTransformComponent.x, entityTransformComponent.y,
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
