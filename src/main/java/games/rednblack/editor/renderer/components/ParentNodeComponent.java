package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.ecs.annotations.EntityId;

public class ParentNodeComponent extends PooledComponent {
    @EntityId public int parentEntity = -1;

    @Override
    public void reset() {
        parentEntity = -1;
    }
}
