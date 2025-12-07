package games.rednblack.editor.renderer.systems.render;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.lights.RayHandler;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ShaderUniformVO;
import games.rednblack.editor.renderer.systems.render.logic.DrawableLogic;
import games.rednblack.editor.renderer.systems.render.logic.DrawableLogicMapper;
import games.rednblack.editor.renderer.utils.ShaderUniformProvider;

import java.util.Stack;

@All(ViewPortComponent.class)
public class HyperLap2dRenderer extends IteratingSystem {
    protected ComponentMapper<ViewPortComponent> viewPortMapper;
    protected ComponentMapper<CompositeTransformComponent> compositeTransformMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;
    protected ComponentMapper<ParentNodeComponent> parentNodeMapper;
    protected ComponentMapper<TransformComponent> transformMapper;
    protected ComponentMapper<BoundingBoxComponent> boundingBoxesMapper;
    protected ComponentMapper<MainItemComponent> mainItemComponentMapper;
    protected ComponentMapper<ShaderComponent> shaderComponentMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<NormalMapRendering> normalMapMapper;
    protected ComponentMapper<TintComponent> tintComponentMapper;
    protected ComponentMapper<LayerMapComponent> layerMapComponentMapper;
    protected ComponentMapper<ZIndexComponent> zIndexComponentMapper;
    protected ComponentMapper<TextureRegionComponent> textureRegionComponentMapper;
    protected ComponentMapper<ChainedEntitiesComponent> chainedEntitiesMapper;

    protected DrawableLogicMapper drawableLogicMapper;
    protected RayHandler rayHandler;
    protected Camera camera;
    protected Viewport viewport;

    public static final Color clearColor = new Color(Color.CLEAR);

    protected final Batch batch;
    protected float timeRunning = 0;

    protected final FrameBufferManager frameBufferManager;
    protected final Camera screenCamera, tmpFboCamera;
    protected Texture screenTexture;
    protected final TextureRegion screenTextureRegion = new TextureRegion();

    protected float invScreenWidth, invScreenHeight;
    protected int pixelsPerWU;

    protected ShaderProgram sceneShader = null;
    protected boolean useLights = false;
    protected boolean hasNormals = false;
    protected final boolean hasStencilBuffer;
    protected float fboScaleFactor = 1;

    protected final Vector3 tmpVec3 = new Vector3();
    protected final Stack<Matrix4> fboM4Stack = new Stack<>();
    protected final Pool<Matrix4> fboM4Pool = new Pool<Matrix4>() {
        @Override
        protected Matrix4 newObject() {
            return new Matrix4();
        }
    };

    protected final SnapshotArray<Integer> screenReadingEntities = new SnapshotArray<>(true, 1, Integer.class);

    protected ShaderUniformProvider shaderUniformProvider;

    protected boolean enableCull = true;

