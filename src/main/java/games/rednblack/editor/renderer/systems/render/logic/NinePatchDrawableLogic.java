package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.BaseComponentMapper;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class NinePatchDrawableLogic implements Drawable {

    private BaseComponentMapper<TintComponent> tintComponentComponentMapper;
    private BaseComponentMapper<TransformComponent> transformMapper;
    private BaseComponentMapper<DimensionsComponent> dimensionsMapper;
    private BaseComponentMapper<NinePatchComponent> ninePatchMapper;


    public void init() {
        tintComponentComponentMapper = ComponentRetriever.getMapper(TintComponent.class);
        transformMapper = ComponentRetriever.getMapper(TransformComponent.class);
        dimensionsMapper = ComponentRetriever.getMapper(DimensionsComponent.class);
        ninePatchMapper = ComponentRetriever.getMapper(NinePatchComponent.class);
    }

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        if(tintComponentComponentMapper==null) init(); // TODO: Can we have an injection for this object?

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

}
