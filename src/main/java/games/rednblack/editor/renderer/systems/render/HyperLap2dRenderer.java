package games.rednblack.editor.renderer.systems.render;

import box2dLight.RayHandler;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.commons.IExternalItemType;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.data.ShaderUniformVO;
import games.rednblack.editor.renderer.systems.render.logic.DrawableLogicMapper;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.Map;

public class HyperLap2dRenderer extends IteratingSystem {
	private ComponentMapper<ViewPortComponent> viewPortMapper = ComponentMapper.getFor(ViewPortComponent.class);
	private ComponentMapper<CompositeTransformComponent> compositeTransformMapper = ComponentMapper.getFor(CompositeTransformComponent.class);
	private ComponentMapper<NodeComponent> nodeMapper = ComponentMapper.getFor(NodeComponent.class);
	private ComponentMapper<ParentNodeComponent> parentNodeMapper = ComponentMapper.getFor(ParentNodeComponent.class);
	private ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
	private ComponentMapper<MainItemComponent> mainItemComponentMapper = ComponentMapper.getFor(MainItemComponent.class);
	private ComponentMapper<ShaderComponent> shaderComponentMapper = ComponentMapper.getFor(ShaderComponent.class);
	private ComponentMapper<DimensionsComponent> dimensionsComponentComponentMapper = ComponentMapper.getFor(DimensionsComponent.class);
	
	private DrawableLogicMapper drawableLogicMapper;
	private RayHandler rayHandler;
	private Camera camera;

	public static float timeRunning = 0;
	
	public Batch batch;

	private final FrameBufferManager frameBufferManager;
	private FrameBuffer screenFBO;
	private Camera screenCamera;

	public HyperLap2dRenderer(Batch batch) {
		super(Family.all(ViewPortComponent.class).get());
		this.batch = batch;
		drawableLogicMapper = new DrawableLogicMapper();

		frameBufferManager = new FrameBufferManager();
		screenFBO = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		screenCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		screenCamera.translate(screenCamera.viewportWidth / 2f, screenCamera.viewportHeight / 2f, 0f);
		screenCamera.update();
	}

	public void addDrawableType(IExternalItemType itemType) {
		drawableLogicMapper.addDrawableToMap(itemType.getTypeId(), itemType.getDrawable());
	}

	@Override
	public void processEntity(Entity entity, float deltaTime) {
		timeRunning+=deltaTime;

		ViewPortComponent ViewPortComponent = viewPortMapper.get(entity);
		Viewport viewport = ViewPortComponent.viewPort;
		camera = viewport.getCamera();

		frameBufferManager.begin(screenFBO);
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		camera.update();
		batch.setProjectionMatrix(camera.combined);

		batch.begin();
		drawRecursively(entity, 1f);
		batch.end();
		frameBufferManager.end(screenFBO);

		Texture t = screenFBO.getColorBufferTexture();
		int w = t.getWidth();
		int h = t.getHeight();

		batch.begin();
		batch.setProjectionMatrix(screenCamera.combined);
		batch.draw(t,
				0, 0,
				0, 0,
				w, h,
				1, 1,
				0,
				0, 0,
				w, h,
				false, true);
		batch.end();

		if (rayHandler != null) {
			OrthographicCamera orthoCamera = (OrthographicCamera) camera;

			rayHandler.setCombinedMatrix(orthoCamera);
			rayHandler.updateAndRender();
		}
		//debugRenderer.render(world, camera.combined);
	}

	private void drawRecursively(Entity rootEntity, float parentAlpha) {
		CompositeTransformComponent curCompositeTransformComponent = compositeTransformMapper.get(rootEntity);
		TransformComponent transform = transformMapper.get(rootEntity);

		boolean scissors = false;

		if (curCompositeTransformComponent.transform || transform.rotation != 0 || transform.scaleX !=1 || transform.scaleY !=1){
			computeTransform(rootEntity);
			applyTransform(rootEntity, batch);
		}

		if (curCompositeTransformComponent.scissorsEnabled) {
			batch.flush();
			//TODO Scissors rectangle does not rotate.. why? Uhm
			ScissorStack.calculateScissors(camera, curCompositeTransformComponent.oldTransform, curCompositeTransformComponent.clipBounds, curCompositeTransformComponent.scissors);
			if (ScissorStack.pushScissors(curCompositeTransformComponent.scissors)) {
				scissors = true;
			}
		}

		applyShader(rootEntity, batch);

        TintComponent tintComponent = ComponentRetriever.get(rootEntity, TintComponent.class);
        parentAlpha *= tintComponent.color.a;

		drawChildren(rootEntity, batch, curCompositeTransformComponent, parentAlpha);

		if (curCompositeTransformComponent.transform || transform.rotation != 0 || transform.scaleX !=1 || transform.scaleY !=1)
			resetTransform(rootEntity, batch);

		resetShader(rootEntity, batch);

		if (scissors) {
			batch.flush();
			ScissorStack.popScissors();
		}
	}

