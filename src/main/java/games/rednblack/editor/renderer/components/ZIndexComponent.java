package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;

public class ZIndexComponent  extends PooledComponent {
    private int zIndex = 0;
    public boolean needReOrder = false;
    private String layerName = "";
    public int layerHash = layerName.hashCode();
    public int layerIndex;

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
        needReOrder = true;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
        this.layerHash = layerName.hashCode();
    }

    public String getLayerName() {
        return layerName;
    }

    public int getGlobalZIndex() {
        return layerIndex + zIndex;
    }

    @Override
    public void reset() {
        zIndex = 0;
        needReOrder = false;
        layerIndex = 0;
        setLayerName("");
    }
}