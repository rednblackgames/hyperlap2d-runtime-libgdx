package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
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

        if (!state.paused) {
            state.time += world.delta;
        }
    }
}
