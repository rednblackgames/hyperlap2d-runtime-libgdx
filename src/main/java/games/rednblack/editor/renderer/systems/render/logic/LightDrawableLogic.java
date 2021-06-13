package games.rednblack.editor.renderer.systems.render.logic;


import com.artemis.BaseComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class LightDrawableLogic implements Drawable {

    private BaseComponentMapper<LightObjectComponent> lightComponentMapper;
    private BaseComponentMapper<TintComponent> tintComponentMapper;
    private BaseComponentMapper<ParentNodeComponent> parentNodeComponentComponentMapper;

    private final Color tmpColor = new Color();

    public void init() {
        lightComponentMapper = ComponentRetriever.getMapper(LightObjectComponent.class);
        tintComponentMapper = ComponentRetriever.getMapper(TintComponent.class);
        parentNodeComponentComponentMapper = ComponentRetriever.getMapper(ParentNodeComponent.class);
    }

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        if(lightComponentMapper==null) init(); // TODO: Can we have an injection for this object?

        LightObjectComponent lightObjectComponent = lightComponentMapper.get(entity);
        TintComponent tint = tintComponentMapper.get(entity);

        tmpColor.set(tint.color);
        tmpColor.a *= tintComponentMapper.get(parentNodeComponentComponentMapper.get(entity).parentEntity).color.a;

        lightObjectComponent.lightObject.setColor(tmpColor);
    }

}