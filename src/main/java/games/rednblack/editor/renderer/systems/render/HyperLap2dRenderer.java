package games.rednblack.editor.renderer.systems.render;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ShaderUniformVO;
import games.rednblack.editor.renderer.systems.render.logic.Drawable;
import games.rednblack.editor.renderer.systems.render.logic.DrawableLogicMapper;

import java.util.Map;
import java.util.Stack;

@All(ViewPortComponent.class)
public class HyperLap2dRenderer extends IteratingSystem {
    protected ComponentMapper<ViewPortComponent> viewPortMapper;
    protected ComponentMapper<CompositeTransformComponent> compositeTransformMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<MainItemComponent> mainItemComponentMapper;
    protected ComponentMapper<ShaderComponent> shaderComponentMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<NormalMapRendering> normalMapMapper;
    protected ComponentMapper<TintComponent> tintComponentMapper;
    protected ComponentMapper<LayerMapComponent> layerMapComponentMapper;
    protected ComponentMapper<ZIndexComponent> zIndexComponentMapper;
    protected ComponentMapper<TextureRegionComponent> textureRegionComponentMapper;

    protected DrawableLogicMapper drawableLogicMapper;
    private RayHandler rayHandler;
    private Camera camera;
    private Viewport viewport;

    public static float timeRunning = 0;
    public static Color clearColor = Color.CLEAR;

    public Batch batch;

    private final FrameBufferManager frameBufferManager;
    private final Camera screenCamera, tmpFboCamera;
    private Texture screenTexture;
    private final TextureRegion screenTextureRegion = new TextureRegion();

    private float invScreenWidth, invScreenHeight;
    private int pixelsPerWU;

    private boolean useLights = false;
    private boolean hasNormals = false;

    private final Vector3 tmpVec3 = new Vector3();
    private final Stack<Matrix4> fboM4Stack = new Stack<>();
    private final Pool<Matrix4> fboM4Pool = new Pool<Matrix4>() {
        @Override
        protected Matrix4 newObject() {
            return new Matrix4();
        }
    };

    private final SnapshotArray<Integer> screenReadingEntities = new SnapshotArray<>(true, 1, Integer.class);

