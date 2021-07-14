package games.rednblack.editor.renderer.components.physics;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import games.rednblack.editor.renderer.box2dLight.LightData;
import games.rednblack.editor.renderer.commons.RefreshableObject;
import games.rednblack.editor.renderer.components.PolygonComponent;
import games.rednblack.editor.renderer.components.RemovableComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.physics.PhysicsBodyLoader;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class PhysicsBodyComponent extends RefreshableObject implements RemovableComponent {
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
    public boolean fixedRotation = false;

	public float density = 1;
	public float friction = 1;
	public float restitution = 0;
    public Filter filter = new Filter();

    public float height = 1;

    public float centerX;
    public float centerY;

    public Body body;

    public PhysicsBodyComponent() {

    }

    @Override
    public void onRemove() {
        if (body != null && body.getWorld() != null) {
            body.getWorld().destroyBody(body);
            body = null;
        }
    }

    @Override
    public void reset() {
        centerX = 0;
        centerY = 0;

        bodyType = 0;
        mass = 0;
        centerOfMass.set(0, 0);
        rotationalInertia = 1;
        damping = 0;
        gravityScale = 1;
        allowSleep = true;
        sensor = false;
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
        body = null;
    }

    public FixtureDef createFixtureDef() {
        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = restitution;

        fixtureDef.isSensor = sensor;

        fixtureDef.filter.maskBits = filter.maskBits;
        fixtureDef.filter.groupIndex = filter.groupIndex;
        fixtureDef.filter.categoryBits = filter.categoryBits;

        fixtureDef.shape = PhysicsBodyLoader.getInstance().tmpShape;

        return fixtureDef;
    }

    @Override
    protected void refresh(Entity entity) {
        PolygonComponent polygonComponent = ComponentRetriever.get(entity, PolygonComponent.class);
        if(polygonComponent == null || polygonComponent.vertices == null) return;

        TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class);
        float scaleX = transformComponent.scaleX * (transformComponent.flipX ? -1 : 1);
        float scaleY = transformComponent.scaleY * (transformComponent.flipY ? -1 : 1);

        if (body != null) {
            //TODO currently we support only one shape per entity, this may be changed in future to support multiple shapes
            for (int i = 0; i < polygonComponent.vertices.length; i++) {
                if (i == body.getFixtureList().size) {
                    body.createFixture(createFixtureDef()).setUserData(new LightData(height));
                }
                Shape shape = body.getFixtureList().get(i).getShape();

                if (shape instanceof PolygonShape) {
                    PolygonShape p = (PolygonShape) shape;
                    Vector2[] minPolygonDatum = polygonComponent.vertices[i];
                    float[] verts = PhysicsBodyLoader.getInstance().getVerticesArray(minPolygonDatum.length * 2);
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
            }

            //Clear unused fixtures
            for (int i = polygonComponent.vertices.length; i < body.getFixtureList().size; i++) {
                Fixture fixture = body.getFixtureList().get(i);
                if (fixture.getShape() instanceof PolygonShape
                        && fixture.getUserData() instanceof LightData)
                    body.destroyFixture(fixture);
            }
        }
    }
}
