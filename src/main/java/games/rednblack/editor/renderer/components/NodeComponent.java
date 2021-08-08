package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.SnapshotArray;

public class NodeComponent extends PooledComponent {
    public SnapshotArray<Integer> children = new SnapshotArray<>(true, 1, Integer.class);

    public void removeChild(int entity) {
        children.removeValue(entity, false);
    }

    public void addChild(int entity) {
        children.add(entity);
    }

    @Override
    public void reset() {
        children.clear();
    }
}
