package games.rednblack.editor.renderer.physics;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pools;
import games.rednblack.editor.renderer.box2dLight.LightData;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.components.physics.SensorUserData;
import games.rednblack.editor.renderer.data.PhysicsBodyDataVO;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.PolygonUtils;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

/**
 * Created by azakhary on 9/28/2014.
 */
public class PhysicsBodyLoader {

    private static PhysicsBodyLoader instance;

    private final Vector2 tmp = new Vector2();
    private final PolygonShape tmpPolygonShape = new PolygonShape();
    private final ChainShape tmpChainShape = new ChainShape();
    private final CircleShape tmpCircleShape = new CircleShape();
    private final ObjectMap<Integer, float[]> verticesCache = new ObjectMap<>();
    private final FixtureDef tmpFixtureDef = new FixtureDef();
    private final FixtureDef sensorFix = new FixtureDef();

    public static PhysicsBodyLoader getInstance() {
        if (instance == null) {
            instance = new PhysicsBodyLoader();
        }
        return instance;
    }

    private PhysicsBodyLoader() {
        sensorFix.isSensor = true;
    }

    public void dispose() {
        tmpPolygonShape.dispose();
        tmpChainShape.dispose();
        tmpCircleShape.dispose();
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

        Body body = world.createBody(bodyDef);

        refreshShape(entity, body, engine);

        if (physicsComponent.mass != 0) {
            MassData massData = body.getMassData();
            massData.mass = physicsComponent.mass;
            massData.center.set(physicsComponent.centerOfMass);
            massData.I = physicsComponent.rotationalInertia;

            body.setMassData(massData);
        }

        return body;
    }