	private void drawChildren(Entity rootEntity, Batch batch, CompositeTransformComponent curCompositeTransformComponent, float parentAlpha) {
		NodeComponent nodeComponent = nodeMapper.get(rootEntity);
		Entity[] children = nodeComponent.children.begin();
		TransformComponent transform = transformMapper.get(rootEntity);
		if (curCompositeTransformComponent.transform || transform.rotation != 0 || transform.scaleX !=1 || transform.scaleY !=1) {
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
				
				int entityType = childMainItemComponent.entityType;

				NodeComponent childNodeComponent = nodeMapper.get(child);
				
				if(childNodeComponent ==null){
					drawEntity(entityType, batch, child, parentAlpha);
				}else{
					//Step into Composite
					drawRecursively(child, parentAlpha);
				}
			}
		} else {
			// No transform for this group, offset each child.
			TransformComponent compositeTransform = transformMapper.get(rootEntity);
			
			float offsetX = compositeTransform.x, offsetY = compositeTransform.y;
			
			if(viewPortMapper.has(rootEntity)){
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
				int entityType = mainItemComponentMapper.get(child).entityType;
				
				if(childNodeComponent ==null){
					drawEntity(entityType, batch, child, parentAlpha);
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

	private void drawEntity(int entityType, Batch batch, Entity child, float parentAlpha) {
		applyShader(child, batch);
		//Find the logic from mapper and draw it
		drawableLogicMapper.getDrawable(entityType).draw(batch, child, parentAlpha);

		resetShader(child, batch);
	}

	/** Returns the transform for this group's coordinate system. 
	 * @param rootEntity */
	protected Matrix4 computeTransform(Entity rootEntity) {
		CompositeTransformComponent curCompositeTransformComponent = compositeTransformMapper.get(rootEntity);
		ParentNodeComponent parentNodeComponent = parentNodeMapper.get(rootEntity);
		TransformComponent curTransform = transformMapper.get(rootEntity);
		Affine2 worldTransform = curCompositeTransformComponent.worldTransform;

		float originX = curTransform.originX;
		float originY = curTransform.originY;
		float x = curTransform.x;
		float y = curTransform.y;
		float rotation = curTransform.rotation;
		float scaleX = curTransform.scaleX;
		float scaleY = curTransform.scaleY;

		worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY);
		if (originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

		// Find the parent that transforms.
		
		CompositeTransformComponent parentTransformComponent = null;
		
		Entity parentEntity = null;
		if(parentNodeComponent != null){
			parentEntity = parentNodeComponent.parentEntity;
		}
		
		if (parentEntity != null){
			parentTransformComponent = compositeTransformMapper.get(parentEntity);
			TransformComponent transform = transformMapper.get(parentEntity);
			if(curCompositeTransformComponent.transform || transform.rotation != 0 || transform.scaleX !=1 || transform.scaleY !=1)
				worldTransform.preMul(parentTransformComponent.worldTransform);
		}

		curCompositeTransformComponent.computedTransform.set(worldTransform);
		return curCompositeTransformComponent.computedTransform;
	}

	protected void applyTransform (Entity rootEntity, Batch batch) {
		CompositeTransformComponent curCompositeTransformComponent = compositeTransformMapper.get(rootEntity);
		curCompositeTransformComponent.oldTransform.set(batch.getTransformMatrix());
		batch.setTransformMatrix(curCompositeTransformComponent.computedTransform);
	}

	protected void resetTransform (Entity rootEntity, Batch batch) {
		CompositeTransformComponent curCompositeTransformComponent = compositeTransformMapper.get(rootEntity);
		batch.setTransformMatrix(curCompositeTransformComponent.oldTransform);
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
	
	public void setRayHandler(RayHandler rayHandler){
		this.rayHandler = rayHandler;
	}

	public Batch getBatch() {
        return batch;
    }

    public void dispose() {
		screenFBO.dispose();
	}

    public void resize(int width, int height) {
		frameBufferManager.end(screenFBO);
		screenFBO.dispose();
		screenFBO = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
		screenCamera.viewportWidth = width;
		screenCamera.viewportHeight = height;
		screenCamera.position.set(0, 0 , 0);

		screenCamera.translate(screenCamera.viewportWidth / 2f, screenCamera.viewportHeight / 2f, 0f);
		screenCamera.update();
	}
}

