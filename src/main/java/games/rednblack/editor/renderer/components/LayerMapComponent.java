package games.rednblack.editor.renderer.components;

import java.util.HashMap;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.data.LayerItemVO;

public class LayerMapComponent  extends PooledComponent {
	public boolean autoIndexing = true;
	private final Array<LayerItemVO> layers = new Array<>();

	private final HashMap<String, LayerItemVO> layerMap = new HashMap<>();

	public void setLayers(Array<LayerItemVO> layersToAdd) {
		this.layers.addAll(layersToAdd);
		layerMap.clear();
		for (LayerItemVO vo : layers) {
			layerMap.put(vo.layerName, vo);
		}
	}

	public LayerItemVO getLayer(String name) {
		return layerMap.get(name);
	}

	public int getIndexByName(String name) {
		if(layerMap.containsKey(name)) {
			return layers.indexOf(layerMap.get(name), false);
		}

		return 0;
	}

	public boolean isVisible(String name) {
		LayerItemVO vo = getLayer(name);
		if (vo != null) {
			return vo.isVisible;
		}

		return true;
	}

	public void addLayer(int index, LayerItemVO layerVo) {
		layers.insert(index, layerVo);
		layerMap.put(layerVo.layerName, layerVo);
	}

	public void addLayer(LayerItemVO layerVo) {
		layers.add(layerVo);
		layerMap.put(layerVo.layerName, layerVo);
	}

	public Array<LayerItemVO> getLayers() {
		return layers;
	}

	public void deleteLayer(String layerName) {
		layers.removeIndex(getIndexByName(layerName));
		layerMap.remove(layerName);
	}

	public void rename(String prevName, String newName) {
		LayerItemVO vo = layerMap.get(prevName);
		vo.layerName = newName;
		layerMap.remove(prevName);
		layerMap.put(newName, vo);
	}

	public void swap(String source, String target) {
		LayerItemVO sourceVO = getLayer(source);
		LayerItemVO targetVO = getLayer(target);
		layers.swap(layers.indexOf(sourceVO, false), layers.indexOf(targetVO, false));
	}

	@Override
	public void reset() {
		autoIndexing = true;
		layers.clear();
		layerMap.clear();
	}
}
