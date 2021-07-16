package games.rednblack.hyperrunner.stage;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.Viewport;

import games.rednblack.hyperrunner.script.PlayerScript;

public class HUD extends Stage {

    private Label mDiamondsLabel;

    private PlayerScript mPlayerScript;

    private boolean leftClicked = false;
    private boolean rightClicked = false;

    private int diamonds = -1;

    public HUD(Skin skin, TextureAtlas atlas, Viewport viewport, Batch batch) {
        super(viewport, batch);

        Table root = new Table();
        root.pad(10, 20, 10, 20);
        root.setFillParent(true);

        Table gemCounter = new Table();
        Image diamond = new Image(atlas.findRegion("GemCounter"));
        gemCounter.add(diamond);

        mDiamondsLabel = new Label("Diamonds", skin);
        gemCounter.add(mDiamondsLabel);

        root.add(gemCounter).expand().left().top().colspan(3);
        root.row();

        ImageButton leftButton = new ImageButton(skin, "left");
        leftButton.addListener(new ClickListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                leftClicked = true;
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                leftClicked = false;
            }
        });
        root.add(leftButton).left().bottom();

        ImageButton rightButton = new ImageButton(skin, "right");
        rightButton.addListener(new ClickListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                rightClicked = true;
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                rightClicked = false;
            }
        });
        root.add(rightButton).left().bottom().padLeft(20);

        ImageButton upButton = new ImageButton(skin, "up");
        upButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                mPlayerScript.movePlayer(PlayerScript.JUMP);
            }
        });
        root.add(upButton).expand().right().bottom();

        addActor(root);
    }

    public void setPlayerScript(PlayerScript playerScript) {
        mPlayerScript = playerScript;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (leftClicked)
            mPlayerScript.movePlayer(PlayerScript.LEFT);
        if (rightClicked)
            mPlayerScript.movePlayer(PlayerScript.RIGHT);

        if (diamonds != mPlayerScript.getPlayerComponent().diamondsCollected) {
            diamonds = mPlayerScript.getPlayerComponent().diamondsCollected;
            mDiamondsLabel.setText("x" + diamonds);
        }
    }
}