    public HyperLap2dRenderer(Batch batch, boolean hasStencilBuffer) {
        this.batch = batch;
        this.hasStencilBuffer = hasStencilBuffer;
        drawableLogicMapper = new DrawableLogicMapper();

        frameBufferManager = new FrameBufferManager();
        createCoreFrameBuffers();
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

    @Override
    public void process(int entity) {
        timeRunning += Gdx.graphics.getDeltaTime();
        batch.setColor(Color.WHITE);

        ViewPortComponent ViewPortComponent = viewPortMapper.get(entity);
        pixelsPerWU = ViewPortComponent.pixelsPerWU;
        viewport = ViewPortComponent.viewPort;
        camera = viewport.getCamera();

        Gdx.gl.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
        frameBufferManager.begin("main");
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        drawableLogicMapper.beginPipeline();
        batch.begin();
        drawRecursively(entity, 1f, DrawableLogic.RenderingType.TEXTURE);
        batch.end();
        frameBufferManager.endCurrent();
        drawableLogicMapper.endPipeline();

        if (rayHandler != null && useLights) {
            //Render normal map texture only if lights are enabled
            frameBufferManager.begin("normalMap");
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.setProjectionMatrix(camera.combined);

            drawableLogicMapper.beginPipeline();
            batch.begin();
            hasNormals = false;
            drawRecursively(entity, 1f, DrawableLogic.RenderingType.NORMAL_MAP);
            batch.end();
            frameBufferManager.endCurrent();
            drawableLogicMapper.endPipeline();
        }

        screenTexture = frameBufferManager.getColorBufferTexture("main");

        //1. Screen Layer
        batch.setProjectionMatrix(screenCamera.combined);
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setShader(sceneShader);
        batch.begin();
        if (sceneShader != null && shaderUniformProvider != null) shaderUniformProvider.applyUniforms(null, sceneShader);
        batch.draw(screenTexture,
                viewport.getScreenX(), viewport.getScreenY(),
                0, 0,
                viewport.getScreenWidth(), viewport.getScreenHeight(),
                1, 1,
                0,
                0, 0,
                screenTexture.getWidth(), screenTexture.getHeight(),
                false, true);
        batch.setShader(null);

        //2. Screen Effects
        if (screenReadingEntities.size > 0) {
            if (screenTextureRegion.getTexture() == null) screenTextureRegion.setRegion(screenTexture);

            batch.setProjectionMatrix(camera.combined);
            Integer[] children = screenReadingEntities.begin();
            for (int i = 0, n = screenReadingEntities.size; i < n; i++) {
                int child = children[i];
                if (mainItemComponentMapper.has(child))
                    drawEntity(batch, child, 1, DrawableLogic.RenderingType.TEXTURE);
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

            rayHandler.useCustomViewport(viewport, viewport.getScreenX(), viewport.getScreenY(),
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

    protected void drawRecursively(int rootEntity, float parentAlpha, DrawableLogic.RenderingType renderingType) {
        CompositeTransformComponent curCompositeTransformComponent = compositeTransformMapper.get(rootEntity);
        TransformComponent transform = transformMapper.get(rootEntity);
        DimensionsComponent dimensions = dimensionsMapper.get(rootEntity);
        MainItemComponent mainItemComponent = mainItemComponentMapper.get(rootEntity);

        String fboTag = mainItemComponent.itemIdentifier;

        boolean scissors = false;

        if (curCompositeTransformComponent.renderToFBO) {
            //Active composite frame buffer
            batch.end();

            frameBufferManager.createIfNotExists(fboTag, (int) (dimensions.width * pixelsPerWU), (int) (dimensions.height * pixelsPerWU), false, hasStencilBuffer);

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
                applyTransform(transform, batch);
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
                resetTransform(transform, batch);
        }
    }

    private void drawChildren(int rootEntity, Batch batch, CompositeTransformComponent curCompositeTransformComponent, float parentAlpha,
                              DrawableLogic.RenderingType renderingType) {
        NodeComponent nodeComponent = nodeMapper.get(rootEntity);
        Integer[] children = nodeComponent.children.begin();
        TransformComponent transform = transformMapper.get(rootEntity);
        LayerMapComponent rootLayers = layerMapComponentMapper.get(rootEntity);

        float offsetX = transform.x, offsetY = transform.y;

        if (viewPortMapper.has(rootEntity) || curCompositeTransformComponent.renderToFBO) {
            //Don't offset children in root composite or in FBOs
            offsetX = 0;
            offsetY = 0;
        }

        for (int i = 0, n = nodeComponent.children.size; i < n; i++) {
            int child = children[i];

            ZIndexComponent childZIndexComponent = zIndexComponentMapper.get(child);

            if (!rootLayers.isVisible(childZIndexComponent.layerHash)) {
                //Skip if layer is not visible
                continue;
            }

            MainItemComponent childMainItemComponent = mainItemComponentMapper.get(child);
            if (!childMainItemComponent.visible || childMainItemComponent.culled) {
                //Skip if entity is culled or not visible
                if (enableCull) {
                    continue;
                }
            }

            TransformComponent childTransformComponent = transformMapper.get(child);
            float cx = childTransformComponent.x, cy = childTransformComponent.y;
            NodeComponent childNodeComponent = nodeMapper.get(child);

            if (!transform.shouldTransform() || curCompositeTransformComponent.renderToFBO) {
                // The group doesn't need matrix transformation. Just offset child in screen coordinates.
                childTransformComponent.x = cx + offsetX;
                childTransformComponent.y = cy + offsetY;
            }

            if (childNodeComponent == null) {
                if (checkRenderingLayer(child))
                    drawEntity(batch, child, parentAlpha, renderingType);
            } else {
                //Step into Composite
                drawRecursively(child, parentAlpha, renderingType);
            }

            if (!transform.shouldTransform() || curCompositeTransformComponent.renderToFBO) {
                //Restore composite relative position.
                childTransformComponent.x = cx;
                childTransformComponent.y = cy;
            }
        }
        nodeComponent.children.end();
    }

    private void drawEntity(Batch batch, int child, float parentAlpha, DrawableLogic.RenderingType renderingType) {
        if (renderingType == DrawableLogic.RenderingType.NORMAL_MAP && !normalMapMapper.has(child)) {
            return;
        } else if (renderingType == DrawableLogic.RenderingType.NORMAL_MAP && normalMapMapper.has(child))
            hasNormals = true;
        int entityType = mainItemComponentMapper.get(child).entityType;

        applyShader(child, batch);
        //Find the logic from mapper and draw it
        drawableLogicMapper.getDrawable(entityType).draw(batch, child, parentAlpha, renderingType);
        resetShader(child, batch);

        ChainedEntitiesComponent chainedEntitiesComponent = chainedEntitiesMapper.get(child);
        if (chainedEntitiesComponent != null && chainedEntitiesComponent.chainedEntities.size != 0) {
            for (int i = 0; i < chainedEntitiesComponent.chainedEntities.size; i++) {
                int chainedEntity = chainedEntitiesComponent.chainedEntities.get(i);
                drawEntity(batch, chainedEntity, parentAlpha, renderingType);
            }
        }
    }

    /**
     * Returns the transform for this group's coordinate system.
     *
     * @param rootEntity
     */
    protected Matrix4 computeTransform(int rootEntity) {
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
        int parentEntity = -1;
        if (parentNodeComponent != null) {
            parentEntity = parentNodeComponent.parentEntity;
        }

        if (parentEntity != -1) {
            TransformComponent transform = transformMapper.get(parentEntity);
            if (transform.shouldTransform())
                worldTransform.preMul(transform.worldTransform);
        }

        curTransform.computedTransform.set(worldTransform);
        return curTransform.computedTransform;
    }

    protected void applyTransform(TransformComponent curTransform, Batch batch) {
        curTransform.oldTransform.set(batch.getTransformMatrix());
        batch.setTransformMatrix(curTransform.computedTransform);
    }

    protected void resetTransform(TransformComponent curTransform, Batch batch) {
        batch.setTransformMatrix(curTransform.oldTransform);
    }

    protected void applyShader(int entity, Batch batch) {
        if (shaderComponentMapper.has(entity)) {
            ShaderComponent shaderComponent = shaderComponentMapper.get(entity);
            if (shaderComponent.getShader() != null && shaderComponent.getShader().isCompiled()) {
                batch.setShader(shaderComponent.getShader());

                batch.getShader().setUniformf("u_delta_time", Gdx.graphics.getDeltaTime());
                batch.getShader().setUniformf("u_time", timeRunning);
                batch.getShader().setUniformf("u_viewportInverse", invScreenWidth, invScreenHeight);

                TextureRegionComponent entityTextureRegionComponent = textureRegionComponentMapper.get(entity);
                if (entityTextureRegionComponent != null && entityTextureRegionComponent.region != null) {
                    batch.getShader().setUniformf("u_atlas_coords", entityTextureRegionComponent.region.getU(),
                            entityTextureRegionComponent.region.getV(),
                            entityTextureRegionComponent.region.getU2(),
                            entityTextureRegionComponent.region.getV2());
                }

                if (shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN_READING) {
                    if (entityTextureRegionComponent != null && entityTextureRegionComponent.region != null) {
                        entityTextureRegionComponent.screenRegion = screenTextureRegion;
                    }

                    BoundingBoxComponent boundingBoxComponent = boundingBoxesMapper.get(entity);

                    tmpVec3.set(boundingBoxComponent.points[3].x, boundingBoxComponent.points[3].y, 0);
                    viewport.project(tmpVec3);
                    float u = tmpVec3.x;
                    float v = tmpVec3.y;

                    tmpVec3.set(boundingBoxComponent.points[1].x, boundingBoxComponent.points[1].y, 0);
                    viewport.project(tmpVec3);
                    float u2 = tmpVec3.x;
                    float v2 = tmpVec3.y;

                    u = u * invScreenWidth;
                    v = v * invScreenHeight;
                    u2 = u2 * invScreenWidth;
                    v2 = v2 * invScreenHeight;

                    batch.getShader().setUniformf("u_screen_coords", u, v, u2, v2);
                }

                for (String key : shaderComponent.customUniforms.keys()) {
                    ShaderUniformVO vo = shaderComponent.customUniforms.get(key);

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

                if (shaderUniformProvider != null) shaderUniformProvider.applyUniforms(shaderComponent.shaderName, batch.getShader());
            }
        }
    }

    protected void resetShader(int entity, Batch batch) {
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
    protected boolean checkRenderingLayer(int entity) {
        if (shaderComponentMapper.has(entity)) {
            ShaderComponent shaderComponent = shaderComponentMapper.get(entity);

            boolean contains = screenReadingEntities.contains(entity, false);
            if (shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN) {
                if (contains)
                    screenReadingEntities.removeValue(entity, false);
                return true;
            } else if (shaderComponent.renderingLayer == MainItemVO.RenderingLayer.SCREEN_READING) {
                if (!contains)
                    screenReadingEntities.add(entity);
                return false;
            }
        }

        return true;
    }

    public void removeSpecialEntity(int entity) {
        screenReadingEntities.removeValue(entity, false);
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
        frameBufferManager.dispose("normalMap");
        createCoreFrameBuffers();
        screenCamera.viewportWidth = width;
        screenCamera.viewportHeight = height;
        screenCamera.position.set(0, 0, 0);

        screenCamera.translate(screenCamera.viewportWidth * 0.5f, screenCamera.viewportHeight * 0.5f, 0f);
        screenCamera.update();

        invScreenWidth = 1f / screenCamera.viewportWidth;
        invScreenHeight = 1f / screenCamera.viewportHeight;

        screenTextureRegion.setTexture(null);
    }

    public void setFBOScaleFactor(float factor) {
        fboScaleFactor = factor;
    }

    protected void createCoreFrameBuffers() {
        frameBufferManager.createFBO("main", (int) (Gdx.graphics.getBackBufferWidth() / fboScaleFactor), (int) (Gdx.graphics.getBackBufferHeight() / fboScaleFactor), false, hasStencilBuffer, true);
        frameBufferManager.createFBO("normalMap", (int) (Gdx.graphics.getBackBufferWidth() / fboScaleFactor), (int) (Gdx.graphics.getBackBufferHeight() / fboScaleFactor), false, false, true);
    }

    public void setUseLights(boolean useLights) {
        this.useLights = useLights;
    }

    public void setSceneShader(ShaderProgram shaderProgram) {
        if (shaderProgram != null && shaderProgram.isCompiled())
            sceneShader = shaderProgram;
        else
            sceneShader = null;
    }

    public void injectMappers(World engine) {
        drawableLogicMapper.injectMappers(engine);
    }

    public float getTimeRunning() {
        return timeRunning;
    }

    public Camera getScreenCamera() {
        return screenCamera;
    }

    public void setShaderUniformProvider(ShaderUniformProvider shaderUniformProvider) {
        this.shaderUniformProvider = shaderUniformProvider;
    }
}

