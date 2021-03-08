package games.rednblack.editor.renderer.systems.render;

import box2dLight.RayHandler;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
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
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ShaderUniformVO;
import games.rednblack.editor.renderer.systems.render.logic.DrawableLogicMapper;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.Map;
import java.util.Stack;

public class HyperLap2dRenderer extends IteratingSystem {
	private final ComponentMapper<ViewPortComponent> viewPortMapper = ComponentMapper.getFor(ViewPortComponent.class);
	private final ComponentMapper<CompositeTransformComponent> compositeTransformMapper = ComponentMapper.getFor(CompositeTransformComponent.class);
	private final ComponentMapper<NodeComponent> nodeMapper = ComponentMapper.getFor(NodeComponent.class);
	private final ComponentMapper<ParentNodeComponent> parentNodeMapper = ComponentMapper.getFor(ParentNodeComponent.class);
	private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
	private final ComponentMapper<MainItemComponent> mainItemComponentMapper = ComponentMapper.getFor(MainItemComponent.class);
	private final ComponentMapper<ShaderComponent> shaderComponentMapper = ComponentMapper.getFor(ShaderComponent.class);
	private final ComponentMapper<DimensionsComponent> dimensionsMapper = ComponentMapper.getFor(DimensionsComponent.class);

	private final DrawableLogicMapper drawableLogicMapper;
	private RayHandler rayHandler;
	private Camera camera;
	private Viewport viewport;

	public static float timeRunning = 0;

	public Batch batch;

	private final FrameBufferManager frameBufferManager;
	private final Camera screenCamera;
	private Texture screenTexture;
	private final TextureRegion screenTextureRegion = new TextureRegion();

	private float invScreenWidth, invScreenHeight;
	private int pixelsPerWU;

	private final Vector3 tmpVec3 = new Vector3();
	private final Stack<OrthographicCamera> fboCameraStack = new Stack<>();
	private final Pool<OrthographicCamera> fboCameraPool = new Pool<OrthographicCamera>() {
		@Override
		protected OrthographicCamera newObject() {
			return new OrthographicCamera();
		}
	};

	private final SnapshotArray<Entity> screenReadingEntities = new SnapshotArray<>(true, 1, Entity.class);

