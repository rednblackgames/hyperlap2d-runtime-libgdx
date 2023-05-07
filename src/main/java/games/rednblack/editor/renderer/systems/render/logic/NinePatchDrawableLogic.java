package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.ComponentMapper;
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

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        TintComponent tintComponent = tintComponentComponentMapper.get(entity);
        TransformComponent entityTransformComponent = transformMapper.get(entity);
        DimensionsComponent entityDimensionsComponent = dimensionsMapper.get(entity);
        NinePatchComponent entityNinePatchComponent = ninePatchMapper.get(entity);
        batch.setColor(tintComponent.color);

        entityNinePatchComponent.ninePatch.draw(batch, entityTransformComponent.x, entityTransformComponent.y,
                entityTransformComponent.originX, entityTransformComponent.originY,
                entityDimensionsComponent.width, entityDimensionsComponent.height,
                entityTransformComponent.scaleX, entityTransformComponent.scaleY, entityTransformComponent.rotation);
    }

    @Override
    public void beginPipeline() {

    }

    @Override
    public void endPipeline() {

    }

}
