package games.rednblack.editor.renderer.components.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import games.rednblack.editor.renderer.commons.RefreshableComponent;
import games.rednblack.editor.renderer.components.RemovableObject;
import games.rednblack.editor.renderer.data.PhysicsBodyDataVO;
import games.rednblack.editor.renderer.physics.PhysicsBodyLoader;

public class PhysicsBodyComponent extends RefreshableComponent implements RemovableObject {
    protected com.artemis.World engine;

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
    public boolean fixedRotation = false;

    public float density = 1;
    public float friction = 1;
    public float restitution = 0;
    public Filter filter = new Filter();

    public float height = 1;

    public float centerX;
    public float centerY;

    public Body body;
    public PhysicsBodyDataVO.ShapeType shapeType = PhysicsBodyDataVO.ShapeType.POLYGON;

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
            PhysicsBodyLoader.getInstance().refreshShape(entity, body, engine);
        }
    }
}
