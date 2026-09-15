package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.tenpatch.TenPatchUtils;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.ArrayList;

public class Image9patchVO extends MainItemVO {

    /** Name of an image region, or of a sprite animation for an animated 9-patch. */
    public String imageName = "";
    public float width = 0;
    public float height = 0;

    /** Playback of an animated 9-patch, the same data a {@link SpriteAnimationVO} carries. Ignored for still images. */
    public int fps = 24;
    public String currentAnimation;
    public ArrayList<FrameRange> frameRangeMap = new ArrayList<>();
    /** Play mode as in {@link SpriteAnimationVO#playMode}: 0 normal, 1 reversed, 2 loop, 3 loop reversed, 4 ping pong, 5 random. */
    public int playMode = 2;

    public Image9patchVO() {
        super();
    }

    public Image9patchVO(Image9patchVO vo) {
        super(vo);
        imageName = new String(vo.imageName);
        width = vo.width;
        height = vo.height;
        fps = vo.fps;
        currentAnimation = vo.currentAnimation;
        frameRangeMap = new ArrayList<>(vo.frameRangeMap);
        playMode = vo.playMode;
    }

    @Override
    public void loadFromEntity(int entity, Engine engine, EntityFactory entityFactory) {
        super.loadFromEntity(entity, engine, entityFactory);

        NinePatchComponent ninePatchComponent = ComponentRetriever.get(entity, NinePatchComponent.class, engine);
        DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class, engine);
        imageName = ninePatchComponent.textureRegionName;

        width = dimensionsComponent.width;
        height = dimensionsComponent.height;

        SpriteAnimationComponent spriteAnimationComponent = ComponentRetriever.get(entity, SpriteAnimationComponent.class, engine);
        frameRangeMap = new ArrayList<>();
        if (spriteAnimationComponent != null) {
            fps = spriteAnimationComponent.fps;
            frameRangeMap.addAll(spriteAnimationComponent.frameRangeMap.values());
            currentAnimation = spriteAnimationComponent.currentAnimation;
            playMode = TenPatchUtils.playModeToInt(spriteAnimationComponent.playMode);
        }
    }

    /** True when this 9-patch plays a sprite animation. */
    public boolean isAnimated() {
        return currentAnimation != null || !frameRangeMap.isEmpty();
    }

    @Override
    public String getResourceName() {
        return imageName;
    }
}
