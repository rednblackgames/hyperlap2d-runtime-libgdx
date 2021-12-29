package games.rednblack.editor.renderer.physics;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pools;
import games.rednblack.editor.renderer.box2dLight.LightData;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.components.physics.SensorUserData;
import games.rednblack.editor.renderer.components.shape.CircleShapeComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.data.PhysicsBodyDataVO;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

public class PhysicsBodyLoader {

    private static PhysicsBodyLoader instance;

    private final Vector2 tmp = new Vector2();
    private final PolygonShape tmpPolygonShape = new PolygonShape();
    private final ChainShape tmpChainShape = new ChainShape();
    private final CircleShape tmpCircleShape = new CircleShape();
    private final IntMap<float[]> verticesCache = new IntMap<>();
    private final FixtureDef tmpFixtureDef = new FixtureDef();
    private final FixtureDef sensorFixtureDef = new FixtureDef();

    public static PhysicsBodyLoader getInstance() {
        if (instance == null) {
            instance = new PhysicsBodyLoader();
        }
        return instance;
    }

    private PhysicsBodyLoader() {
        sensorFixtureDef.isSensor = true;
        tmpPolygonShape.set(new float[]{0, 0, 0, 1, 1, 0});
    }

    public void dispose() {
        tmpPolygonShape.dispose();
        tmpChainShape.dispose();
        tmpCircleShape.dispose();
        instance = null;
    }

    public Body createBody(World world, int entity, PhysicsBodyComponent physicsComponent, TransformComponent transformComponent, com.artemis.World engine) {
        if (physicsComponent == null || ComponentRetriever.get(entity, MainItemComponent.class, engine) == null) {
            return null;
        }

        BodyDef bodyDef = new BodyDef();
        tmp.set(transformComponent.originX, transformComponent.originY);
        ComponentMapper<TransformComponent> transformMapper = (ComponentMapper<TransformComponent>) ComponentMapper.getFor(TransformComponent.class, engine);
        ComponentMapper<ParentNodeComponent> parentNodeMapper = (ComponentMapper<ParentNodeComponent>) ComponentMapper.getFor(ParentNodeComponent.class, engine);
        TransformMathUtils.localToSceneCoordinates(entity, tmp, transformMapper, parentNodeMapper);
        bodyDef.position.set(tmp.x, tmp.y);
        bodyDef.angle = transformComponent.rotation * MathUtils.degreesToRadians;

        bodyDef.gravityScale = physicsComponent.gravityScale;
        bodyDef.linearDamping = physicsComponent.damping < 0 ? 0 : physicsComponent.damping;
        bodyDef.angularDamping = physicsComponent.angularDamping < 0 ? 0 : physicsComponent.angularDamping;

        bodyDef.awake = physicsComponent.awake;
        bodyDef.allowSleep = physicsComponent.allowSleep;
        bodyDef.bullet = physicsComponent.bullet;
        bodyDef.fixedRotation = physicsComponent.fixedRotation;

        if (physicsComponent.bodyType == 0) {
            bodyDef.type = BodyDef.BodyType.StaticBody;
        } else if (physicsComponent.bodyType == 1) {
            bodyDef.type = BodyDef.BodyType.KinematicBody;
        } else {
            bodyDef.type = BodyDef.BodyType.DynamicBody;
        }

        physicsComponent.body = world.createBody(bodyDef);

        refreshShape(entity, engine);

        if (physicsComponent.mass != 0 && bodyDef.type == BodyDef.BodyType.DynamicBody) {
            MassData massData = physicsComponent.body.getMassData();
            massData.mass = physicsComponent.mass;
            massData.center.set(physicsComponent.centerOfMass);
            massData.I = physicsComponent.rotationalInertia;

            float mI = massData.I - massData.mass * tmp.set(massData.center).dot(massData.center);
            if (mI > 0)
                physicsComponent.body.setMassData(massData);
        }

        return physicsComponent.body;
    }

