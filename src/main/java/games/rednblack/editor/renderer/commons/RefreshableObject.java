package games.rednblack.editor.renderer.commons;

public abstract class RefreshableObject implements IRefreshableObject {
    protected boolean needsRefresh = false;

    @Override
    public void scheduleRefresh() {
        needsRefresh = true;
    }

    @Override
    public void executeRefresh(int entity) {
        if (needsRefresh) {
            refresh(entity);
            needsRefresh = false;
        }
    }

    protected abstract void refresh(int entity);
}
