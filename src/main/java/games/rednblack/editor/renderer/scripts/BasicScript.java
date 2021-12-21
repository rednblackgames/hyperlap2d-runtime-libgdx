package games.rednblack.editor.renderer.scripts;

import com.badlogic.gdx.utils.Pool;

/**
 * Created by CyberJoe on 6/19/2015.
 */
public abstract class BasicScript implements IScript, Pool.Poolable {
    private Pool pool;
    private boolean isInit = false;
    protected int entity = -1;

    public final void doInit(int item) {
        if (isInit) return;
        init(item);
    }

    @Override
    public void init(int item) {
        entity = item;
        isInit = true;
    }

    public int getEntity() {
        return entity;
    }

    @Override
    public void reset() {
        pool = null;
        entity = -1;
        isInit = false;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public Pool getPool() {
        return pool;
    }
}