    /**
     * Creates the sensors and attaches them to the body.
     *
     * @param physicsBodyComponent The body to attach the sensor to.
     * @param sensorComponent      The sensor component.
     * @param dimensionsComponent  The dimension of the body. Used to compute the position and dimension of the sensors.
     * @author Jan-Thierry Wegener
     */
    private void createSensors(PhysicsBodyComponent physicsBodyComponent, SensorComponent sensorComponent, DimensionsComponent dimensionsComponent, TransformComponent transformComponent) {
        sensorFixtureDef.shape = tmpPolygonShape;
        Array<Fixture> fixtures = physicsBodyComponent.fixturesMap.get(PhysicsBodyComponent.FIXTURE_TYPE_SENSORS);
        Fixture bottom = null, top = null, left = null, right = null;
        if (fixtures != null) {
            for (int i = 0; i < fixtures.size; i++) {
                Fixture f = fixtures.get(i);
                if (f.getUserData() == SensorUserData.BOTTOM)
                    bottom = f;
                else if (f.getUserData() == SensorUserData.TOP)
                    top = f;
                else if (f.getUserData() == SensorUserData.LEFT)
                    left = f;
                else if (f.getUserData() == SensorUserData.RIGHT)
                    right = f;
            }
        }
        PolygonShape p = tmpPolygonShape;

        if (sensorComponent.bottom) {
            tmp.set(dimensionsComponent.width * 0.5f, 0);

            if (bottom != null) //Recycle previous fixture
                p = (PolygonShape) bottom.getShape();
            p.setAsBox(tmp.x * sensorComponent.bottomSpanPercent, 0.05f, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            if (bottom == null)
                physicsBodyComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SENSORS, sensorFixtureDef, SensorUserData.BOTTOM);
        } else {
            physicsBodyComponent.destroyFixture(bottom);
        }

        if (sensorComponent.top) {
            tmp.set(dimensionsComponent.width * 0.5f, dimensionsComponent.height);

            if (top != null) //Recycle previous fixture
                p = (PolygonShape) top.getShape();
            p.setAsBox(tmp.x * sensorComponent.topSpanPercent, 0.05f, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            if (top == null)
                physicsBodyComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SENSORS, sensorFixtureDef, SensorUserData.TOP);
        } else {
            physicsBodyComponent.destroyFixture(top);
        }

        if (sensorComponent.left) {
            tmp.set(0, dimensionsComponent.height * 0.5f);

            if (left != null) //Recycle previous fixture
                p = (PolygonShape) left.getShape();
            p.setAsBox(0.05f, tmp.y * sensorComponent.leftSpanPercent, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            if (left == null)
                physicsBodyComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SENSORS, sensorFixtureDef, SensorUserData.LEFT);
        } else {
            physicsBodyComponent.destroyFixture(left);
        }

        if (sensorComponent.right) {
            tmp.set(dimensionsComponent.width, dimensionsComponent.height * 0.5f);

            if (right != null) //Recycle previous fixture
                p = (PolygonShape) right.getShape();
            p.setAsBox(0.05f, tmp.y * sensorComponent.rightSpanPercent, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            if (right == null)
                physicsBodyComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SENSORS, sensorFixtureDef, SensorUserData.RIGHT);
        } else {
            physicsBodyComponent.destroyFixture(right);
        }
    }

