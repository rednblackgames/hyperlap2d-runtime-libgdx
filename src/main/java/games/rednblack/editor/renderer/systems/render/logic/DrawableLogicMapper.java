package games.rednblack.editor.renderer.systems.render.logic;

import com.artemis.World;
import com.badlogic.gdx.utils.IntMap;
import games.rednblack.editor.renderer.factory.EntityFactory;

public class DrawableLogicMapper {

    private final IntMap<DrawableLogic> logicClassMap = new IntMap<>();

    public DrawableLogicMapper() {
        logicClassMap.put(EntityFactory.IMAGE_TYPE, new TextureRegionDrawLogic());
        logicClassMap.put(EntityFactory.LABEL_TYPE, new LabelDrawableLogic());
        logicClassMap.put(EntityFactory.NINE_PATCH, new NinePatchDrawableLogic());
        logicClassMap.put(EntityFactory.PARTICLE_TYPE, new ParticleDrawableLogic());
        logicClassMap.put(EntityFactory.SPRITE_TYPE, new SpriteDrawableLogic());
        logicClassMap.put(EntityFactory.COLOR_PRIMITIVE, new TextureRegionDrawLogic());
        logicClassMap.put(EntityFactory.LIGHT_TYPE, new LightDrawableLogic());
    }

    public void addDrawableToMap(int type, DrawableLogic drawableLogic) {
        logicClassMap.put(type, drawableLogic);
    }

    public DrawableLogic getDrawable(int type) {
        return logicClassMap.get(type);
    }

    public void injectMappers(World engine) {
        for (DrawableLogic value : logicClassMap.values()) engine.inject(value);
    }

    public void beginPipeline() {
        for (DrawableLogic value : logicClassMap.values()) value.beginPipeline();
    }

    public void endPipeline() {
        for (DrawableLogic value : logicClassMap.values()) value.endPipeline();
    }
}
