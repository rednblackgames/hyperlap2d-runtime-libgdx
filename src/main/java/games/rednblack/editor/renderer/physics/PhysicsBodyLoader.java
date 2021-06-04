package games.rednblack.editor.renderer.physics;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import games.rednblack.editor.renderer.box2dLight.LightData;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.components.physics.SensorUserData;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.PolygonUtils;
import games.rednblack.editor.renderer.utils.TransformMathUtils;

/**
 * Created by azakhary on 9/28/2014.
 */
public class PhysicsBodyLoader {

    private static PhysicsBodyLoader instance;
    private final Vector2 tmp = new Vector2();

    public static PhysicsBodyLoader getInstance() {
        if(instance == null) {
            instance = new PhysicsBodyLoader();
        }
        return instance;
    }

    private PhysicsBodyLoader() {
    }

    public Body createBody(World world, Entity entity, PhysicsBodyComponent physicsComponent, Vector2[][] minPolygonData, TransformComponent transformComponent) {
        if(physicsComponent == null || ComponentRetriever.get(entity, MainItemComponent.class) == null) {
            return null;
        }

        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.density = physicsComponent.density;
        fixtureDef.friction = physicsComponent.friction;
        fixtureDef.restitution = physicsComponent.restitution;

        fixtureDef.isSensor = physicsComponent.sensor;

        fixtureDef.filter.maskBits = physicsComponent.filter.maskBits;
        fixtureDef.filter.groupIndex = physicsComponent.filter.groupIndex;
        fixtureDef.filter.categoryBits = physicsComponent.filter.categoryBits;

        BodyDef bodyDef = new BodyDef();
        tmp.set(transformComponent.originX, transformComponent.originY);
        TransformMathUtils.localToSceneCoordinates(entity, tmp);
        bodyDef.position.set(tmp.x, tmp.y);
        bodyDef.angle = transformComponent.rotation * MathUtils.degreesToRadians;

        bodyDef.gravityScale = physicsComponent.gravityScale;
        bodyDef.linearDamping = physicsComponent.damping < 0 ? 0 : physicsComponent.damping;
        bodyDef.angularDamping = physicsComponent.angularDamping < 0 ? 0 : physicsComponent.angularDamping;

        bodyDef.awake = physicsComponent.awake;
        bodyDef.allowSleep = physicsComponent.allowSleep;
        bodyDef.bullet = physicsComponent.bullet;
        bodyDef.fixedRotation = physicsComponent.fixedRotation;

        if(physicsComponent.bodyType == 0) {
            bodyDef.type = BodyDef.BodyType.StaticBody;
        } else if (physicsComponent.bodyType == 1){
            bodyDef.type = BodyDef.BodyType.KinematicBody;
        } else {
            bodyDef.type = BodyDef.BodyType.DynamicBody;
        }

        Body body = world.createBody(bodyDef);

        if (ComponentRetriever.get(entity, LightBodyComponent.class) != null) {
            //createChainShape(body, fixtureDef, minPolygonData);
        } else {
            createPolygonShape(body, fixtureDef, transformComponent, physicsComponent, minPolygonData);
        }

        if (physicsComponent.mass != 0) {
            MassData massData = new MassData();
            massData.mass = physicsComponent.mass;
            massData.center.set(physicsComponent.centerOfMass);
            massData.I = physicsComponent.rotationalInertia;

            body.setMassData(massData);
        }
        
        SensorComponent sensorComponent = ComponentRetriever.get(entity, SensorComponent.class);
    	DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class);
        if (sensorComponent != null && dimensionsComponent != null) {
        	createSensors(body, sensorComponent, dimensionsComponent);
        }

        return body;
    }

    /**
     * Creates the sensors and attaches them to the body.
     * 
     * @param body The body to attach the sensor to.
     * @param sensorComponent The sensor component.
     * @param dimensionsComponent The dimension of the body. Used to compute the position and dimension of the sensors.
     * 
     * @author Jan-Thierry Wegener
     */
    private void createSensors(Body body, SensorComponent sensorComponent, DimensionsComponent dimensionsComponent) {
    	if (sensorComponent.bottom) {
            tmp.set(dimensionsComponent.width / 2f, 0);
    		
    		PolygonShape ps = new PolygonShape();
    		ps.setAsBox(dimensionsComponent.width / 4f, 0.05f, tmp, 0f);
    		
    		FixtureDef sensorFix = new FixtureDef();
    		sensorFix.isSensor = true;
    		sensorFix.shape = ps;
    		
    		body.createFixture(sensorFix).setUserData(SensorUserData.BOTTOM);
    		
    		ps.dispose();
    	}
    	if (sensorComponent.top) {
            tmp.set(dimensionsComponent.width / 2f, dimensionsComponent.height);
    		
    		PolygonShape ps = new PolygonShape();
    		ps.setAsBox(dimensionsComponent.width / 4f, 0.05f, tmp, 0f);
    		
    		FixtureDef sensorFix = new FixtureDef();
    		sensorFix.isSensor = true;
    		sensorFix.shape = ps;
    		
    		body.createFixture(sensorFix).setUserData(SensorUserData.TOP);
    		
    		ps.dispose();
    	}
    	if (sensorComponent.left) {
            tmp.set(0, dimensionsComponent.height / 2f);
    		
    		PolygonShape ps = new PolygonShape();
    		ps.setAsBox(0.05f, dimensionsComponent.height / 4f, tmp, 0f);
    		
    		FixtureDef sensorFix = new FixtureDef();
    		sensorFix.isSensor = true;
    		sensorFix.shape = ps;
    		
    		body.createFixture(sensorFix).setUserData(SensorUserData.LEFT);
    		
    		ps.dispose();
    	}
    	if (sensorComponent.right) {
            tmp.set(dimensionsComponent.width, dimensionsComponent.height / 2f);
    		
    		PolygonShape ps = new PolygonShape();
    		ps.setAsBox(0.05f, dimensionsComponent.height / 4f, tmp, 0f);
    		
    		FixtureDef sensorFix = new FixtureDef();
    		sensorFix.isSensor = true;
    		sensorFix.shape = ps;
    		
    		body.createFixture(sensorFix).setUserData(SensorUserData.RIGHT);
    		
    		ps.dispose();
    	}
	}

	private void createChainShape(Body body, FixtureDef fixtureDef, Vector2[][] minPolygonData) {
        Vector2[] vertices = PolygonUtils.mergeTouchingPolygonsToOne(minPolygonData);
        ChainShape chainShape = new ChainShape();
        chainShape.createChain(vertices);

        fixtureDef.shape = chainShape;
        body.createFixture(fixtureDef);
        chainShape.dispose();
    }

    private void createPolygonShape(Body body, FixtureDef fixtureDef, TransformComponent transformComponent, PhysicsBodyComponent physicsComponent, Vector2[][] minPolygonData) {
        PolygonShape polygonShape = new PolygonShape();

        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        for (Vector2[] minPolygonDatum : minPolygonData) {
            float[] verts = new float[minPolygonDatum.length * 2];
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
            polygonShape.set(verts);
            fixtureDef.shape = polygonShape;
            body.createFixture(fixtureDef).setUserData(new LightData(physicsComponent.height));
        }

        polygonShape.dispose();
    }
}
