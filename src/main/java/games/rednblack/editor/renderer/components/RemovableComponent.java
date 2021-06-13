package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;

public abstract class RemovableComponent extends PooledComponent {
    abstract void onRemove();
}
