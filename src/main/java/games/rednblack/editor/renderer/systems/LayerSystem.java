package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.utils.SnapshotArray;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.LayerMapComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;

import java.util.Comparator;

@All(CompositeTransformComponent.class)
public class LayerSystem extends IteratingSystem {

    private final Comparator<Integer> comparator = new ZComparator();

    protected ComponentMapper<ZIndexComponent> zIndexMapper;
    protected ComponentMapper<LayerMapComponent> layerMapper;
    protected ComponentMapper<NodeComponent> nodeMapper;

	@Override
	protected void process(int entityId) {
        NodeComponent nodeComponent = nodeMapper.get(entityId);
        if (nodeComponent.children.size != nodeComponent.persistentChildren.size()) {
            for(int i = 0; i < nodeComponent.persistentChildren.size(); i ++) {
                int entityID = nodeComponent.persistentChildren.get(i);
                if(!nodeComponent.children.contains(entityID, false))
                    nodeComponent.children.add(entityID);
            }
        }
        LayerMapComponent layerMapComponent = layerMapper.get(entityId);
        updateLayers(nodeComponent.children, layerMapComponent);

        sort(nodeComponent.children);

        if (layerMapComponent.autoIndexing) {
            updateZIndices(nodeComponent.children);
        }
    }

    private void updateLayers(SnapshotArray<Integer> children, LayerMapComponent layerMapComponent) {
        for (int i = 0; i < children.size; i++) {
            Integer entity = children.get(i);
            ZIndexComponent zindexComponent = zIndexMapper.get(entity);
            zindexComponent.layerIndex = getLayerIndexByName(zindexComponent.layerName, layerMapComponent);
            if (zindexComponent.needReOrder && layerMapComponent.autoIndexing) {
                if (zindexComponent.getZIndex() < 0) throw new IllegalArgumentException("ZIndex cannot be < 0.");
                if (children.size == 1) {
                    zindexComponent.setZIndex(0);
                    zindexComponent.needReOrder = false;
                    return;
                }
                if (!children.removeValue(entity, true)) return;
                if (zindexComponent.getZIndex() >= children.size)
                    children.add(entity);
                else
                    children.insert(zindexComponent.getZIndex(), entity);
            }
        }
    }

    private void updateZIndices(SnapshotArray<Integer> children) {
        for (int i = 0; i < children.size; i++) {
            Integer entity = children.get(i);
            ZIndexComponent zindexComponent = zIndexMapper.get(entity);
            zindexComponent.setZIndex(i);
            zindexComponent.needReOrder = false;
        }
    }

    private void sort(SnapshotArray<Integer> children) {
        children.sort(comparator);
    }

    private int getLayerIndexByName(String layerName, LayerMapComponent layerMapComponent) {
        if (layerMapComponent == null) {
            return 0;
        }
        return layerMapComponent.getIndexByName(layerName);
    }

    private class ZComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer e1, Integer e2) {
            ZIndexComponent zIndexComponent1 = zIndexMapper.get(e1);
            ZIndexComponent zIndexComponent2 = zIndexMapper.get(e2);
            return zIndexComponent1.layerIndex == zIndexComponent2.layerIndex ? Integer.signum(zIndexComponent1.getZIndex() - zIndexComponent2.getZIndex()) : Integer.signum(zIndexComponent1.layerIndex - zIndexComponent2.layerIndex);
        }
    }

}