    /**
     * Creates the sensors and attaches them to the body.
     *
     * @param body                The body to attach the sensor to.
     * @param sensorComponent     The sensor component.
     * @param dimensionsComponent The dimension of the body. Used to compute the position and dimension of the sensors.
     * @author Jan-Thierry Wegener
     */
    private void createSensors(Body body, SensorComponent sensorComponent, DimensionsComponent dimensionsComponent, TransformComponent transformComponent) {
        sensorFix.shape = tmpPolygonShape;

        if (sensorComponent.bottom) {
            tmp.set(dimensionsComponent.width * 0.5f, 0);

            tmpPolygonShape.setAsBox(tmp.x * sensorComponent.bottomSpanPercent, 0.05f, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            body.createFixture(sensorFix).setUserData(SensorUserData.BOTTOM);
        }

        if (sensorComponent.top) {
            tmp.set(dimensionsComponent.width * 0.5f, dimensionsComponent.height);

            tmpPolygonShape.setAsBox(tmp.x * sensorComponent.topSpanPercent, 0.05f, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            body.createFixture(sensorFix).setUserData(SensorUserData.TOP);
        }

        if (sensorComponent.left) {
            tmp.set(0, dimensionsComponent.height * 0.5f);

            tmpPolygonShape.setAsBox(0.05f, tmp.y * sensorComponent.leftSpanPercent, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            body.createFixture(sensorFix).setUserData(SensorUserData.LEFT);
        }

        if (sensorComponent.right) {
            tmp.set(dimensionsComponent.width, dimensionsComponent.height * 0.5f);

            tmpPolygonShape.setAsBox(0.05f, tmp.y * sensorComponent.rightSpanPercent, tmp.sub(transformComponent.originX, transformComponent.originY), 0f);

            body.createFixture(sensorFix).setUserData(SensorUserData.RIGHT);
        }
    }

    private void createChainShape(Body body, TransformComponent transformComponent, PhysicsBodyComponent physicsComponent, Vector2[][] minPolygonData) {
        //TODO Fixtures should be reused if possible
        clearFixtures(body);

        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        Vector2[] vertices = PolygonUtils.mergeTouchingPolygonsToOne(minPolygonData);
        float[] verts = getTemporaryVerticesArray(vertices.length * 2);
        for (int j = 0; j < verts.length; j += 2) {
            Vector2 point = vertices[j / 2];

            point.x -= transformComponent.originX;
            point.y -= transformComponent.originY;
            point.x *= scaleX;
            point.y *= scaleY;

            verts[j] = point.x;
            verts[j + 1] = point.y;
        }
        FixtureDef fixtureDef = getFixtureDef(physicsComponent);
        //TODO remove `new ChainShape()` and clear previous state instead, needs libGDX update :(
        ChainShape chainShape = new ChainShape();
        fixtureDef.shape = chainShape;
        //chainShape.clear();
        if (physicsComponent.shapeType == PhysicsBodyDataVO.ShapeType.CHAIN_LOOP)
            chainShape.createLoop(verts);
        else
            chainShape.createChain(verts);

        LightData lightData = Pools.obtain(LightData.class);
        lightData.height = physicsComponent.height;
        body.createFixture(fixtureDef).setUserData(lightData);
        chainShape.dispose();
    }

    private void createPolygonShape(Body body, TransformComponent transformComponent, PhysicsBodyComponent physicsComponent, Vector2[][] minPolygonData) {
        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        //TODO Fixtures should be reused if possible
        clearFixtures(body);

        for (int i = 0; i < minPolygonData.length; i++) {
            FixtureDef fixtureDef = getFixtureDef(physicsComponent);
            PolygonShape p = (PolygonShape) fixtureDef.shape;

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

            LightData lightData = Pools.obtain(LightData.class);
            lightData.height = physicsComponent.height;
            body.createFixture(fixtureDef).setUserData(lightData);
        }
    }

    private void createCircleShape(Body body, TransformComponent transformComponent, DimensionsComponent dimensionsComponent, PhysicsBodyComponent physicsComponent, CircleShapeComponent component) {
        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        //TODO Fixtures should be reused if possible
        clearFixtures(body);

        float x = (dimensionsComponent.width / 2f - transformComponent.originX) * scaleX;
        float y = (dimensionsComponent.height / 2f - transformComponent.originY) * scaleY;

        FixtureDef fixtureDef = getFixtureDef(physicsComponent);
        CircleShape shape = (CircleShape) fixtureDef.shape;
        shape.setRadius(component.radius);
        shape.setPosition(tmp.set(x, y));

        LightData lightData = Pools.obtain(LightData.class);
        lightData.height = physicsComponent.height;
        body.createFixture(fixtureDef).setUserData(lightData);
    }

    private float[] getTemporaryVerticesArray(int size) {
        if (!verticesCache.containsKey(size))
            verticesCache.put(size, new float[size]);
        return verticesCache.get(size);
    }

    public void refreshShape(int entity, Body body, com.artemis.World engine) {
        PhysicsBodyComponent physicsBodyComponent = ComponentRetriever.get(entity, PhysicsBodyComponent.class, engine);
        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class, engine);
        DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class, engine);
        PolygonComponent polygonComponent = ComponentRetriever.get(entity, PolygonComponent.class, engine);
        CircleShapeComponent circleShapeComponent = ComponentRetriever.get(entity, CircleShapeComponent.class, engine);
        switch (physicsBodyComponent.shapeType) {
            case POLYGON:
                if (polygonComponent != null && polygonComponent.vertices != null)
                    createPolygonShape(body, transformComponent, physicsBodyComponent, polygonComponent.vertices);
                break;
            case CHAIN_LOOP:
            case CHAIN:
                if (polygonComponent != null && polygonComponent.vertices != null)
                    createChainShape(body, transformComponent, physicsBodyComponent, polygonComponent.vertices);
                break;
            case CIRCLE:
                if (circleShapeComponent != null)
                    createCircleShape(body, transformComponent, dimensionsComponent, physicsBodyComponent, circleShapeComponent);
                break;
            case NONE:
                clearFixtures(body);
                break;
        }

        SensorComponent sensorComponent = ComponentRetriever.get(entity, SensorComponent.class, engine);
        if (sensorComponent != null && dimensionsComponent != null) {
            createSensors(body, sensorComponent, dimensionsComponent, transformComponent);
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

    private void clearFixtures(Body body) {
        while (body.getFixtureList().size > 0) {
            Fixture fixture = body.getFixtureList().get(0);
            Pools.free(fixture.getUserData());
            body.destroyFixture(fixture);
        }
    }
}
