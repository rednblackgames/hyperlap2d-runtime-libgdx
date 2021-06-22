package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.SnapshotArray;


// TODO: Check if i can change Entity to Integer
public class NodeComponent extends PooledComponent {
    public SnapshotArray<Integer> children = new SnapshotArray<>(true, 1, Integer.class);

    public void removeChild(Integer entity) {
        children.removeValue(entity, false);
    }

    public void addChild(Integer entity) {
        children.add(entity);
    }

    @Override
    public void reset() {
        children.clear();
    }
}
