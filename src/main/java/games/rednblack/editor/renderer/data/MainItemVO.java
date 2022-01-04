package games.rednblack.editor.renderer.data;

import java.util.Arrays;

import com.artemis.World;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.components.physics.SensorComponent;
import games.rednblack.editor.renderer.components.shape.CircleShapeComponent;
import games.rednblack.editor.renderer.components.shape.PolygonShapeComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public abstract class MainItemVO {
	public enum RenderingLayer {SCREEN, SCREEN_READING}

	public int uniqueId = -1;
	public String itemIdentifier = "";
	public String itemName = "";
    public String[] tags = null;
    public ObjectMap<String, String> customVariables = new ObjectMap<>();
	public float x; 
	public float y;
	public float scaleX	= 1f;
	public float scaleY	= 1f;
	public float originX = Float.NaN;
	public float originY = Float.NaN;
	public float rotation;
	public int zIndex = 0;
	public String layerName = "";
	public float[] tint = {1, 1, 1, 1};
	public boolean flipX = false;
	public boolean flipY = false;

	public String shaderName = "";
	public ObjectMap<String, ShaderUniformVO> shaderUniforms = new ObjectMap<>();
	public RenderingLayer renderingLayer = RenderingLayer.SCREEN;

	public PolygonShapeVO shape = null;
	public PhysicsBodyDataVO physics = null;
	public LightBodyDataVO light = null;
	public SensorDataVO sensor = null;
	public Circle circle = null;
	
	public MainItemVO() {
		
	}
	
	public MainItemVO(MainItemVO vo) {
		uniqueId = vo.uniqueId;
		itemIdentifier = vo.itemIdentifier;
		itemName = vo.itemName;
        if(vo.tags != null) tags = Arrays.copyOf(vo.tags, vo.tags.length);
		customVariables.putAll(vo.customVariables);
		x = vo.x; 
		y = vo.y;
		rotation = vo.rotation;
		zIndex = vo.zIndex;
		layerName = vo.layerName;
		if(vo.tint != null) tint = Arrays.copyOf(vo.tint, vo.tint.length);
		scaleX = vo.scaleX;
		scaleY = vo.scaleY;
		originX = vo.originX;
		originY = vo.originY;
		flipX = vo.flipX;
		flipY = vo.flipY;

		if(vo.shape != null) {
			shape = vo.shape.clone();
		}

		if(vo.circle != null) {
			circle = new Circle(vo.circle);
		}

		if(vo.physics != null){
            physics = new PhysicsBodyDataVO(vo.physics);
		}
		
		if (vo.sensor != null) {
			sensor = new SensorDataVO(vo.sensor);
		}

		if(vo.light != null){
			light = new LightBodyDataVO(vo.light);
		}

		shaderName = vo.shaderName;
		shaderUniforms.clear();
		shaderUniforms.putAll(vo.shaderUniforms);

		renderingLayer = vo.renderingLayer;
    }

	public void loadFromEntity(int entity, World engine, EntityFactory entityFactory) {
		MainItemComponent mainItemComponent = ComponentRetriever.get(entity, MainItemComponent.class, engine);
		TransformComponent transformComponent = ComponentRetriever.get(entity, TransformComponent.class, engine);
		transformComponent = transformComponent.getRealComponent();
		TintComponent tintComponent = ComponentRetriever.get(entity, TintComponent.class, engine);
		ZIndexComponent zindexComponent = ComponentRetriever.get(entity, ZIndexComponent.class, engine);

		uniqueId = mainItemComponent.uniqueId;
		itemIdentifier = mainItemComponent.itemIdentifier;
		itemName = mainItemComponent.libraryLink;
		tags = new String[mainItemComponent.tags.size];
		int i = 0;
		for (String tag : mainItemComponent.tags) {
			tags[i++] = tag;
		}
		customVariables.putAll(mainItemComponent.customVariables);

		x = transformComponent.x;
		y = transformComponent.y;
		scaleX = transformComponent.scaleX;
		scaleY = transformComponent.scaleY;
		originX = transformComponent.originX;
		originY = transformComponent.originY;
		rotation = transformComponent.rotation;

		flipX = transformComponent.flipX;
		flipY = transformComponent.flipY;

        layerName = zindexComponent.layerName;

		tint = new float[4];
		tint[0] = tintComponent.color.r;
		tint[1] = tintComponent.color.g;
		tint[2] = tintComponent.color.b;
		tint[3] = tintComponent.color.a;

		zIndex = zindexComponent.getZIndex();

		//Secondary components
		PolygonShapeComponent polygonShapeComponent = ComponentRetriever.get(entity, PolygonShapeComponent.class, engine);
		if(polygonShapeComponent != null && polygonShapeComponent.vertices != null) {
			shape = new PolygonShapeVO();
			shape.vertices = polygonShapeComponent.vertices;
			shape.polygonizedVertices = polygonShapeComponent.polygonizedVertices;
			shape.openEnded = polygonShapeComponent.openEnded;
		}

		CircleShapeComponent circleComponent = ComponentRetriever.get(entity, CircleShapeComponent.class, engine);
		if(circleComponent != null) {
			circle = new Circle(originX, originY, circleComponent.radius);
		}

        PhysicsBodyComponent physicsComponent = ComponentRetriever.get(entity, PhysicsBodyComponent.class, engine);
        if(physicsComponent != null) {
            physics = new PhysicsBodyDataVO();
            physics.loadFromComponent(physicsComponent);
        }

        SensorComponent sensorComponent = ComponentRetriever.get(entity, SensorComponent.class, engine);
        if (sensorComponent != null) {
        	sensor = new SensorDataVO();
        	sensor.loadFromComponent(sensorComponent);
        }

        LightBodyComponent lightBodyComponent = ComponentRetriever.get(entity, LightBodyComponent.class, engine);
        if (lightBodyComponent != null) {
        	light = new LightBodyDataVO();
        	light.loadFromComponent(lightBodyComponent);
		}

		ShaderComponent shaderComponent = ComponentRetriever.get(entity, ShaderComponent.class, engine);
		if(shaderComponent != null && shaderComponent.shaderName != null) {
			shaderName = shaderComponent.shaderName;
			shaderUniforms.clear();
			shaderUniforms.putAll(shaderComponent.customUniforms);
			renderingLayer = shaderComponent.renderingLayer;
		}
	}

	public abstract String getResourceName();
}
