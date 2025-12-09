package games.rednblack.editor.renderer.data;

import com.artemis.World;
import com.badlogic.gdx.math.Vector3;
import games.rednblack.editor.renderer.components.light.LightObjectComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.Objects;

public class LightVO extends MainItemVO {
	//public int itemId = -1;

	public LightObjectComponent.LightType type;
	public int rays = 12;
	public float distance = 300;
	public float directionDegree = 0;
	public float height = 0;
	public Vector3 falloff = new Vector3(1.2f, 0, 5);
	public float coneDegree = 30;
	public float softnessLength = -1f;
	public float intensity = 1f;
	public boolean isStatic = false;
	public boolean isXRay = false;
	public boolean isSoft = false;
	public boolean isActive = true;
	
	public LightVO() {
		tint = new float[4];
		tint[0] = 1f;
		tint[1] = 1f;
		tint[2] = 1f;
		tint[3] = 1f;
	}
	
	public LightVO(LightVO vo) {
		super(vo);
		type = vo.type;
		rays = vo.rays;
		distance = vo.distance;
		directionDegree = vo.directionDegree;
		height = vo.height;
		intensity = vo.intensity;
		coneDegree = vo.coneDegree;
		isStatic = vo.isStatic;
		isXRay = vo.isXRay;
		softnessLength = vo.softnessLength;
		isActive = vo.isActive;
		isSoft = vo.isSoft;
		falloff.set(vo.falloff);
	}

	@Override
	public void loadFromEntity(int entity, World engine, EntityFactory entityFactory) {
		super.loadFromEntity(entity, engine, entityFactory);

		LightObjectComponent lightObjectComponent = ComponentRetriever.get(entity, LightObjectComponent.class, engine);
		type = lightObjectComponent.type;
		rays = lightObjectComponent.rays;
		distance = lightObjectComponent.distance;
		directionDegree = lightObjectComponent.directionDegree;
		height = lightObjectComponent.height;
		coneDegree = lightObjectComponent.coneDegree;
		isStatic = lightObjectComponent.isStatic;
		isXRay = lightObjectComponent.isXRay;
		softnessLength = lightObjectComponent.softnessLength;
		isSoft = lightObjectComponent.isSoft;
		isActive = lightObjectComponent.isActive;
		intensity = lightObjectComponent.intensity;
		falloff.set(lightObjectComponent.falloff);
	}

	@Override
	public String getResourceName() {
		throw new RuntimeException("Light doesn't have resources to load.");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LightVO lightVO = (LightVO) o;
		return rays == lightVO.rays && Float.compare(lightVO.distance, distance) == 0 && Float.compare(lightVO.directionDegree, directionDegree) == 0 && Float.compare(lightVO.height, height) == 0 && Float.compare(lightVO.coneDegree, coneDegree) == 0 && Float.compare(lightVO.softnessLength, softnessLength) == 0 && Float.compare(lightVO.intensity, intensity) == 0 && isStatic == lightVO.isStatic && isXRay == lightVO.isXRay && isSoft == lightVO.isSoft && isActive == lightVO.isActive && type == lightVO.type && falloff.equals(lightVO.falloff);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, rays, distance, directionDegree, height, falloff, coneDegree, softnessLength, intensity, isStatic, isXRay, isSoft, isActive);
	}
}
