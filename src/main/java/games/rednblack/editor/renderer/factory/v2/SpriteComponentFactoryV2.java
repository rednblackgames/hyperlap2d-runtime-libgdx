package games.rednblack.editor.renderer.factory.v2;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;
import games.rednblack.editor.renderer.data.FrameRange;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class SpriteComponentFactoryV2 extends ComponentFactoryV2 {

    protected ComponentMapper<SpriteAnimationComponent> spriteAnimationCM;
    protected ComponentMapper<SpriteAnimationStateComponent> spriteAnimationStateCM;
    protected ComponentMapper<TextureRegionComponent> textureRegionCM;

    private final EntityTransmuter transmuter;

    public SpriteComponentFactoryV2(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
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
        return EntityFactoryV2.SPRITE_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        spriteAnimationCM.get(entity).animationName = (String) data;
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
