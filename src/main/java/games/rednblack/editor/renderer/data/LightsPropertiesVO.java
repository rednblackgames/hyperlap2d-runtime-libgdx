package games.rednblack.editor.renderer.data;

public class LightsPropertiesVO {
    public boolean enabled;
    public boolean pseudo3d;
    public float[] ambientColor;
    public int blurNum;
    public int lightMapScale;
    public String lightType; //DIFFUSE, DIRECTIONAL

    public int directionalRays;
    public float directionalDegree;
    public float directionalHeight;
    public float[] directionalColor;

    public LightsPropertiesVO() {
        blurNum = 1;
        lightMapScale = 4;
        lightType = "DIFFUSE";
        enabled = false;
        pseudo3d = false;
        directionalRays = 12;
        directionalDegree = 0;
        directionalHeight = 0;
        ambientColor = new float[]{1f, 1f, 1f, 1f};
        directionalColor = new float[]{1f, 1f, 1f, 1f};
    }

    public LightsPropertiesVO(LightsPropertiesVO lightsPropertiesVO) {
        this.enabled = lightsPropertiesVO.enabled;
        this.pseudo3d = lightsPropertiesVO.pseudo3d;
        this.blurNum = lightsPropertiesVO.blurNum;
        this.lightMapScale = lightsPropertiesVO.lightMapScale;
        this.lightType = lightsPropertiesVO.lightType;
        this.directionalRays = lightsPropertiesVO.directionalRays;
        this.directionalDegree = lightsPropertiesVO.directionalDegree;
        this.directionalHeight = lightsPropertiesVO.directionalHeight;
        this.ambientColor = new float[4];
        System.arraycopy(lightsPropertiesVO.ambientColor, 0, this.ambientColor, 0, ambientColor.length);
        this.directionalColor = new float[4];
        System.arraycopy(lightsPropertiesVO.directionalColor, 0, this.directionalColor, 0, directionalColor.length);
    }
}
