package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.light.LightBodyComponent;

public class LightBodyDataVO {
    public float[] color;
    public int rays;
    public float distance;
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
    }
}
