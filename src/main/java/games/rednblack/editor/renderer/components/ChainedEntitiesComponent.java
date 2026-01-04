package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import com.badlogic.gdx.utils.IntArray;

public class ChainedEntitiesComponent extends PooledComponent {
    public IntArray chainedEntities = new IntArray();

    @Override
    protected void reset() {
        chainedEntities.clear();
    }
}
