package games.rednblack.editor.renderer.physics;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.physics.box2d.Fixture;

/**
 * Interface to handle Box2D physics contacts to be used with
 * {@link games.rednblack.editor.renderer.scripts.IScript}
 *
 * These methods are called during collisions
 */

public interface PhysicsContact {
    /**
     * Called when two bodies starts a collison
     *
     * @param contact Entity of the object which contact has begun
     * @param contactFixture Fixture of the object's body which contact has begun
     * @param ownFixture Fixture of this object
     */
    void beginContact(Entity contact, Fixture contactFixture, Fixture ownFixture);

    /**
     * Called when two bodies ends a collision
     *
     * @param contact Entity of the object which contact has end
     * @param contactFixture Fixture of the object's body which contact has end
     * @param ownFixture Fixture of this object
     */
    void endContact(Entity contact, Fixture contactFixture, Fixture ownFixture);
}
