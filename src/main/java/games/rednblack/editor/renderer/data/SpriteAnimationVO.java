package games.rednblack.editor.renderer.data;

import com.artemis.World;
import com.badlogic.gdx.graphics.g2d.Animation;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.ArrayList;

public class SpriteAnimationVO extends MainItemVO {

    public String animationName = "";
    public int fps = 24;
    public String currentAnimation;
    public ArrayList<FrameRange> frameRangeMap = new ArrayList<FrameRange>();
    public int playMode = 0;

    public SpriteAnimationVO() {

    }

    public SpriteAnimationVO(SpriteAnimationVO vo) {
        super(vo);
        animationName = vo.animationName;
        fps = vo.fps;
        frameRangeMap = vo.frameRangeMap;
    }

    @Override
    public void loadFromEntity(int entity, World engine, EntityFactory entityFactory) {
        super.loadFromEntity(entity, engine, entityFactory);

        SpriteAnimationComponent spriteAnimationComponent = ComponentRetriever.get(entity, SpriteAnimationComponent.class, engine);
        animationName = spriteAnimationComponent.animationName;
        fps = spriteAnimationComponent.fps;
        frameRangeMap = new ArrayList<FrameRange>();
        for(FrameRange fr: spriteAnimationComponent.frameRangeMap.values()) {
            frameRangeMap.add(fr);
        }
        currentAnimation = spriteAnimationComponent.currentAnimation;
        if(spriteAnimationComponent.playMode == Animation.PlayMode.NORMAL) playMode = 0;
        if(spriteAnimationComponent.playMode == Animation.PlayMode.REVERSED) playMode = 1;
        if(spriteAnimationComponent.playMode == Animation.PlayMode.LOOP) playMode = 2;
        if(spriteAnimationComponent.playMode == Animation.PlayMode.LOOP_REVERSED) playMode = 3;
        if(spriteAnimationComponent.playMode == Animation.PlayMode.LOOP_PINGPONG) playMode = 4;
        if(spriteAnimationComponent.playMode == Animation.PlayMode.LOOP_RANDOM) playMode = 5;
        if(spriteAnimationComponent.playMode == Animation.PlayMode.NORMAL) playMode = 6;
    }

    @Override
    public String getResourceName() {
        return animationName;
    }
}
