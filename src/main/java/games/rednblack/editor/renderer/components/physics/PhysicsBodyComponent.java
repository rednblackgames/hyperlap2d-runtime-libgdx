package games.rednblack.editor.renderer.components.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.data.PhysicsBodyDataVO;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.physics.PhysicsBodyLoader;

public class PhysicsBodyComponent extends RefreshableComponent {
    public static final int FIXTURE_TYPE_SHAPE = 0;
    public static final int FIXTURE_TYPE_SENSORS = 1;
    public static final int FIXTURE_TYPE_USER_DEFINED = 2;

    protected transient Engine engine;

    protected boolean needsRefresh = false;

    public int bodyType = 0;

    public float mass = 0;
    public Vector2 centerOfMass = new Vector2(0, 0);
    public float rotationalInertia = 1;
    public float damping = 0;
    public float angularDamping = 0;
    public float gravityScale = 1;

    public boolean allowSleep = true;
    public boolean awake = true;
    public boolean bullet = false;
    public boolean sensor = false;
    public boolean fineBoundBox = false;
    public boolean fixedRotation = false;

    public float density = 1;
    public float friction = 1;
    public float restitution = 0;
    public Filter filter = new Filter();

    public float height = 1;

    public transient Body body;
    public PhysicsBodyDataVO.ShapeType shapeType = PhysicsBodyDataVO.ShapeType.POLYGON;

    public transient final IntMap<Array<Fixture>> fixturesMap = new IntMap<>();
    private transient final ObjectIntMap<Fixture> inverseFixtureMap = new ObjectIntMap<>();

    public float prevX;
    public float prevY;
    public float prevAngle;

    public PhysicsBodyComponent() {

    }

    @Override
    public void reset() {
        bodyType = 0;
        mass = 0;
        centerOfMass.set(0, 0);
        rotationalInertia = 1;
        damping = 0;
        gravityScale = 1;
        allowSleep = true;
        sensor = false;
        fineBoundBox = false;
        awake = true;
        bullet = false;
        density = 1;
        friction = 1;
        restitution = 0;
        fixedRotation = false;
        angularDamping = 0;

        filter.categoryBits = 0x0001;
        filter.maskBits = -1;
        filter.groupIndex = 0;

        height = 1;

        needsRefresh = false;

        if (body != null && body.getWorld() != null) {
            body.getWorld().destroyBody(body);
            body = null;
        }

        clearFixturesMap();
    }

    @Override
    public void scheduleRefresh() {
        needsRefresh = true;
    }

    @Override
    public void executeRefresh(int entity) {
        if (needsRefresh) {
            refresh(entity);
            needsRefresh = false;
        }
    }

    protected void refresh(int entity) {
        if (body != null) {
            PhysicsBodyLoader.getInstance().refreshShape(entity, engine);
        }
    }

    public Fixture createFixture(int type, FixtureDef fixtureDef, Object userData) {
        if (type < 0) throw new IllegalArgumentException("Fixture Type can't be < 0");
        if (body == null) return null;

        Fixture fixture = body.createFixture(fixtureDef);
        fixture.setUserData(userData);

        if (fixturesMap.get(type) == null)
            fixturesMap.put(type, new Array<Fixture>());

        fixturesMap.get(type).add(fixture);
        inverseFixtureMap.put(fixture, type);

        return fixture;
    }

    public void destroyFixture(Fixture fixture) {
        if (body == null) return;
        if (fixture == null) return;

        int type = inverseFixtureMap.remove(fixture, -1);
        if (type != -1)
            fixturesMap.get(type).removeValue(fixture, true);

        if (fixture.getUserData() != null)
            PhysicsBodyLoader.POOLS.free(fixture.getUserData());
        body.destroyFixture(fixture);
    }

    public void clearFixtures() {
        if (body == null) return;

        while (body.getFixtureList().size > 0) {
            Fixture fixture = body.getFixtureList().get(0);
            if (fixture.getUserData() != null)
                PhysicsBodyLoader.POOLS.free(fixture.getUserData());
            body.destroyFixture(fixture);
        }

        clearFixturesMap();
    }

    public void clearFixtures(int type) {
        if (body == null) return;
        Array<Fixture> fixtures = fixturesMap.get(type);
        if (fixtures == null) return;

        while (fixtures.size > 0) {
            Fixture fixture = fixtures.removeIndex(0);
            if (fixture.getUserData() != null)
                PhysicsBodyLoader.POOLS.free(fixture.getUserData());
            body.destroyFixture(fixture);
            inverseFixtureMap.remove(fixture, -1);
        }
    }

    public void clearFixturesMap() {
        for (Array<Fixture> array : fixturesMap.values())
            array.clear();
        inverseFixtureMap.clear();
    }
}