    public HyperLap2dRenderer(Batch batch) {
        this.batch = batch;
        drawableLogicMapper = new DrawableLogicMapper();

        frameBufferManager = new FrameBufferManager();
        frameBufferManager.createFBO("main", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        frameBufferManager.createFBO("normalMap", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        screenCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        tmpFboCamera = new OrthographicCamera();

        screenCamera.translate(screenCamera.viewportWidth * 0.5f, screenCamera.viewportHeight * 0.5f, 0f);
        screenCamera.update();

        invScreenWidth = 1f / screenCamera.viewportWidth;
        invScreenHeight = 1f / screenCamera.viewportHeight;
    }

    public void addDrawableType(IExternalItemType itemType) {
        drawableLogicMapper.addDrawableToMap(itemType.getTypeId(), itemType.getDrawable());
    }

    public void setPixelsPerWU(int pixelsPerWU) {
        this.pixelsPerWU = pixelsPerWU;
    }

    @Override
    public void process(int entity) {
        timeRunning += getWorld().delta;

        ViewPortComponent ViewPortComponent = viewPortMapper.get(entity);
        viewport = ViewPortComponent.viewPort;
        camera = viewport.getCamera();

        Gdx.gl.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
        frameBufferManager.begin("main");
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        drawRecursively(entity, 1f, Drawable.RenderingType.TEXTURE);
        batch.end();
        frameBufferManager.endCurrent();

        if (rayHandler != null && useLights) {
            //Render normal map texture only if lights are enabled
            frameBufferManager.begin("normalMap");
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.setProjectionMatrix(camera.combined);

            batch.begin();
            hasNormals = false;
            drawRecursively(entity, 1f, Drawable.RenderingType.NORMAL_MAP);
            batch.end();
            frameBufferManager.endCurrent();
        }

        screenTexture = frameBufferManager.getColorBufferTexture("main");

        //1. Screen Layer
        batch.setProjectionMatrix(screenCamera.combined);
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        batch.draw(screenTexture,
                viewport.getScreenX(), viewport.getScreenY(),
                0, 0,
                viewport.getScreenWidth(), viewport.getScreenHeight(),
                1, 1,
                0,
                0, 0,
                screenTexture.getWidth(), screenTexture.getHeight(),
                false, true);

		/*batch.draw(frameBufferManager.getColorBufferTexture("normalMap"),
				viewport.getScreenX(), viewport.getScreenY(),
				0, 0,
				viewport.getScreenWidth(), viewport.getScreenHeight(),
				1, 1,
				0,
				0, 0,
				screenTexture.getWidth(), screenTexture.getHeight(),
				false, false);*/

        //2. Screen Effects
        if (screenReadingEntities.size > 0) {
            batch.setProjectionMatrix(camera.combined);
            Integer[] children = screenReadingEntities.begin();
            for (int i = 0; i < screenReadingEntities.size; i++) {
                Integer child = children[i];
                if (mainItemComponentMapper.has(child))
                    drawEntity(batch, child, 1, Drawable.RenderingType.TEXTURE);
                else
                    screenReadingEntities.removeIndex(i);
            }
            screenReadingEntities.end();
        }
        batch.end();

        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                GL20.GL_ONE_MINUS_DST_ALPHA, GL20.GL_ONE);

        //3. Over Screen
        if (rayHandler != null && useLights) {
            OrthographicCamera orthoCamera = (OrthographicCamera) camera;

            int retinaScale = Gdx.graphics.getHeight() > 0 ?
                    Gdx.graphics.getBackBufferHeight() / Gdx.graphics.getHeight() : 1;
            int screenWidth = viewport.getScreenWidth() * retinaScale;
            int screenHeight = viewport.getScreenHeight() * retinaScale;

            rayHandler.setViewport(viewport);
            rayHandler.useCustomViewport(viewport.getScreenX(), viewport.getScreenY(),
                    screenWidth, screenHeight);

            if (hasNormals) {
                Texture normal = frameBufferManager.getColorBufferTexture("normalMap");
                rayHandler.setNormalMap(normal);
            } else {
                rayHandler.setNormalMap(null);
            }

            rayHandler.setCombinedMatrix(orthoCamera);
            rayHandler.updateAndRender();
        }
    }

    private void drawRecursively(int rootEntity, float parentAlpha, Drawable.RenderingType renderingType) {
        CompositeTransformComponent curCompositeTransformComponent = compositeTransformMapper.get(rootEntity);
        TransformComponent transform = transformMapper.get(rootEntity);
        DimensionsComponent dimensions = dimensionsMapper.get(rootEntity);
        MainItemComponent mainItemComponent = mainItemComponentMapper.get(rootEntity);

        String fboTag = mainItemComponent.itemIdentifier;

        boolean scissors = false;

        if (curCompositeTransformComponent.renderToFBO) {
            //Active composite frame buffer
            batch.end();

            frameBufferManager.createIfNotExists(fboTag, (int) dimensions.width * pixelsPerWU, (int) dimensions.height * pixelsPerWU);

            tmpFboCamera.viewportWidth = dimensions.width;
            tmpFboCamera.viewportHeight = dimensions.height;
            tmpFboCamera.position.set(tmpFboCamera.viewportWidth * 0.5f, tmpFboCamera.viewportHeight * 0.5f, 0f);
            tmpFboCamera.update();
            batch.setProjectionMatrix(tmpFboCamera.combined);
            fboM4Stack.push(fboM4Pool.obtain().set(tmpFboCamera.combined));

            frameBufferManager.begin(fboTag);

            batch.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        } else {
            if (transform.shouldTransform()) {
                computeTransform(rootEntity);
                applyTransform(rootEntity, batch);
            }

            if (curCompositeTransformComponent.scissorsEnabled) {
                batch.flush();
                ScissorStack.calculateScissors(camera, transform.oldTransform, curCompositeTransformComponent.clipBounds, curCompositeTransformComponent.scissors);
                if (ScissorStack.pushScissors(curCompositeTransformComponent.scissors)) {
                    scissors = true;
                }
            }

            applyShader(rootEntity, batch);
        }

        TintComponent tintComponent = tintComponentMapper.get(rootEntity);
        parentAlpha *= tintComponent.color.a;

        drawChildren(rootEntity, batch, curCompositeTransformComponent, parentAlpha, renderingType);

        if (curCompositeTransformComponent.renderToFBO) {
            //Close FBO and render the result
            batch.end();
            frameBufferManager.endCurrent();

            Matrix4 fboM4 = fboM4Stack.pop();
            fboM4Pool.free(fboM4);

            Matrix4 renderingMatrix = fboM4Stack.size() == 0 ? camera.combined : fboM4Stack.peek();

            batch.setProjectionMatrix(renderingMatrix);
            batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

            batch.begin();

            applyShader(rootEntity, batch);

            Texture bufferTexture = frameBufferManager.getColorBufferTexture(fboTag);
            float scaleX = transform.scaleX * (transform.flipX ? -1 : 1);
            float scaleY = transform.scaleY * (transform.flipY ? -1 : 1);
            batch.draw(bufferTexture,
                    transform.x, transform.y,
                    transform.originX, transform.originY,
                    dimensions.width, dimensions.height,
                    scaleX, scaleY,
                    transform.rotation,
                    0, 0,
                    bufferTexture.getWidth(), bufferTexture.getHeight(),
                    false, true);

            resetShader(rootEntity, batch);

            batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                    GL20.GL_ONE_MINUS_DST_ALPHA, GL20.GL_ONE);
        } else {
            resetShader(rootEntity, batch);

            if (scissors) {
                batch.flush();
                ScissorStack.popScissors();
            }

            if (transform.shouldTransform())
                resetTransform(rootEntity, batch);
        }
    }

