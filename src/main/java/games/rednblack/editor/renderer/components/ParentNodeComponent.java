package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.artemis.annotations.EntityId;

public class ParentNodeComponent extends PooledComponent {
    @EntityId public int parentEntity = -1;

    @Override
    public void reset() {
        parentEntity = -1;
    }
}
