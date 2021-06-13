package games.rednblack.editor.renderer.commons;

/**
 * Implementation Note: Make sure
 */
public interface IRefreshableObject {

    void scheduleRefresh();

    void executeRefresh(int entity);

}
