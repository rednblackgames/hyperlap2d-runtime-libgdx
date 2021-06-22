package games.rednblack.editor.renderer.commons;

import com.artemis.PooledComponent;

public abstract class RefreshableComponent extends PooledComponent {
    protected boolean needsRefresh = false;

    public void scheduleRefresh() {
        needsRefresh = true;
    }

    public void executeRefresh(int entity) {
        if (needsRefresh) {
            refresh(entity);
            needsRefresh = false;
        }
    }

    protected abstract void refresh(int entity);
}