    private void drawChildren(Integer rootEntity, Batch batch, CompositeTransformComponent curCompositeTransformComponent, float parentAlpha,
                              Drawable.RenderingType renderingType) {
        NodeComponent nodeComponent = nodeMapper.get(rootEntity);
        Integer[] children = nodeComponent.children.begin();
        TransformComponent transform = transformMapper.get(rootEntity);
        if (transform.shouldTransform() && !curCompositeTransformComponent.renderToFBO) {
            for (int i = 0, n = nodeComponent.children.size; i < n; i++) {
                Integer child = children[i];

                LayerMapComponent rootLayers = layerMapComponentMapper.get(rootEntity);
                ZIndexComponent childZIndexComponent = zIndexComponentMapper.get(child);

                if (!rootLayers.isVisible(childZIndexComponent.layerName)) {
                    continue;
                }

                MainItemComponent childMainItemComponent = mainItemComponentMapper.get(child);
                if (!childMainItemComponent.visible || childMainItemComponent.culled) {
                    continue;
                }

                NodeComponent childNodeComponent = nodeMapper.get(child);

                if (childNodeComponent == null) {
                    if (checkRenderingLayer(child))
                        drawEntity(batch, child, parentAlpha, renderingType);
                } else {
                    //Step into Composite
                    drawRecursively(child, parentAlpha, renderingType);
                }
            }
        } else {
            // No transform for this group, offset each child.
            TransformComponent compositeTransform = transformMapper.get(rootEntity);

            float offsetX = compositeTransform.x, offsetY = compositeTransform.y;

            if (viewPortMapper.has(rootEntity) || curCompositeTransformComponent.renderToFBO) {
                offsetX = 0;
                offsetY = 0;
            }

            for (int i = 0, n = nodeComponent.children.size; i < n; i++) {
                Integer child = children[i];

                LayerMapComponent rootLayers = layerMapComponentMapper.get(rootEntity);
                ZIndexComponent childZIndexComponent = zIndexComponentMapper.get(child);

                if (!rootLayers.isVisible(childZIndexComponent.layerName)) {
                    continue;
                }

                MainItemComponent childMainItemComponent = mainItemComponentMapper.get(child);
                if (!childMainItemComponent.visible || childMainItemComponent.culled) {
                    continue;
                }

                TransformComponent childTransformComponent = transformMapper.get(child);
                float cx = childTransformComponent.x, cy = childTransformComponent.y;
                childTransformComponent.x = cx + offsetX;
                childTransformComponent.y = cy + offsetY;

                NodeComponent childNodeComponent = nodeMapper.get(child);

                if (childNodeComponent == null) {
                    if (checkRenderingLayer(child))
                        drawEntity(batch, child, parentAlpha, renderingType);
                } else {
                    //Step into Composite
                    drawRecursively(child, parentAlpha, renderingType);
                }
                childTransformComponent.x = cx;
                childTransformComponent.y = cy;
            }
        }
        nodeComponent.children.end();
    }

