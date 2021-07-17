package games.rednblack.hyperrunner.script;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.physics.PhysicsContact;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.utils.ItemWrapper;
import games.rednblack.hyperrunner.component.DiamondComponent;
import games.rednblack.hyperrunner.component.PlayerComponent;

public class PlayerScript extends BasicScript implements PhysicsContact {

    public static final int LEFT = 1;
    public static final int RIGHT = -1;
    public static final int JUMP = 0;

    protected ComponentMapper<DiamondComponent> diamondCM;
    protected ComponentMapper<DimensionsComponent> dimensionsCM;
    protected ComponentMapper<MainItemComponent> mainItemCM;
    protected ComponentMapper<PhysicsBodyComponent> physicsBodyCM;
    protected ComponentMapper<PlayerComponent> playerCM;
    protected ComponentMapper<TransformComponent> transformCM;

    private int animEntity;
    private PhysicsBodyComponent mPhysicsBodyComponent;

    private final Vector2 impulse = new Vector2(0, 0);
    private final Vector2 speed = new Vector2(0, 0);

    private World mEngine;

    private World getmEngine() {
        return mEngine;
    }

    private void setmEngine(World mEngine) {
        this.mEngine = mEngine;
    }

    public PlayerScript(World engine) {
        mEngine = engine;
    }

    @Override
    public void init(int item) {
        super.init(item);

        ItemWrapper itemWrapper = new ItemWrapper(item);
        animEntity = itemWrapper.getChild("player-anim").getEntity();

        mPhysicsBodyComponent = physicsBodyCM.get(item);
    }

    @Override
    public void act(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            movePlayer(LEFT);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            movePlayer(RIGHT);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            movePlayer(JUMP);
        }
    }

    public void movePlayer(int direction) {
        Body body = mPhysicsBodyComponent.body;

        speed.set(body.getLinearVelocity());

        switch (direction) {
            case LEFT:
                impulse.set(-5, speed.y);
                break;
            case RIGHT:
                impulse.set(5, speed.y);
                break;
            case JUMP:
                TransformComponent transformComponent = transformCM.get(entity);
                impulse.set(speed.x, transformComponent.y < 6 ? 5 : speed.y);
                break;
        }

        body.applyLinearImpulse(impulse.sub(speed), body.getWorldCenter(), true);
    }

    public PlayerComponent getPlayerComponent() {
        return playerCM.get(animEntity);
    }

    @Override
    public void dispose() {

    }

    @Override
    public void beginContact(int contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact) {
        MainItemComponent mainItemComponent = mainItemCM.get(contactEntity);

        PlayerComponent playerComponent = playerCM.get(animEntity);

        if (mainItemComponent.tags.contains("platform"))
            playerComponent.touchedPlatforms++;

        DiamondComponent diamondComponent = diamondCM.get(contactEntity);
        if (diamondComponent != null) {
            playerComponent.diamondsCollected += diamondComponent.value;
            mEngine.delete(contactEntity);
        }
    }

    @Override
    public void endContact(int contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact) {
        MainItemComponent mainItemComponent = mainItemCM.get(contactEntity);

        PlayerComponent playerComponent = playerCM.get(animEntity);

        if (mainItemComponent.tags.contains("platform"))
            playerComponent.touchedPlatforms--;
    }

    @Override
    public void preSolve(int contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact) {
        TransformComponent transformComponent = transformCM.get(this.entity);

        TransformComponent colliderTransform = transformCM.get(contactEntity);
        DimensionsComponent colliderDimension = dimensionsCM.get(contactEntity);

        if (transformComponent.y < colliderTransform.y + colliderDimension.height) {
            contact.setFriction(0);
        } else {
            contact.setFriction(1);
        }
    }

    @Override
    public void postSolve(int contactEntity, Fixture contactFixture, Fixture ownFixture, Contact contact) {

    }
}
