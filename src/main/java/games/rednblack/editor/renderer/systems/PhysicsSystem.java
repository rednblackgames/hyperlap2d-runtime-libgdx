package games.rednblack.editor.renderer.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.physics.PhysicsContact;
import games.rednblack.editor.renderer.scripts.IScript;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class PhysicsSystem extends DetachableSystem implements ContactListener {

    public static int VELOCITY_ITERATIONS = 8;
    public static int POSITION_ITERATIONS = 3;
    public static float TIME_STEP = 1f / 60f;

    protected ComponentMapper<TransformComponent> transformComponentMapper = ComponentMapper.getFor(TransformComponent.class);

    private final World world;
    private boolean isPhysicsOn = true;
    private float accumulator = 0;

    public PhysicsSystem(World world) {
        super(Family.all(PhysicsBodyComponent.class).get());
        this.world = world;
        world.setContactListener(this);
    }

    @Override
    public void update(float deltaTime) {
        if (!isDetached())
            fixedPhysicStep(deltaTime);

        super.update(deltaTime);
    }

    /**
     * WARN Physics world isn't updated with a fixed time step.
     *
     * @param deltaTime time step passed directly to {@link World#step}
     */
    @Override
    public void manualUpdate(float deltaTime) {
        physicStep(deltaTime);

        super.manualUpdate(deltaTime);
    }

    private void fixedPhysicStep(float deltaTime) {
        if (world != null && isPhysicsOn) {
            float frameTime = Math.min(deltaTime, 0.25f); //avoid spiral of death
            accumulator += frameTime;
            while (accumulator >= TIME_STEP) {
                physicStep(TIME_STEP);
                accumulator -= TIME_STEP;
            }
        }
    }

    private void physicStep(float deltaTime) {
        world.step(deltaTime, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
    }

    /**
     * Iterate over all entities and interpolate {@link TransformComponent#x} and {@link TransformComponent#y}
     * to current {@link PhysicsBodyComponent#body} position
     *
     * @param alpha linear interpolation factor
     */
    public void interpolate(float alpha) {
        for (int i = 0; i < getEntities().size(); ++i) {
            interpolate(getEntities().get(i), alpha);
        }
    }

    /**
     * Interpolate {@link TransformComponent#x} and {@link TransformComponent#y}
     * to current {@link PhysicsBodyComponent#body} position
     *
     * @param entity Entity to interpolate
     * @param alpha linear interpolation factor
     */
    public void interpolate(Entity entity, float alpha) {
        PhysicsBodyComponent physicsBodyComponent = ComponentRetriever.get(entity, PhysicsBodyComponent.class);
        Body body = physicsBodyComponent.body;

        if (body == null)
            return;

        TransformComponent transformComponent = transformComponentMapper.get(entity);

        Transform transform = body.getTransform();
        Vector2 bodyPosition = transform.getPosition();
        bodyPosition.sub(transformComponent.originX, transformComponent.originY);
        float angle = (float) Math.toRadians(transformComponent.rotation);
        float bodyAngle = transform.getRotation();

        transformComponent.x = bodyPosition.x * alpha + transformComponent.x * (1.0f - alpha);
        transformComponent.y = bodyPosition.y * alpha + transformComponent.y * (1.0f - alpha);
        transformComponent.rotation = (float) Math.toDegrees(bodyAngle * alpha + angle * (1.0f - alpha));
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        processBody(entity);

        if (!isDetached())
            interpolate(entity, 1);
    }

    protected void processBody(Entity entity) {
        PhysicsBodyComponent physicsBodyComponent = ComponentRetriever.get(entity, PhysicsBodyComponent.class);
        PolygonComponent polygonComponent = ComponentRetriever.get(entity, PolygonComponent.class);

        physicsBodyComponent.setWorld(world);

        if (polygonComponent == null && physicsBodyComponent.body != null) {
            world.destroyBody(physicsBodyComponent.body);
            physicsBodyComponent.body = null;
        }

        if (physicsBodyComponent.body == null && polygonComponent != null) {
            physicsBodyComponent.scheduleRefresh();
        }

        physicsBodyComponent.executeRefresh(entity);
    }

    public void setPhysicsOn(boolean isPhysicsOn) {
        this.isPhysicsOn = isPhysicsOn;
    }

    private void processCollision(Contact contact, boolean in, boolean preSolve, boolean postSolve) {
        // Get both fixtures
        Fixture f1 = contact.getFixtureA();
        Fixture f2 = contact.getFixtureB();
        // Get both bodies
        Body b1 = f1.getBody();
        Body b2 = f2.getBody();

        // Get our objects that reference these bodies
        Object o1 = b1.getUserData();
        Object o2 = b2.getUserData();

        if (!(o1 instanceof Entity) || !(o2 instanceof Entity))
            return;

        // cast to entity
        Entity et1 = (Entity) o1;
        Entity et2 = (Entity) o2;
        // get script comp
        ScriptComponent ic1 = ComponentRetriever.get(et1, ScriptComponent.class);
        ScriptComponent ic2 = ComponentRetriever.get(et2, ScriptComponent.class);

        // cast script to contacts, if scripts implement contacts
        for (IScript sc : ic1.scripts) {
            if (sc instanceof PhysicsContact) {
                PhysicsContact ct = (PhysicsContact) sc;
                if (preSolve) {
                    ct.preSolve(et2, f2, f1, contact);
                } else if (postSolve) {
                    ct.postSolve(et2, f2, f1, contact);
                } else if (in) {
                    ct.beginContact(et2, f2, f1, contact);
                } else {
                    ct.endContact(et2, f2, f1, contact);
                }
            }
        }

        for (IScript sc : ic2.scripts) {
            if (sc instanceof PhysicsContact) {
                PhysicsContact ct = (PhysicsContact) sc;
                if (preSolve) {
                    ct.preSolve(et1, f1, f2, contact);
                } else if (postSolve) {
                    ct.postSolve(et1, f1, f2, contact);
                } else if (in) {
                    ct.beginContact(et1, f1, f2, contact);
                } else {
                    ct.endContact(et1, f1, f2, contact);
                }
            }
        }
    }

    @Override
    public void beginContact(Contact contact) {
        processCollision(contact, true, false, false);
    }

    @Override
    public void endContact(Contact contact) {
        processCollision(contact, false, false, false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        processCollision(contact, false, true, false);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        processCollision(contact, false, false, true);
    }
}
