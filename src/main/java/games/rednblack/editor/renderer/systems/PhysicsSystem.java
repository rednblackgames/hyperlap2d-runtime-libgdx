package games.rednblack.editor.renderer.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import games.rednblack.editor.renderer.components.shape.CircleShapeComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.physics.PhysicsBodyLoader;
import games.rednblack.editor.renderer.physics.PhysicsContact;
import games.rednblack.editor.renderer.scripts.IScript;
import games.rednblack.editor.renderer.systems.strategy.InterpolationSystem;

@All(PhysicsBodyComponent.class)
public class PhysicsSystem extends BaseEntitySystem implements ContactListener, InterpolationSystem {

    public static int VELOCITY_ITERATIONS = 8;
    public static int POSITION_ITERATIONS = 3;

    protected ComponentMapper<TransformComponent> transformComponentMapper;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyComponentMapper;
    protected ComponentMapper<PolygonShapeComponent> polygonComponentMapper;
    protected ComponentMapper<CircleShapeComponent> circleShapeComponentMapper;
    protected ComponentMapper<ScriptComponent> scriptComponentMapper;

    private World world;
    private boolean isPhysicsOn = true;

    public void setBox2DWorld(World world) {
        this.world = world;
        world.setContactListener(this);
    }

    @Override
    protected final void processSystem() {
        IntBag actives = subscription.getEntities();
        int[] ids = actives.getData();
        for (int i = 0, s = actives.size(); s > i; i++) {
            //Update previous state
            interpolate(ids[i], 1f);
            process(ids[i]);
        }

        if (world != null && isPhysicsOn)
            world.step(getWorld().getDelta(), VELOCITY_ITERATIONS, POSITION_ITERATIONS);
    }

    /**
     * Iterate over all entities and interpolate {@link TransformComponent#x} and {@link TransformComponent#y}
     * to current {@link PhysicsBodyComponent#body} position
     *
     * @param alpha linear interpolation factor
     */
    //@Override
    public void interpolate(float alpha) {
        IntBag bag = subscription.getEntities();
        for (int i = 0, s = bag.size(); i < s; ++i) {
            interpolate(bag.get(i), alpha);
        }
    }

    /**
     * Interpolate {@link TransformComponent#x} and {@link TransformComponent#y}
     * to current {@link PhysicsBodyComponent#body} position
     *
     * @param entity Entity to interpolate
     * @param alpha  linear interpolation factor
     */
    public void interpolate(int entity, float alpha) {
        if (!isPhysicsOn)
            return;

        PhysicsBodyComponent physicsBodyComponent = physicsBodyComponentMapper.get(entity);
        Body body = physicsBodyComponent.body;

        if (body == null)
            return;

        TransformComponent transformComponent = transformComponentMapper.get(entity);

        Transform transform = body.getTransform();
        Vector2 bodyPosition = transform.getPosition();
        bodyPosition.sub(transformComponent.originX, transformComponent.originY);
        float angle = transformComponent.rotation;
        float bodyAngle = transform.getRotation();

        transformComponent.x = bodyPosition.x * alpha + transformComponent.x * (1.0f - alpha);
        transformComponent.y = bodyPosition.y * alpha + transformComponent.y * (1.0f - alpha);

        float cs = (1.0f - alpha) * MathUtils.cosDeg(angle) + alpha * MathUtils.cos(bodyAngle);
        float sn = (1.0f - alpha) * MathUtils.sinDeg(angle) + alpha * MathUtils.sin(bodyAngle);

        transformComponent.rotation = MathUtils.atan2(sn, cs) * MathUtils.radiansToDegrees;
    }

    protected void process(int entity) {
        PhysicsBodyComponent physicsBodyComponent = physicsBodyComponentMapper.get(entity);
        PolygonShapeComponent polygonShapeComponent = polygonComponentMapper.get(entity);
        CircleShapeComponent circleShapeComponent = circleShapeComponentMapper.get(entity);

        TransformComponent transformComponent = transformComponentMapper.get(entity);

        if ((polygonShapeComponent == null || polygonShapeComponent.vertices == null) && circleShapeComponent == null && physicsBodyComponent.body != null) {
            world.destroyBody(physicsBodyComponent.body);
            physicsBodyComponent.body = null;
            physicsBodyComponent.clearFixturesMap();
        }

        if (physicsBodyComponent.body == null && ((polygonShapeComponent != null && polygonShapeComponent.vertices != null) || circleShapeComponent != null)) {
            physicsBodyComponent.centerX = transformComponent.originX;
            physicsBodyComponent.centerY = transformComponent.originY;

            PhysicsBodyLoader.getInstance().createBody(world, entity, physicsBodyComponent, transformComponent, getWorld());
            physicsBodyComponent.body.setUserData(entity);
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

        if (!(o1 instanceof Integer) || !(o2 instanceof Integer))
            return;

        // cast to entity
        int et1 = (int) o1;
        int et2 = (int) o2;
        // get script comp
        ScriptComponent ic1 = scriptComponentMapper.get(et1);
        ScriptComponent ic2 = scriptComponentMapper.get(et2);

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
