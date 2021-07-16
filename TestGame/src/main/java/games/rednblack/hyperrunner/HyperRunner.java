package games.rednblack.hyperrunner;

import com.artemis.World;
import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.SceneConfiguration;
import games.rednblack.editor.renderer.SceneLoader;
import games.rednblack.editor.renderer.resources.AsyncResourceManager;
import games.rednblack.editor.renderer.resources.ResourceManagerLoader;
import games.rednblack.editor.renderer.utils.ItemWrapper;
import games.rednblack.hyperrunner.component.DiamondComponent;
import games.rednblack.hyperrunner.component.PlayerComponent;
import games.rednblack.hyperrunner.script.PlayerScript;
import games.rednblack.hyperrunner.stage.HUD;
import games.rednblack.hyperrunner.system.CameraSystem;
import games.rednblack.hyperrunner.system.PlayerAnimationSystem;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class HyperRunner extends ApplicationAdapter {

    private AssetManager mAssetManager;

    private SceneLoader mSceneLoader;
    private AsyncResourceManager mAsyncResourceManager;

    private Viewport mViewport;
    private OrthographicCamera mCamera;

    private World mEngine;

    private HUD mHUD;
    private ExtendViewport mHUDViewport;

    @Override
    public void create() {
        System.out.println(getClass().getName());

        mAssetManager = new AssetManager();
        mAssetManager.setLoader(AsyncResourceManager.class, new ResourceManagerLoader(mAssetManager.getFileHandleResolver()));
        mAssetManager.load("project.dt", AsyncResourceManager.class);
        mAssetManager.load("skin/skin.json", Skin.class);

        mAssetManager.finishLoading();

        mAsyncResourceManager = mAssetManager.get("project.dt", AsyncResourceManager.class);

        SceneConfiguration configuration = new SceneConfiguration();
        configuration.setiResourceRetriever(mAsyncResourceManager);
        configuration.addSystem(new PlayerAnimationSystem());
//        configuration.addSystem(new TestSystem());
        CameraSystem cameraSystem = new CameraSystem(5, 40, 5, 6);
        configuration.addSystem(cameraSystem);

        mSceneLoader = new SceneLoader(configuration);

        mEngine = mSceneLoader.getEngine();

        mCamera = new OrthographicCamera();
        mViewport = new ExtendViewport(15, 8, mCamera);

        long a = TimeUtils.nanoTime();
        mSceneLoader.loadScene("MainScene", mViewport);
        System.out.println("Time for loadScene: " + (TimeUtils.nanoTime() - a));

        ItemWrapper root = new ItemWrapper(mSceneLoader.getRoot());

        ItemWrapper player = root.getChild("player");
        int playerAnimId = player.getChild("player-anim").getEntity();
        mEngine.edit(playerAnimId).create(PlayerComponent.class);
        PlayerScript playerScript = new PlayerScript(mEngine);
        player.addScript(playerScript, mEngine);
        cameraSystem.setFocus(player.getEntity());

        mSceneLoader.addComponentByTagName("diamond", DiamondComponent.class);

        mHUDViewport = new ExtendViewport(768, 576);
        mHUD = new HUD(mAssetManager.get("skin/skin.json"), mAsyncResourceManager.getTextureAtlas("main"), mHUDViewport, mSceneLoader.getBatch());
        mHUD.setPlayerScript(playerScript);

        InputAdapter webGlfullscreen = new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ENTER && Gdx.app.getType() == Application.ApplicationType.WebGL) {
                    if (!Gdx.graphics.isFullscreen()) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayModes()[0]);
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (Gdx.app.getType() == Application.ApplicationType.WebGL) {
                    if (!Gdx.graphics.isFullscreen()) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayModes()[0]);
                }
                return super.touchUp(screenX, screenY, pointer, button);
            }
        };

        Gdx.input.setInputProcessor(new InputMultiplexer(webGlfullscreen, mHUD));

        System.out.println("Artemis: " + mSceneLoader.getEngine().getClass().getName());
    }

    long sum = 0;
    double count = 0;
    double iterations = 10_000;
    double spacing = 1_000;

    @Override
    public void render() {
        mCamera.update();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mViewport.apply();

//        System.out.println("Frame Change");

        if (count > spacing && count < iterations + spacing) {

            long a = TimeUtils.nanoTime();
            mEngine.setDelta(Gdx.graphics.getDeltaTime());
            mEngine.process();
            a = TimeUtils.nanoTime() - a;

            if (count % 1_000 == 0) System.out.println("Time for Engine   : " + a);

            sum += a;
        } else {
            if (count == iterations + spacing) System.out.println("Average Value = " + (sum / count++));

            mEngine.setDelta(Gdx.graphics.getDeltaTime());
            mEngine.process();
        }

        count++;
        mHUD.act(Gdx.graphics.getDeltaTime());
        mHUDViewport.apply();
        mHUD.draw();
    }

    @Override
    public void resize(int width, int height) {
        mViewport.update(width, height);
        mHUDViewport.update(width, height, true);

        if (width != 0 && height != 0)
            mSceneLoader.resize(width, height);
    }

    @Override
    public void dispose() {
        mAssetManager.dispose();
    }
}