    private void createChainShape(TransformComponent transformComponent, PhysicsBodyComponent physicsComponent, Array<Vector2> minPolygonData) {
        //FIXME Can't recycle shapes here, needs libGDX update :(
        physicsComponent.clearFixtures(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE);

        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        float[] verts = getTemporaryVerticesArray(minPolygonData.size * 2);
        Vector2 point = Pools.obtain(Vector2.class);
        for (int j = 0; j < verts.length; j += 2) {
            point.set(minPolygonData.get(j / 2));

            point.x -= transformComponent.originX;
            point.y -= transformComponent.originY;
            point.x *= scaleX;
            point.y *= scaleY;

            verts[j] = point.x;
            verts[j + 1] = point.y;
        }
        Pools.free(point);
        FixtureDef fixtureDef = getFixtureDef(physicsComponent);
        //FIXME remove `new ChainShape()` and clear previous state instead, needs libGDX update :(
        ChainShape chainShape = new ChainShape();
        fixtureDef.shape = chainShape;
        //FIXME chainShape.clear();
        if (physicsComponent.shapeType == PhysicsBodyDataVO.ShapeType.CHAIN_LOOP)
            chainShape.createLoop(verts);
        else
            chainShape.createChain(verts);

        LightData lightData = Pools.obtain(LightData.class);
        lightData.height = physicsComponent.height;
        physicsComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE, fixtureDef, lightData);
        chainShape.dispose();
    }

    private void createPolygonShape(TransformComponent transformComponent, PhysicsBodyComponent physicsComponent, Vector2[][] minPolygonData) {
        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        LightData lightData = Pools.obtain(LightData.class);
        lightData.height = physicsComponent.height;

        FixtureDef fixtureDef = getFixtureDef(physicsComponent);
        PolygonShape p = (PolygonShape) fixtureDef.shape;

        Array<Fixture> fixtures = physicsComponent.fixturesMap.get(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE);
        if (fixtures != null && fixtures.size > 0 && fixtures.get(0).getShape() instanceof PolygonShape) {
            for (int i = 0; i < minPolygonData.length; i++) {
                //Recycle as many fixtures as possible, create new one only when needed
                if (i == fixtures.size) {
                    physicsComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE, fixtureDef, lightData);
                } else {
                    if (fixtures.get(i).getUserData() != null)
                        Pools.free(fixtures.get(i).getUserData());
                    fixtures.get(i).setUserData(lightData);
                }

                p = (PolygonShape) fixtures.get(i).getShape();

                Vector2[] minPolygonDatum = minPolygonData[i];
                float[] verts = getTemporaryVerticesArray(minPolygonDatum.length * 2);
                for (int j = 0; j < verts.length; j += 2) {
                    float tempX = minPolygonDatum[j / 2].x;
                    float tempY = minPolygonDatum[j / 2].y;

                    minPolygonDatum[j / 2].x -= transformComponent.originX;
                    minPolygonDatum[j / 2].y -= transformComponent.originY;

                    minPolygonDatum[j / 2].x *= scaleX;
                    minPolygonDatum[j / 2].y *= scaleY;

                    verts[j] = minPolygonDatum[j / 2].x;
                    verts[j + 1] = minPolygonDatum[j / 2].y;

                    minPolygonDatum[j / 2].x = tempX;
                    minPolygonDatum[j / 2].y = tempY;

                }
                p.set(verts);
            }

            //Clear all unused fixtures
            for (int i = minPolygonData.length; i < fixtures.size; i++) {
                Fixture fixture = fixtures.get(i);
                physicsComponent.destroyFixture(fixture);
            }
        } else {
            //Can't recycle shapes, destroy all fixtures
            physicsComponent.clearFixtures(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE);

            for (int i = 0; i < minPolygonData.length; i++) {
                Vector2[] minPolygonDatum = minPolygonData[i];
                float[] verts = getTemporaryVerticesArray(minPolygonDatum.length * 2);
                for (int j = 0; j < verts.length; j += 2) {
                    float tempX = minPolygonDatum[j / 2].x;
                    float tempY = minPolygonDatum[j / 2].y;

                    minPolygonDatum[j / 2].x -= transformComponent.originX;
                    minPolygonDatum[j / 2].y -= transformComponent.originY;

                    minPolygonDatum[j / 2].x *= scaleX;
                    minPolygonDatum[j / 2].y *= scaleY;

                    verts[j] = minPolygonDatum[j / 2].x;
                    verts[j + 1] = minPolygonDatum[j / 2].y;

                    minPolygonDatum[j / 2].x = tempX;
                    minPolygonDatum[j / 2].y = tempY;

                }
                p.set(verts);

                physicsComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE, fixtureDef, lightData);
            }
        }
    }

    private void createCircleShape(TransformComponent transformComponent, DimensionsComponent dimensionsComponent, PhysicsBodyComponent physicsComponent, CircleShapeComponent component) {
        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        float x = (dimensionsComponent.width / 2f - transformComponent.originX) * scaleX;
        float y = (dimensionsComponent.height / 2f - transformComponent.originY) * scaleY;

        LightData lightData = Pools.obtain(LightData.class);
        lightData.height = physicsComponent.height;

        Array<Fixture> fixtures = physicsComponent.fixturesMap.get(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE);
        CircleShape shape;
        if (fixtures != null && fixtures.size == 1 && fixtures.get(0).getShape() instanceof CircleShape) {
            //Recycle already created shape
            shape = (CircleShape) fixtures.get(0).getShape();
            if (fixtures.get(0).getUserData() != null)
                Pools.free(fixtures.get(0).getUserData());
            fixtures.get(0).setUserData(lightData);

            shape.setRadius(component.radius);
            shape.setPosition(tmp.set(x, y));
        } else {
            //Can't recycle shape, destroy all fixtures and create a new one
            physicsComponent.clearFixtures(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE);

            FixtureDef fixtureDef = getFixtureDef(physicsComponent);
            shape = (CircleShape) fixtureDef.shape;

            shape.setRadius(component.radius);
            shape.setPosition(tmp.set(x, y));

            physicsComponent.createFixture(PhysicsBodyComponent.FIXTURE_TYPE_SHAPE, fixtureDef, lightData);
        }
    }

    private float[] getTemporaryVerticesArray(int size) {
        if (!verticesCache.containsKey(size))
            verticesCache.put(size, new float[size]);
        return verticesCache.get(size);
    }

    public void refreshShape(int entity, com.artemis.World engine) {
        PhysicsBodyComponent physicsBodyComponent = ComponentRetriever.get(entity, PhysicsBodyComponent.class, engine);
        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class, engine);
        DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class, engine);
        PolygonShapeComponent polygonShapeComponent = ComponentRetriever.get(entity, PolygonShapeComponent.class, engine);
        CircleShapeComponent circleShapeComponent = ComponentRetriever.get(entity, CircleShapeComponent.class, engine);

        switch (physicsBodyComponent.shapeType) {
            case POLYGON:
                if (polygonShapeComponent != null && polygonShapeComponent.vertices != null)
                    createPolygonShape(transformComponent, physicsBodyComponent, polygonShapeComponent.polygonizedVertices);
                break;
            case CHAIN_LOOP:
            case CHAIN:
                if (polygonShapeComponent != null && polygonShapeComponent.vertices != null)
                    createChainShape(transformComponent, physicsBodyComponent, polygonShapeComponent.vertices);
                break;
            case CIRCLE:
                if (circleShapeComponent != null)
                    createCircleShape(transformComponent, dimensionsComponent, physicsBodyComponent, circleShapeComponent);
                break;
            case NONE:
                physicsBodyComponent.clearFixtures();
                break;
        }

        SensorComponent sensorComponent = ComponentRetriever.get(entity, SensorComponent.class, engine);
        if (sensorComponent != null && dimensionsComponent != null) {
            createSensors(physicsBodyComponent, sensorComponent, dimensionsComponent, transformComponent);
        }
    }

    private FixtureDef getFixtureDef(PhysicsBodyComponent component) {
        tmpFixtureDef.density = component.density;
        tmpFixtureDef.friction = component.friction;
        tmpFixtureDef.restitution = component.restitution;

        tmpFixtureDef.isSensor = component.sensor;

        tmpFixtureDef.filter.maskBits = component.filter.maskBits;
        tmpFixtureDef.filter.groupIndex = component.filter.groupIndex;
        tmpFixtureDef.filter.categoryBits = component.filter.categoryBits;

        switch (component.shapeType) {
            case POLYGON:
                tmpFixtureDef.shape = tmpPolygonShape;
                break;
            case CHAIN_LOOP:
            case CHAIN:
                tmpFixtureDef.shape = tmpChainShape;
                break;
            case CIRCLE:
                tmpFixtureDef.shape = tmpCircleShape;
                break;
        }

        return tmpFixtureDef;
    }
}