	public HyperLap2dRenderer(Batch batch) {
		super(Family.all(ViewPortComponent.class).get(), 1);
		this.batch = batch;
		drawableLogicMapper = new DrawableLogicMapper();

		frameBufferManager = new FrameBufferManager();
		frameBufferManager.createFBO("main", Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		screenCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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
	public void processEntity(Entity entity, float deltaTime) {
		timeRunning+=deltaTime;

		ViewPortComponent ViewPortComponent = viewPortMapper.get(entity);
		viewport = ViewPortComponent.viewPort;
		camera = viewport.getCamera();

		frameBufferManager.begin("main");
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		camera.update();
		batch.setProjectionMatrix(camera.combined);

		batch.begin();
		drawRecursively(entity, 1f);
		batch.end();
		frameBufferManager.endCurrent();

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

		//2. Screen Effects
		if (screenReadingEntities.size > 0) {
			batch.setProjectionMatrix(camera.combined);
			Entity[] children = screenReadingEntities.begin();
			for (int i = 0; i < screenReadingEntities.size; i++) {
				Entity child = children[i];
				if (mainItemComponentMapper.has(child))
					drawEntity(batch, child, 1);
				else
					screenReadingEntities.removeIndex(i);
			}
			screenReadingEntities.end();
		}
		batch.end();

        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
				GL20.GL_ONE_MINUS_DST_ALPHA, GL20.GL_ONE);

		//3. Over Screen
		if (rayHandler != null) {
			OrthographicCamera orthoCamera = (OrthographicCamera) camera;

			rayHandler.useCustomViewport(viewport.getScreenX(), viewport.getScreenY(),
					viewport.getScreenWidth(), viewport.getScreenHeight());
			rayHandler.setCombinedMatrix(orthoCamera);
			rayHandler.updateAndRender();
		}
	}

	private void drawRecursively(Entity rootEntity, float parentAlpha) {
		CompositeTransformComponent curCompositeTransformComponent = compositeTransformMapper.get(rootEntity);
		TransformComponent transform = transformMapper.get(rootEntity);
		DimensionsComponent dimensions = dimensionsMapper.get(rootEntity);
		MainItemComponent mainItemComponent = mainItemComponentMapper.get(rootEntity);

		String tag = mainItemComponent.itemIdentifier;

		boolean scissors = false;

		if (curCompositeTransformComponent.renderToFBO) {
			//Active composite frame buffer
			batch.end();

			frameBufferManager.createIfNotExists(tag, (int) dimensions.width * pixelsPerWU, (int) dimensions.height * pixelsPerWU);

			OrthographicCamera fboCamera = fboCameraPool.obtain();

			fboCamera.viewportWidth = dimensions.width;
			fboCamera.viewportHeight = dimensions.height;
			fboCamera.position.set(fboCamera.viewportWidth * 0.5f, fboCamera.viewportHeight * 0.5f, 0f);
			fboCamera.update();
			batch.setProjectionMatrix(fboCamera.combined);
			fboCameraStack.push(fboCamera);

			frameBufferManager.begin(tag);

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

        TintComponent tintComponent = ComponentRetriever.get(rootEntity, TintComponent.class);
        parentAlpha *= tintComponent.color.a;

		drawChildren(rootEntity, batch, curCompositeTransformComponent, parentAlpha);

		if (curCompositeTransformComponent.renderToFBO) {
			//Close FBO and render the result
			batch.end();
			frameBufferManager.endCurrent();

			OrthographicCamera fboCamera = fboCameraStack.pop();
			fboCameraPool.free(fboCamera);

			Camera renderingCamera = fboCameraStack.size() == 0 ? camera : fboCameraStack.peek();

			batch.setProjectionMatrix(renderingCamera.combined);
			batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

			batch.begin();

			applyShader(rootEntity, batch);

			Texture bufferTexture = frameBufferManager.getColorBufferTexture(tag);
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

	private void drawChildren(Entity rootEntity, Batch batch, CompositeTransformComponent curCompositeTransformComponent, float parentAlpha) {
		NodeComponent nodeComponent = nodeMapper.get(rootEntity);
		Entity[] children = nodeComponent.children.begin();
		TransformComponent transform = transformMapper.get(rootEntity);
		if (transform.shouldTransform() && !curCompositeTransformComponent.renderToFBO) {
			for (int i = 0, n = nodeComponent.children.size; i < n; i++) {
				Entity child = children[i];

				LayerMapComponent rootLayers = ComponentRetriever.get(rootEntity, LayerMapComponent.class);
				ZIndexComponent childZIndexComponent = ComponentRetriever.get(child, ZIndexComponent.class);

				if(!rootLayers.isVisible(childZIndexComponent.layerName)) {
					continue;
				}

				MainItemComponent childMainItemComponent = mainItemComponentMapper.get(child);
				if(!childMainItemComponent.visible || childMainItemComponent.culled){
					continue;
				}

				NodeComponent childNodeComponent = nodeMapper.get(child);

				if(childNodeComponent ==null) {
                    if (checkRenderingLayer(child))
					    drawEntity(batch, child, parentAlpha);
				}else{
					//Step into Composite
					drawRecursively(child, parentAlpha);
				}
			}
		} else {
			// No transform for this group, offset each child.
			TransformComponent compositeTransform = transformMapper.get(rootEntity);

			float offsetX = compositeTransform.x, offsetY = compositeTransform.y;

			if(viewPortMapper.has(rootEntity) || curCompositeTransformComponent.renderToFBO){
				offsetX = 0;
				offsetY = 0;
			}

			for (int i = 0, n = nodeComponent.children.size; i < n; i++) {
				Entity child = children[i];

				LayerMapComponent rootLayers = ComponentRetriever.get(rootEntity, LayerMapComponent.class);
				ZIndexComponent childZIndexComponent = ComponentRetriever.get(child, ZIndexComponent.class);

				if(!rootLayers.isVisible(childZIndexComponent.layerName)) {
					continue;
				}

				MainItemComponent childMainItemComponent = mainItemComponentMapper.get(child);
				if(!childMainItemComponent.visible || childMainItemComponent.culled){
					continue;
				}

				TransformComponent childTransformComponent = transformMapper.get(child);
				float cx = childTransformComponent.x, cy = childTransformComponent.y;
				childTransformComponent.x = cx + offsetX;
				childTransformComponent.y = cy + offsetY;

				NodeComponent childNodeComponent = nodeMapper.get(child);

				if(childNodeComponent ==null) {
                    if (checkRenderingLayer(child))
					    drawEntity(batch, child, parentAlpha);
				}else{
					//Step into Composite
					drawRecursively(child, parentAlpha);
				}
				childTransformComponent.x = cx;
				childTransformComponent.y = cy;
			}
		}
		nodeComponent.children.end();
	}

	private void drawEntity(Batch batch, Entity child, float parentAlpha) {
		int entityType = mainItemComponentMapper.get(child).entityType;

		applyShader(child, batch);
		//Find the logic from mapper and draw it
		drawableLogicMapper.getDrawable(entityType).draw(batch, child, parentAlpha);

		resetShader(child, batch);
	}

	/** Returns the transform for this group's coordinate system.
	 * @param rootEntity
	 *
	 */
	protected Matrix4 computeTransform(Entity rootEntity) {
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
		Entity parentEntity = null;
		if(parentNodeComponent != null){
			parentEntity = parentNodeComponent.parentEntity;
		}

		if (parentEntity != null){
			TransformComponent transform = transformMapper.get(parentEntity);
			if(transform.shouldTransform())
				worldTransform.preMul(transform.worldTransform);
		}

		curTransform.computedTransform.set(worldTransform);
		return curTransform.computedTransform;
	}

	protected void applyTransform (Entity rootEntity, Batch batch) {
		TransformComponent curTransform = transformMapper.get(rootEntity);
		curTransform.oldTransform.set(batch.getTransformMatrix());
		batch.setTransformMatrix(curTransform.computedTransform);
	}

	protected void resetTransform (Entity rootEntity, Batch batch) {
		TransformComponent curTransform = transformMapper.get(rootEntity);
		batch.setTransformMatrix(curTransform.oldTransform);
	}

	protected void applyShader(Entity entity, Batch batch) {
		if(shaderComponentMapper.has(entity)){
			ShaderComponent shaderComponent = shaderComponentMapper.get(entity);
			if(shaderComponent.getShader() != null) {
				batch.setShader(shaderComponent.getShader());

				batch.getShader().setUniformf("u_delta_time", Gdx.graphics.getDeltaTime());
				batch.getShader().setUniformf("u_time", HyperLap2dRenderer.timeRunning);

				TextureRegionComponent entityTextureRegionComponent = ComponentRetriever.get(entity, TextureRegionComponent.class);
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

					u  = Math.max(0f, Math.min(1f, u  * invScreenWidth));
					v  = Math.max(0f, Math.min(1f, v  * invScreenHeight));
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

	protected void resetShader(Entity entity, Batch batch) {
		if(shaderComponentMapper.has(entity)){
			batch.setShader(null);
		}
	}

	/**
	 * Check if the entity should be rendered in the first screen layer
	 * @param entity
	 * @return false if the entity belongs to a different rendering layer
	 */
	protected boolean checkRenderingLayer(Entity entity) {
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

	public void setRayHandler(RayHandler rayHandler){
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
		fboCameraPool.clear();
	}

    public void resize(int width, int height) {
		frameBufferManager.endCurrent();
		frameBufferManager.dispose("main");
		frameBufferManager.createFBO("main", Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		screenCamera.viewportWidth = width;
		screenCamera.viewportHeight = height;
		screenCamera.position.set(0, 0 , 0);

		screenCamera.translate(screenCamera.viewportWidth * 0.5f, screenCamera.viewportHeight * 0.5f, 0f);
		screenCamera.update();

		invScreenWidth = 1f / screenCamera.viewportWidth;
		invScreenHeight = 1f / screenCamera.viewportHeight;
	}
}

