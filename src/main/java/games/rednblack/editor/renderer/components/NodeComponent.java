package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.artemis.annotations.EntityId;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.utils.SnapshotArray;

public class NodeComponent extends PooledComponent {
    public transient SnapshotArray<Integer> children = new SnapshotArray<>(true, 1, Integer.class);
    @EntityId public IntBag persistentChildren = new IntBag();

    public void removeChild(int entity) {
        children.removeValue(entity, false);
        persistentChildren.removeValue(entity);
    }

    public void addChild(int entity) {
        children.add(entity);
        persistentChildren.add(entity);
    }

    @Override
    public void reset() {
        children.clear();
        persistentChildren.clear();
    }
}
