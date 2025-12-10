package games.rednblack.editor.renderer.lights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.lights.shaders.LightShader;

import static games.rednblack.editor.renderer.systems.render.FrameBufferManager.GL_MAX_TEXTURE_SIZE;

/**
 * Handler that manages everything related to lights updating and rendering
 * <p>Implements {@link Disposable}
 * @author kalle_h
 * @author fgnm
 */
public class RayHandler implements Disposable {

    /** Gamma correction value used if enabled */
    static final float GAMMA_COR = 0.625f;

    static boolean gammaCorrection = false;
    static float gammaCorrectionParameter = 1f;

    static int CIRCLE_APPROX_POINTS = 32;
    static float dynamicShadowColorReduction = 1;

    static boolean isDiffuse = false;

    /** Blend function for lights rendering with both shadows and diffusion */
    public final BlendFunc diffuseBlendFunc = new BlendFunc(GL20.GL_DST_COLOR, GL20.GL_ZERO);

    /** Blend function for lights rendering with shadows but without diffusion */
    public final BlendFunc shadowBlendFunc = new BlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

    /** Blend function for lights rendering without shadows and diffusion */
    public final BlendFunc simpleBlendFunc = new BlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

    final Matrix4 combined = new Matrix4();
    final Color ambientLight = new Color();

    /** This Array contain all the lights. */
    final Array<Light> lightList = new Array<>(false, 16);

    /** This Array contain all the disabled lights. */
    final Array<Light> disabledLights = new Array<>(false, 16);

    LightMap lightMap;

    LightBatch lightBatch;
    ShaderProgram lightShader; // Ora usiamo l'Uber-Shader

    boolean culling = true;
    boolean shadows = true;
    boolean blur = true;

    // Pseudo3D & Experimental
    boolean pseudo3d = false;
    boolean shadowColorInterpolation = true;
    int shadowsDroppedLimit = 10;

    int blurNum = 1;
    int lightMapScale = 4;

    boolean customViewport = false;
    int viewportX = 0;
    int viewportY = 0;
    int viewportWidth = Gdx.graphics.getWidth();
    int viewportHeight = Gdx.graphics.getHeight();

    // Setup references
    World world;
    OrthographicCamera camera;

    /** camera matrix corners */
    float x1, x2, y1, y2;

    int lightRenderedLastFrame = 0;

    int originalFboWidth;
    int originalFboHeight;

