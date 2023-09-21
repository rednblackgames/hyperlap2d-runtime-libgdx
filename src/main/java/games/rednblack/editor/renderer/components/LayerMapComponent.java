package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import games.rednblack.editor.renderer.data.LayerItemVO;

public class LayerMapComponent  extends PooledComponent {
	public boolean autoIndexing = true;
	private final Array<LayerItemVO> layers = new Array<>();
	private final IntMap<LayerItemVO> layerMap = new IntMap<>();

	public void setLayers(Array<LayerItemVO> layersToAdd) {
		this.layers.addAll(layersToAdd);
		layerMap.clear();
		for (LayerItemVO vo : layers) {
			putLayer(vo.layerName, vo);
		}
	}

	public LayerItemVO getLayer(String name) {
		return layerMap.get(name.hashCode());
	}

	private void putLayer(String name, LayerItemVO itemVO) {
		int hashCode = name.hashCode();
		if (layerMap.containsKey(hashCode)) throw new IllegalArgumentException("Layer name hash collision.");
		layerMap.put(hashCode, itemVO);
	}

	public int getIndexByName(String name) {
		LayerItemVO layer = getLayer(name);
		if (layer != null) {
			return layers.indexOf(layer, false);
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
		putLayer(layerVo.layerName, layerVo);
	}

	public void addLayer(LayerItemVO layerVo) {
		layers.add(layerVo);
		putLayer(layerVo.layerName, layerVo);
	}

	public Array<LayerItemVO> getLayers() {
		return layers;
	}

	public void deleteLayer(String layerName) {
		layers.removeIndex(getIndexByName(layerName));
		layerMap.remove(layerName.hashCode());
	}

	public void rename(String prevName, String newName) {
		LayerItemVO vo = getLayer(prevName);
		vo.layerName = newName;
		layerMap.remove(prevName.hashCode());
		putLayer(newName, vo);
	}

	public void swap(String source, String target) {
		LayerItemVO sourceVO = getLayer(source);
		LayerItemVO targetVO = getLayer(target);
		layers.swap(layers.indexOf(sourceVO, false), layers.indexOf(targetVO, false));
	}

	public String jump(String source, String target) {
		LayerItemVO sourceVO = getLayer(source);
		LayerItemVO targetVO = getLayer(target);
		int sourceIndex = layers.indexOf(sourceVO, false);
		int targetIndex = layers.indexOf(targetVO, false);

		layers.removeIndex(sourceIndex);
		if (targetIndex == layers.size) {
			layers.add(sourceVO);
		} else {
			layers.insert(Math.max(targetIndex, 0), sourceVO);
		}

		return layers.get(sourceIndex).layerName;
	}

	@Override
	public void reset() {
		autoIndexing = true;
		layers.clear();
		layerMap.clear();
	}
}