    private void drawEntity(Batch batch, Integer child, float parentAlpha, Drawable.RenderingType renderingType) {
        if (renderingType == Drawable.RenderingType.NORMAL_MAP && !normalMapMapper.has(child)) {
            return;
        } else if (renderingType == Drawable.RenderingType.NORMAL_MAP && normalMapMapper.has(child))
            hasNormals = true;
        int entityType = mainItemComponentMapper.get(child).entityType;

        applyShader(child, batch);
        //Find the logic from mapper and draw it
        drawableLogicMapper.getDrawable(entityType).draw(batch, child, parentAlpha, renderingType);
        resetShader(child, batch);
    }

    /**
     * Returns the transform for this group's coordinate system.
     *
     * @param rootEntity
     */
    protected Matrix4 computeTransform(Integer rootEntity) {
        ParentNodeComponent parentNodeComponent = parentNodeMapper.get(rootEntity);
        TransformComponent curTransform = transformMapper.get(rootEntity);
        Affine2 worldTransform = curTransform.worldTransform;

        float originX = curTransform.originX;
        float originY = curTransform.originY;
        float x = curTransform.x;
        float y = curTransform.y;
        float rotation = curTransform.rotation;
        float scaleX = curTransform.scaleX * (curTransform.flipX ? -1 : 1);
        float scaleY = curTransform.scaleY * (curTransform.flipY ? -1 : 1);

        worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY);
        if (originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

        // Find the parent that transforms.
        Integer parentEntity = null;
        if (parentNodeComponent != null) {
            parentEntity = parentNodeComponent.parentEntity;
        }

        if (parentEntity != null) {
            TransformComponent transform = transformMapper.get(parentEntity);
            if (transform.shouldTransform())
                worldTransform.preMul(transform.worldTransform);
        }

        curTransform.computedTransform.set(worldTransform);
        return curTransform.computedTransform;
    }

    protected void applyTransform(Integer rootEntity, Batch batch) {
        TransformComponent curTransform = transformMapper.get(rootEntity);
        curTransform.oldTransform.set(batch.getTransformMatrix());
        batch.setTransformMatrix(curTransform.computedTransform);
    }

    protected void resetTransform(Integer rootEntity, Batch batch) {
        TransformComponent curTransform = transformMapper.get(rootEntity);
        batch.setTransformMatrix(curTransform.oldTransform);
    }

