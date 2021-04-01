package games.rednblack.editor.renderer.physics;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;

/**
 * Interface to handle Box2D physics contacts to be used with
 * {@link games.rednblack.editor.renderer.scripts.IScript}
 *
 * These methods are called during collisions
 */

public interface PhysicsContact {
    /**
     * Called when two bodies starts a collision
     *
     * @param contactEntity Entity of the object which contact has begun
     * @param contactFixture Fixture of the object's body which contact has begun
     * @param ownFixture Fixture of this object
     * @param contact Box2D Contact data
     */
    void beginContact(Entity contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact);

    /**
     * Called when two bodies ends a collision
     *
     * @param contactEntity Entity of the object which contact has end
     * @param contactFixture Fixture of the object's body which contact has end
     * @param ownFixture Fixture of this object
     * @param contact Box2D Contact data
     */
    void endContact(Entity contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact);

    /**
     * Called when two bodies overlap but not collides yet, useful when custom
     * collisions have to be applied in some cases. A classic example of this is the one-way wall or platform,
     * where the player is able to pass through an otherwise solid object.
     *
     * @param contactEntity Entity of the object which contact has begun
     * @param contactFixture Fixture of the object's body which contact has begun
     * @param ownFixture Fixture of this object
     * @param contact Box2D Contact data
     */
    void preSolve(Entity contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact);

    /**
     * Called after a collision has been calculated and applied.
     *
     * @param contactEntity Entity of the object which contact has end
     * @param contactFixture Fixture of the object's body which contact has end
     * @param ownFixture Fixture of this object
     * @param contact Box2D Contact data
     */
    void postSolve(Entity contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact);
}
