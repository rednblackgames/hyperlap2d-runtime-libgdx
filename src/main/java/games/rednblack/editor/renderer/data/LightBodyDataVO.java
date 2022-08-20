package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.math.Vector3;
import games.rednblack.editor.renderer.components.light.LightBodyComponent;

import java.util.Arrays;
import java.util.Objects;

public class LightBodyDataVO {
    public float[] color;
    public int rays;
    public float distance;
    public float intensity = 1f;
    public float height = 0;
    public Vector3 falloff = new Vector3();
    public int rayDirection;
    public float softnessLength;
    public boolean isStatic;
    public boolean isXRay;
    public boolean isSoft;
    public boolean isActive;

    public LightBodyDataVO() {
        color = new float[]{1f, 1f, 1f, 1f};
    }

    public LightBodyDataVO(LightBodyDataVO vo){
        color = new float[4];
        System.arraycopy(vo.color, 0, this.color, 0, color.length);
        rays = vo.rays;
        distance = vo.distance;
        rayDirection = vo.rayDirection;
        softnessLength = vo.softnessLength;
        isStatic = vo.isStatic;
        isXRay = vo.isXRay;
        isSoft = vo.isSoft;
        isActive = vo.isActive;
        intensity = vo.intensity;
        height = vo.height;
        falloff.set(vo.falloff);
    }

    public void loadFromComponent(LightBodyComponent lightComponent) {
        color = new float[4];
        System.arraycopy(lightComponent.color, 0, this.color, 0, color.length);
        rays = lightComponent.rays;
        distance = lightComponent.distance;
        rayDirection = lightComponent.rayDirection;
        softnessLength = lightComponent.softnessLength;
        isStatic = lightComponent.isStatic;
        isXRay = lightComponent.isXRay;
        isSoft = lightComponent.isSoft;
        isActive = lightComponent.isActive;
        intensity = lightComponent.intensity;
        height = lightComponent.height;
        falloff.set(lightComponent.falloff);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LightBodyDataVO that = (LightBodyDataVO) o;
        return rays == that.rays && Float.compare(that.distance, distance) == 0 && Float.compare(that.intensity, intensity) == 0 && Float.compare(that.height, height) == 0 && rayDirection == that.rayDirection && Float.compare(that.softnessLength, softnessLength) == 0 && isStatic == that.isStatic && isXRay == that.isXRay && isSoft == that.isSoft && isActive == that.isActive && Arrays.equals(color, that.color) && falloff.equals(that.falloff);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rays, distance, intensity, height, falloff, rayDirection, softnessLength, isStatic, isXRay, isSoft, isActive);
        result = 31 * result + Arrays.hashCode(color);
        return result;
    }
}
