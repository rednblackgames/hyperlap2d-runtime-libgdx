package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;

public class CircleShapeComponent extends PooledComponent {
    public float radius = 1;

    @Override
    protected void reset() {
        radius = 1;
    }
}