    /**
     * Class constructor specifying the physics world.
     */
    public RayHandler(World world) {
        this(world, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), null);
    }

    public RayHandler(World world, RayHandlerOptions options) {
        this(world, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), options);
    }

    public RayHandler(World world, int fboWidth, int fboHeight) {
        this(world, fboWidth, fboHeight, null);
    }

    public RayHandler(World world, int fboWidth, int fboHeight, RayHandlerOptions options) {
        this.world = world;

        if (options != null) {
            isDiffuse = options.isDiffuse;
            gammaCorrection = options.gammaCorrection;
            pseudo3d = options.pseudo3d;
            shadowColorInterpolation = options.shadowColorInterpolation;
        }

        lightShader = LightShader.createLightShader();
        lightBatch = new LightBatch(lightShader);

        resizeFBO(fboWidth, fboHeight);
    }

    /**
     * Resize the FBO used for intermediate rendering.
     */
    public void resizeFBO(int fboWidth, int fboHeight) {
        originalFboWidth = fboWidth;
        originalFboHeight = fboHeight;

        fboWidth /= lightMapScale;
        fboHeight /= lightMapScale;

        if (fboWidth > GL_MAX_TEXTURE_SIZE || fboHeight > GL_MAX_TEXTURE_SIZE) {
            double aspectRatio = (double) fboWidth / fboHeight;
            if (fboWidth > fboHeight) {
                fboWidth = GL_MAX_TEXTURE_SIZE;
                fboHeight = (int) (GL_MAX_TEXTURE_SIZE / aspectRatio);
            } else {
                fboHeight = GL_MAX_TEXTURE_SIZE;
                fboWidth = (int) (GL_MAX_TEXTURE_SIZE * aspectRatio);
            }
        }

        if (lightMap != null) {
            lightMap.dispose();
        }
        lightMap = new LightMap(this, fboWidth, fboHeight);
    }

    public void setCombinedMatrix(OrthographicCamera camera) {
        this.camera = camera;
        this.setCombinedMatrix(
                camera.combined,
                camera.position.x,
                camera.position.y,
                camera.viewportWidth * camera.zoom,
                camera.viewportHeight * camera.zoom);
    }

    public void setCombinedMatrix(Matrix4 combined, float x, float y,
                                  float viewPortWidth, float viewPortHeight) {

        System.arraycopy(combined.val, 0, this.combined.val, 0, 16);

        final float halfViewPortWidth = viewPortWidth * 0.5f;
        x1 = x - halfViewPortWidth;
        x2 = x + halfViewPortWidth;

        final float halfViewPortHeight = viewPortHeight * 0.5f;
        y1 = y - halfViewPortHeight;
        y2 = y + halfViewPortHeight;
    }

    boolean intersect(float x, float y, float radius) {
        return (x1 < (x + radius) && x2 > (x - radius) &&
                y1 < (y + radius) && y2 > (y - radius));
    }

    public void updateAndRender() {
        update();
        render();
    }

    public void update() {
        for (Light light : lightList) {
            light.update();
        }
    }

    /**
     * Prepare all lights for rendering and renders them to FBO.
     */
    public void prepareRender() {
        lightRenderedLastFrame = 0;

        Gdx.gl.glDepthMask(false);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // 1. Render Main Lights (Geometry) to FrameBuffer
        boolean useLightMap = (shadows || blur);
        if (useLightMap) {
            lightMap.frameBuffer.begin();
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        }

        // Setup blending for lights accumulation
        simpleBlendFunc.apply();

        lightBatch.resize(lightMap.frameBuffer.getWidth(), lightMap.frameBuffer.getHeight());
        lightBatch.begin(combined);

        for (Light light : lightList) {
            light.draw(lightBatch);
        }

        if (pseudo3d && shadows) {
            lightBatch.flush();

            Gdx.gl.glBlendEquation(GL20.GL_FUNC_REVERSE_SUBTRACT);
            Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);

            for (Light light : lightList) {
                light.drawDynamicShadows(lightBatch);
            }
        }

        lightBatch.end();

        Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);

        if (useLightMap) {
            if (customViewport) {
                lightMap.frameBuffer.end(viewportX, viewportY, viewportWidth, viewportHeight);
            } else {
                lightMap.frameBuffer.end();
            }

            // 3. Blur Passes
            boolean needed = lightRenderedLastFrame > 0;
            if (needed && blur)
                lightMap.gaussianBlur(lightMap.frameBuffer, blurNum);
        }
    }

    /**
     * Manual rendering method for all lights.
     */
    public void render() {
        prepareRender();
        lightMap.render();
    }

    /**
     * Checks whether the given point is inside of any light volume
     */
    public boolean pointAtLight(float x, float y) {
        for (Light light : lightList) {
            if (light.contains(x, y)) return true;
        }
        return false;
    }

    public boolean pointAtShadow(float x, float y) {
        for (Light light : lightList) {
            if (light.contains(x, y)) return false;
        }
        return true;
    }

    public void dispose() {
        removeAll();
        if (lightMap != null) lightMap.dispose();
        if (lightBatch != null) lightBatch.dispose();
        if (lightShader != null) lightShader.dispose();
    }

    public void removeAll() {
        for (Light light : lightList) {
            light.dispose();
        }
        lightList.clear();

        for (Light light : disabledLights) {
            light.dispose();
        }
        disabledLights.clear();
    }

    public void setCulling(boolean culling) {
        this.culling = culling;
    }

    public void setBlur(boolean blur) {
        this.blur = blur;
    }

    public void setBlurNum(int blurNum) {
        this.blurNum = blurNum;
    }

    public void setLightMapScale(int lightMapScale) {
        if (lightMapScale <= 0) return;

        if (lightMapScale != this.lightMapScale) {
            this.lightMapScale = lightMapScale;
            resizeFBO(originalFboWidth, originalFboHeight);
        }
    }

    public void setShadows(boolean shadows) {
        this.shadows = shadows;
    }

    public void setAmbientLight(float ambientLight) {
        this.ambientLight.a = MathUtils.clamp(ambientLight, 0f, 1f);
    }

    public void setAmbientLight(float r, float g, float b, float a) {
        this.ambientLight.set(r, g, b, a);
    }

    public void setAmbientLight(Color ambientLightColor) {
        this.ambientLight.set(ambientLightColor);
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public static boolean getGammaCorrection() {
        return gammaCorrection;
    }

    public void applyGammaCorrection(boolean gammaCorrectionWanted) {
        gammaCorrection = gammaCorrectionWanted;
        gammaCorrectionParameter = gammaCorrection ? GAMMA_COR : 1f;
    }

    public void setDiffuseLight(boolean useDiffuse) {
        isDiffuse = useDiffuse;
        lightMap.createShaders();
    }

    public static boolean isDiffuseLight() {
        return isDiffuse;
    }

    public static float getDynamicShadowColorReduction () {
        return dynamicShadowColorReduction;
    }

    public void useCustomViewport(Viewport viewport, int x, int y, int width, int height) {
        customViewport = true;
        viewportX = x;
        viewportY = y;
        viewportWidth = width;
        viewportHeight = height;
    }

    public void useDefaultViewport() {
        customViewport = false;
    }

    public void setPseudo3dLight(boolean flag) {
        setPseudo3dLight(flag, false);
    }

    public void setPseudo3dLight(boolean flag, boolean interpolateShadows) {
        pseudo3d = flag;
        shadowColorInterpolation = interpolateShadows;
        lightMap.createShaders();
    }

    public void setLightMapRendering(boolean isAutomatic) {
        lightMap.lightMapDrawingDisabled = !isAutomatic;
    }

    public Texture getLightMapTexture() {
        return lightMap.frameBuffer.getColorBufferTexture();
    }

    public FrameBuffer getLightMapBuffer() {
        return lightMap.frameBuffer;
    }

    public void setNormalMap(Texture normalMap) {
        lightBatch.setNormalMap(normalMap);
    }

    public LightBatch getLightBatch() {
        return lightBatch;
    }
}