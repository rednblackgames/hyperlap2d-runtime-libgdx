package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import games.rednblack.editor.renderer.data.LayerItemVO;

public class LayerMapComponent  extends PooledComponent {
	public boolean autoIndexing = true;
	private final Array<LayerItemVO> layers = new Array<>();
	private final IntMap<LayerItemVO> layerMap = new IntMap<>();

	public void setLayers(Array<LayerItemVO> layersToAdd) {
		for (int i = 0; i < layersToAdd.size; i++) {
			this.layers.add(new LayerItemVO(layersToAdd.get(i)));
		}
		layerMap.clear();
		for (LayerItemVO vo : layers) {
			putLayer(vo.layerName, vo);
		}
	}

	public LayerItemVO getLayer(int nameHashCode) {
		return layerMap.get(nameHashCode);
	}

	private void putLayer(String name, LayerItemVO itemVO) {
		int hashCode = name.hashCode();
		if (layerMap.containsKey(hashCode)) throw new IllegalArgumentException("Layer name hash collision.");
		layerMap.put(hashCode, itemVO);
	}

	public int getIndexByName(int nameHashCode) {
		LayerItemVO layer = getLayer(nameHashCode);
		if (layer != null) {
			return layers.indexOf(layer, false);
		}
		return 0;
	}

	public boolean isVisible(int layerHashcode) {
		LayerItemVO vo = getLayer(layerHashcode);
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
		layers.removeIndex(getIndexByName(layerName.hashCode()));
		layerMap.remove(layerName.hashCode());
	}

	public void rename(String prevName, String newName) {
		LayerItemVO vo = getLayer(prevName.hashCode());
		vo.layerName = newName;
		layerMap.remove(prevName.hashCode());
		putLayer(newName, vo);
	}

	public void swap(String source, String target) {
		LayerItemVO sourceVO = getLayer(source.hashCode());
		LayerItemVO targetVO = getLayer(target.hashCode());
		layers.swap(layers.indexOf(sourceVO, false), layers.indexOf(targetVO, false));
	}

	public String jump(String source, String target) {
		LayerItemVO sourceVO = getLayer(source.hashCode());
		LayerItemVO targetVO = getLayer(target.hashCode());
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
