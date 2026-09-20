package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.Animation;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;

@All(SpriteAnimationComponent.class)
public class SpriteAnimationSystem extends IteratingSystem {
    protected ComponentMapper<TextureRegionComponent> tm;
    protected ComponentMapper<SpriteAnimationStateComponent> sm;
    protected ComponentMapper<SpriteAnimationComponent> sa;

    @Override
    protected void process(int entityId) {
        TextureRegionComponent tex = tm.get(entityId);
        SpriteAnimationStateComponent state = sm.get(entityId);
        state.currentAnimation.setFrameDuration(1f / sa.get(entityId).fps);
        tex.region = state.currentAnimation.getKeyFrame(state.time);

        if (state.paused) return;

        state.time += engine.getDelta();

        // an animation waiting its turn takes over the moment the one playing is through
        if (state.queue.size > 0 && state.currentAnimation.isAnimationFinished(state.time)) {
            SpriteAnimationComponent animation = sa.get(entityId);
            String next = state.queue.removeIndex(0);
            boolean last = state.queue.size == 0;
            state.set(animation.frameRangeMap.get(next), animation.fps,
                    last ? animation.playMode : Animation.PlayMode.NORMAL);
        }
    }
}
