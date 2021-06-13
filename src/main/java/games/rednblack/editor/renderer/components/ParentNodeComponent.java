package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;

public class ParentNodeComponent extends PooledComponent {
    public int parentEntity = -1;

    @Override
    public void reset() {
        parentEntity = -1;
    }
}
