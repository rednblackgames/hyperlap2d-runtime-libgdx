package games.rednblack.hyperrunner.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.physics.box2d.Body;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;
import games.rednblack.hyperrunner.component.PlayerComponent;

@All(PlayerComponent.class)
public class PlayerAnimationSystem extends IteratingSystem {

    protected ComponentMapper<ParentNodeComponent> parentNodeCM;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    protected ComponentMapper<PlayerComponent> playerCM;
    protected ComponentMapper<SpriteAnimationComponent> spriteAnimationCM;
    protected ComponentMapper<SpriteAnimationStateComponent> spriteAnimationStateCM;
    protected ComponentMapper<TransformComponent> transformCM;

    @Override
    protected void process(int entity) {
        ParentNodeComponent nodeComponent = parentNodeCM.get(entity);
        Body body = physicsBodyCM.get(nodeComponent.parentEntity).body;

        PlayerComponent playerComponent = playerCM.get(entity);
        SpriteAnimationComponent spriteAnimationComponent = spriteAnimationCM.get(entity);
        SpriteAnimationStateComponent spriteAnimationStateComponent = spriteAnimationStateCM.get(entity);
        TransformComponent transformComponent = transformCM.get(entity);

        if (Math.abs(body.getLinearVelocity().x) > 0.1f) {
            spriteAnimationComponent.playMode = Animation.PlayMode.LOOP;

            spriteAnimationComponent.currentAnimation = "run";
            spriteAnimationComponent.fps = Math.max(6, (int) Math.abs(body.getLinearVelocity().x) * 3);

            transformComponent.flipX = body.getLinearVelocity().x < 0;
        } else if (playerComponent.touchedPlatforms > 0) {
            spriteAnimationComponent.playMode = Animation.PlayMode.LOOP;

            spriteAnimationComponent.currentAnimation = "idle";
        }

        if (body.getLinearVelocity().y > 0.2f) {
            spriteAnimationComponent.currentAnimation = "jumpUp";
            spriteAnimationComponent.playMode = Animation.PlayMode.NORMAL;
        } else if (body.getLinearVelocity().y < -0.2f) {
            spriteAnimationComponent.currentAnimation = "jumpUp";
            spriteAnimationComponent.playMode = Animation.PlayMode.REVERSED;
        }

        spriteAnimationStateComponent.set(spriteAnimationComponent);
    }
}