    protected void applyShader(Integer entity, Batch batch) {
        if (shaderComponentMapper.has(entity)) {
            ShaderComponent shaderComponent = shaderComponentMapper.get(entity);
            if (shaderComponent.getShader() != null && shaderComponent.getShader().isCompiled()) {
                batch.setShader(shaderComponent.getShader());

                batch.getShader().setUniformf("u_delta_time", Gdx.graphics.getDeltaTime());
                batch.getShader().setUniformf("u_time", HyperLap2dRenderer.timeRunning);

                TextureRegionComponent entityTextureRegionComponent = textureRegionComponentMapper.get(entity);
                if (entityTextureRegionComponent != null && entityTextureRegionComponent.region != null) {
                    batch.getShader().setUniformf("u_atlas_coords", entityTextureRegionComponent.region.getU(),
                            entityTextureRegionComponent.region.getV(),
                            entityTextureRegionComponent.region.getU2(),
                            entityTextureRegionComponent.region.getV2());
                }

                if (shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN_READING) {
                    screenTextureRegion.setRegion(screenTexture);
                    if (entityTextureRegionComponent != null && entityTextureRegionComponent.region != null) {
                        entityTextureRegionComponent.region = screenTextureRegion;
                    }

                    TransformComponent transformComponent = transformMapper.get(entity);
                    DimensionsComponent dimensionsComponent = dimensionsMapper.get(entity);

                    tmpVec3.set(transformComponent.x, transformComponent.y, 0);
                    viewport.project(tmpVec3);
                    float u = tmpVec3.x;
                    float v = tmpVec3.y;

                    tmpVec3.set(transformComponent.x + dimensionsComponent.width, transformComponent.y + dimensionsComponent.height, 0);
                    viewport.project(tmpVec3);
                    float u2 = tmpVec3.x;
                    float v2 = tmpVec3.y;

                    u = Math.max(0f, Math.min(1f, u * invScreenWidth));
                    v = Math.max(0f, Math.min(1f, v * invScreenHeight));
                    u2 = Math.max(0f, Math.min(1f, u2 * invScreenWidth));
                    v2 = Math.max(0f, Math.min(1f, v2 * invScreenHeight));

                    batch.getShader().setUniformf("u_screen_coords", u, v, u2, v2);
                }

                for (Map.Entry<String, ShaderUniformVO> me : shaderComponent.customUniforms.entrySet()) {
                    String key = me.getKey();
                    ShaderUniformVO vo = me.getValue();

                    switch (vo.getType()) {
                        case "int":
                            batch.getShader().setUniformi(key, vo.intValue);
                            break;
                        case "float":
                            batch.getShader().setUniformf(key, vo.floatValue);
                            break;
                        case "vec2":
                            batch.getShader().setUniformf(key, vo.floatValue, vo.floatValue2);
                            break;
                        case "vec3":
                            batch.getShader().setUniformf(key, vo.floatValue, vo.floatValue2, vo.floatValue3);
                            break;
                        case "vec4":
                            batch.getShader().setUniformf(key, vo.floatValue, vo.floatValue2, vo.floatValue3, vo.floatValue4);
                            break;
                    }
                }

                GL20 gl = Gdx.gl20;
                int error;
                if ((error = gl.glGetError()) != GL20.GL_NO_ERROR) {
                    Gdx.app.log("opengl", "Error: " + error);
                    Gdx.app.log("opengl", shaderComponent.getShader().getLog());
                    //throw new RuntimeException( ": glError " + error);
                }
            }
        }
    }

    protected void resetShader(Integer entity, Batch batch) {
        if (shaderComponentMapper.has(entity)) {
            batch.setShader(null);
        }
    }

    /**
     * Check if the entity should be rendered in the first screen layer
     *
     * @param entity
     * @return false if the entity belongs to a different rendering layer
     */
    protected boolean checkRenderingLayer(Integer entity) {
        if (shaderComponentMapper.has(entity)) {
            ShaderComponent shaderComponent = shaderComponentMapper.get(entity);

            boolean contains = screenReadingEntities.contains(entity, true);
            if (shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN) {
                if (contains)
                    screenReadingEntities.removeValue(entity, true);
                return true;
            } else if (shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN_READING) {
                if (!contains)
                    screenReadingEntities.add(entity);
                return false;
            }
        }

        return true;
    }

    public void removeSpecialEntity(Integer entity) {
        screenReadingEntities.removeValue(entity, true);
    }

    public void setRayHandler(RayHandler rayHandler) {
        this.rayHandler = rayHandler;
    }

    public Batch getBatch() {
        return batch;
    }

    public FrameBufferManager getFrameBufferManager() {
        return frameBufferManager;
    }

    public void dispose() {
        frameBufferManager.disposeAll();
        fboM4Pool.clear();
        fboM4Stack.clear();
    }

    public void resize(int width, int height) {
        frameBufferManager.endCurrent();
        frameBufferManager.dispose("main");
        frameBufferManager.createFBO("main", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        frameBufferManager.dispose("normalMap");
        frameBufferManager.createFBO("normalMap", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        screenCamera.viewportWidth = width;
        screenCamera.viewportHeight = height;
        screenCamera.position.set(0, 0, 0);

        screenCamera.translate(screenCamera.viewportWidth * 0.5f, screenCamera.viewportHeight * 0.5f, 0f);
        screenCamera.update();

        invScreenWidth = 1f / screenCamera.viewportWidth;
        invScreenHeight = 1f / screenCamera.viewportHeight;
    }

    public void setUseLights(boolean useLights) {
        this.useLights = useLights;
    }

    public void injectMappers(World engine) {
        drawableLogicMapper.injectMappers(engine);
    }
}

