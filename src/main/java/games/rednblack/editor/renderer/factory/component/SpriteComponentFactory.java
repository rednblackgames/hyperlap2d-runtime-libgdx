package games.rednblack.editor.renderer.factory.component;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;
import games.rednblack.editor.renderer.data.*;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class SpriteComponentFactory extends ComponentFactory {

    protected ComponentMapper<SpriteAnimationComponent> spriteAnimationCM;
    protected ComponentMapper<SpriteAnimationStateComponent> spriteAnimationStateCM;
    protected ComponentMapper<TextureRegionComponent> textureRegionCM;

    private final EntityTransmuter transmuter;

    public SpriteComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(TextureRegionComponent.class)
                .add(SpriteAnimationComponent.class)
                .add(SpriteAnimationStateComponent.class)
                .build();
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.SPRITE_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        spriteAnimationCM.get(entity).animationName = (String) data;
    }

    @Override
    public Class<SpriteAnimationVO> getVOType() {
        return SpriteAnimationVO.class;
    }

    @Override
    public void initializeSpecialComponentsFromVO(int entity, MainItemVO voG) {
        SpriteAnimationVO vo = (SpriteAnimationVO) voG;
        SpriteAnimationComponent spriteAnimationComponent = spriteAnimationCM.get(entity);
        spriteAnimationComponent.animationName = vo.animationName;

        for (int i = 0; i < vo.frameRangeMap.size(); i++) {
            spriteAnimationComponent.frameRangeMap.put(vo.frameRangeMap.get(i).name, vo.frameRangeMap.get(i));
        }
        spriteAnimationComponent.fps = vo.fps;
        spriteAnimationComponent.currentAnimation = vo.currentAnimation;

        if (vo.playMode == 0) spriteAnimationComponent.playMode = Animation.PlayMode.NORMAL;
        if (vo.playMode == 1) spriteAnimationComponent.playMode = Animation.PlayMode.REVERSED;
        if (vo.playMode == 2) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP;
        if (vo.playMode == 3) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP_REVERSED;
        if (vo.playMode == 4) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP_PINGPONG;
        if (vo.playMode == 5) spriteAnimationComponent.playMode = Animation.PlayMode.LOOP_RANDOM;
        if (vo.playMode == 6) spriteAnimationComponent.playMode = Animation.PlayMode.NORMAL;
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        SpriteAnimationComponent spriteAnimationComponent = spriteAnimationCM.get(entity);

        // filtering regions by name
        Array<TextureAtlas.AtlasRegion> regions = rm.getSpriteAnimation(spriteAnimationComponent.animationName);
        SpriteAnimationStateComponent stateComponent = spriteAnimationStateCM.get(entity);
        stateComponent.setAllRegions(regions);

        if (spriteAnimationComponent.frameRangeMap.isEmpty()) {
            spriteAnimationComponent.frameRangeMap.put("Default", new FrameRange("Default", 0, regions.size - 1));
        } else {
            FrameRange defaultRange = spriteAnimationComponent.frameRangeMap.get("Default");
            defaultRange.endFrame = regions.size - 1;
        }

        if (spriteAnimationComponent.currentAnimation == null) {
            spriteAnimationComponent.currentAnimation = (String) spriteAnimationComponent.frameRangeMap.keySet().toArray()[0];
        }

        if (spriteAnimationComponent.playMode == null) {
            spriteAnimationComponent.playMode = Animation.PlayMode.LOOP;
        }

        stateComponent.set(spriteAnimationComponent);

        TextureRegionComponent textureRegionComponent = textureRegionCM.get(entity);
        engine.inject(textureRegionComponent);
        textureRegionComponent.region = regions.get(0);
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        DimensionsComponent dimension = dimensionsCM.get(entity);
        SpriteAnimationComponent spriteAnimationComponent = spriteAnimationCM.get(entity);

        Array<TextureAtlas.AtlasRegion> regions = rm.getSpriteAnimation(spriteAnimationComponent.animationName);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);
        dimension.width = (float) regions.get(0).getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        dimension.height = (float) regions.get(0).getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;
    }
}
