package games.rednblack.editor.renderer.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.shape.CircleShapeComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.physics.PhysicsBodyLoader;
import games.rednblack.editor.renderer.physics.PhysicsContact;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;
import games.rednblack.editor.renderer.systems.strategy.FixedTimestep;
import games.rednblack.editor.renderer.systems.strategy.InterpolationSystem;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

@FixedTimestep
@All(PhysicsBodyComponent.class)
public class PhysicsSystem extends BaseEntitySystem implements ContactListener, InterpolationSystem {

    public static boolean enableInterpolation = true;
    public static int VELOCITY_ITERATIONS = 8;
    public static int POSITION_ITERATIONS = 3;

    private final Vector2 tempPos = new Vector2(0, 0);

    protected ComponentMapper<TransformComponent> transformComponentMapper;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyComponentMapper;
    protected ComponentMapper<PolygonShapeComponent> polygonComponentMapper;
    protected ComponentMapper<CircleShapeComponent> circleShapeComponentMapper;
    protected ComponentMapper<ScriptComponent> scriptComponentMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeComponentMapper;
    protected ComponentMapper<ViewPortComponent> viewPortComponentMapper;

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
    @Override
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

        float activeAlpha = enableInterpolation ? alpha : 1;

        PhysicsBodyComponent physicsBodyComponent = physicsBodyComponentMapper.get(entity);
        Body body = physicsBodyComponent.body;

        if (body == null) return;

        TransformComponent transformComponent = transformComponentMapper.get(entity);

        Vector2 currentPos = body.getPosition();
        float worldInterpX = physicsBodyComponent.prevX * (1.0f - activeAlpha) + currentPos.x * activeAlpha;
        float worldInterpY = physicsBodyComponent.prevY * (1.0f - activeAlpha) + currentPos.y * activeAlpha;

        float currentAngle = body.getTransform().getRotation();
        float worldInterpAngleRad = MathUtils.lerpAngle(physicsBodyComponent.prevAngle, currentAngle, activeAlpha);
        float worldInterpAngleDeg = worldInterpAngleRad * MathUtils.radiansToDegrees;

        ParentNodeComponent parentNode = parentNodeComponentMapper.get(entity);
        int parentEntity = (parentNode != null) ? parentNode.parentEntity : -1;
        ParentNodeComponent rootParentNode = (parentEntity != -1) ? parentNodeComponentMapper.get(parentEntity) : null;

        if (rootParentNode != null) {
            tempPos.set(worldInterpX, worldInterpY);

            TransformMathUtils.sceneToLocalCoordinates(parentEntity, tempPos, transformComponentMapper, parentNodeComponentMapper);

            transformComponent.x = tempPos.x - transformComponent.originX;
            transformComponent.y = tempPos.y - transformComponent.originY;

            float parentWorldRotation = TransformMathUtils.localToSceneRotation(parentEntity, transformComponentMapper, parentNodeComponentMapper);
            float localRotation = worldInterpAngleDeg - parentWorldRotation;
            transformComponent.rotation = normalizeAngle(localRotation);
        } else {
            transformComponent.x = worldInterpX - transformComponent.originX;
            transformComponent.y = worldInterpY - transformComponent.originY;

            transformComponent.rotation = worldInterpAngleDeg;
        }
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
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
            PhysicsBodyLoader.getInstance().createBody(world, entity, physicsBodyComponent, transformComponent, getWorld());
            physicsBodyComponent.body.setUserData(entity);
        }

        physicsBodyComponent.executeRefresh(entity);

        if (physicsBodyComponent.body != null) {
            Vector2 position = physicsBodyComponent.body.getPosition();
            physicsBodyComponent.prevX = position.x;
            physicsBodyComponent.prevY = position.y;
            physicsBodyComponent.prevAngle = physicsBodyComponent.body.getTransform().getRotation();
        }
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
        if (ic1 == null || ic2 == null) return;

        // cast script to contacts, if scripts implement contacts
        for (IScript sc : ic1.scripts) {
            if (sc instanceof PhysicsContact) {
                if (sc instanceof BasicScript) {
                    ((BasicScript) sc).doInit(et1);
                }

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
                if (sc instanceof BasicScript) {
                    ((BasicScript) sc).doInit(et2);
                }

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
