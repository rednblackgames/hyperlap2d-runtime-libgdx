package games.rednblack.editor.renderer.factory.component;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EntityEdit;
import games.rednblack.editor.renderer.ecs.EntityTransmuter;
import games.rednblack.editor.renderer.ecs.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;
import games.rednblack.editor.renderer.data.FrameRange;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.data.Image9patchVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.tenpatch.TenPatchUtils;

/**
 * Builds 9-patch entities. When the image name is a sprite animation the entity also gets the sprite
 * animation components, so {@code SpriteAnimationSystem} drives its frames exactly like a sprite entity
 * and the same ranges, fps and play mode apply; the drawable then follows the region of the
 * {@link TextureRegionComponent}.
 */
public class NinePatchComponentFactory extends ComponentFactory {
    protected ComponentMapper<NinePatchComponent> ninePatchCM;
    protected ComponentMapper<SpriteAnimationComponent> spriteAnimationCM;
    protected ComponentMapper<SpriteAnimationStateComponent> spriteAnimationStateCM;
    protected ComponentMapper<TextureRegionComponent> textureRegionCM;

    private final EntityTransmuter transmuter;

    public NinePatchComponentFactory(Engine engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(NinePatchComponent.class)
                .build();
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.NINE_PATCH;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        String name = (String) data;
        ninePatchCM.get(entity).textureRegionName = name;
        if (TenPatchUtils.isAnimation(rm, name)) {
            addAnimationComponents(entity).animationName = name;
        }
    }

    @Override
    public Class<Image9patchVO> getVOType() {
        return Image9patchVO.class;
    }

    @Override
    public void initializeSpecialComponentsFromVO(int entity, MainItemVO voG) {
        Image9patchVO vo = (Image9patchVO) voG;
        DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);
        dimensionsComponent.height = vo.height;
        dimensionsComponent.width = vo.width;

        NinePatchComponent ninePatchComponent = ninePatchCM.get(entity);
        ninePatchComponent.textureRegionName = vo.imageName;

        if (TenPatchUtils.isAnimation(rm, vo.imageName)) {
            SpriteAnimationComponent spriteAnimationComponent = addAnimationComponents(entity);
            spriteAnimationComponent.animationName = vo.imageName;
            for (int i = 0; i < vo.frameRangeMap.size(); i++) {
                spriteAnimationComponent.frameRangeMap.put(vo.frameRangeMap.get(i).name, vo.frameRangeMap.get(i));
            }
            spriteAnimationComponent.fps = vo.fps;
            spriteAnimationComponent.currentAnimation = vo.currentAnimation;
            spriteAnimationComponent.playMode = TenPatchUtils.playModeFromInt(vo.playMode);
        }
    }

    private SpriteAnimationComponent addAnimationComponents(int entity) {
        EntityEdit edit = engine.edit(entity);
        SpriteAnimationComponent spriteAnimationComponent = edit.create(SpriteAnimationComponent.class);
        edit.create(SpriteAnimationStateComponent.class);
        edit.create(TextureRegionComponent.class);
        return spriteAnimationComponent;
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        NinePatchComponent ninePatchComponent = ninePatchCM.get(entity);
        createNinePatchComponent(ninePatchComponent, ninePatchComponent.textureRegionName);

        if (spriteAnimationCM.has(entity)) {
            initializeAnimation(entity);
        }
    }

    /** Same setup {@code SpriteComponentFactory} does for a sprite entity. */
    private void initializeAnimation(int entity) {
        SpriteAnimationComponent spriteAnimationComponent = spriteAnimationCM.get(entity);
        Array<TextureAtlas.AtlasRegion> regions = rm.getSpriteAnimation(spriteAnimationComponent.animationName);
        SpriteAnimationStateComponent stateComponent = spriteAnimationStateCM.get(entity);
        stateComponent.setAllRegions(regions);

        if (spriteAnimationComponent.frameRangeMap.isEmpty()) {
            spriteAnimationComponent.frameRangeMap.put("Default", new FrameRange("Default", 0, regions.size - 1));
        } else {
            FrameRange defaultRange = spriteAnimationComponent.frameRangeMap.get("Default");
            if (defaultRange == null) {
                spriteAnimationComponent.frameRangeMap.put("Default", new FrameRange("Default", 0, regions.size - 1));
            } else {
                defaultRange.endFrame = regions.size - 1;
            }
        }

        if (spriteAnimationComponent.currentAnimation == null
                || !spriteAnimationComponent.frameRangeMap.containsKey(spriteAnimationComponent.currentAnimation)) {
            spriteAnimationComponent.currentAnimation = "Default";
        }

        if (spriteAnimationComponent.playMode == null) {
            spriteAnimationComponent.playMode = com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP;
        }

        stateComponent.set(spriteAnimationComponent);

        TextureRegionComponent textureRegionComponent = textureRegionCM.get(entity);
        engine.inject(textureRegionComponent);
        textureRegionComponent.regionName = spriteAnimationComponent.animationName;
        textureRegionComponent.region = regions.get(0);
    }

    private void createNinePatchComponent(NinePatchComponent component, String imageName) {
        component.tenPatch = TenPatchUtils.createDrawable(rm, imageName);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        TenPatchUtils.scaleDrawable(component.tenPatch, multiplier / projectInfoVO.pixelToWorld, multiplier / projectInfoVO.pixelToWorld);
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        NinePatchComponent ninePatchComponent = ninePatchCM.get(entity);
        TextureRegion region = TenPatchUtils.resolveRegion(rm, ninePatchComponent.textureRegionName);
        if (region == null) region = rm.getTextureRegion(ninePatchComponent.textureRegionName);

        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        DimensionsComponent component = dimensionsCM.get(entity);
        if (component.width == 0) {
            component.width = (float) region.getRegionWidth() * multiplier / projectInfoVO.pixelToWorld;
        }

        if (component.height == 0) {
            component.height = (float) region.getRegionHeight() * multiplier / projectInfoVO.pixelToWorld;
        }
    }
}
