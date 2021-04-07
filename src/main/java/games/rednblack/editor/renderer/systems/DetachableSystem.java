package games.rednblack.editor.renderer.systems;

import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

public abstract class DetachableSystem extends IteratingSystem {

    private boolean detached = false;

    public DetachableSystem(Family family) {
        super(family);
    }

    public DetachableSystem(Family family, int priority) {
        super(family, priority);
    }

    @Override
    public void update(float deltaTime) {
        if (!detached)
            super.update(deltaTime);
    }

    public void manualUpdate(float deltaTime) {
        super.update(deltaTime);
    }

    public final void detach() {
        detached = true;
    }

    public boolean isDetached() {
        return detached;
    }
}
