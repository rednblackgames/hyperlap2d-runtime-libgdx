package games.rednblack.editor.renderer.systems.render.logic;


import com.artemis.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;

public class LightDrawableLogic implements Drawable {

    protected ComponentMapper<LightObjectComponent> lightComponentMapper;
    protected ComponentMapper<TintComponent> tintComponentMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeComponentComponentMapper;

    private final Color tmpColor = new Color();

    @Override
    public void draw(Batch batch, int entity, float parentAlpha, RenderingType renderingType) {
        LightObjectComponent lightObjectComponent = lightComponentMapper.get(entity);
        TintComponent tint = tintComponentMapper.get(entity);

        tmpColor.set(tint.color);
        tmpColor.a *= tintComponentMapper.get(parentNodeComponentComponentMapper.get(entity).parentEntity).color.a;

        lightObjectComponent.lightObject.setColor(tmpColor);
    }

}