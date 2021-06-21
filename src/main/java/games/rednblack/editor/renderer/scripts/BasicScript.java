package games.rednblack.editor.renderer.scripts;

import com.badlogic.gdx.utils.Pool;

/**
 * Created by CyberJoe on 6/19/2015.
 */
public abstract class BasicScript implements IScript, Pool.Poolable {
    private Pool pool;
    protected int entity;

    @Override
    public void init(int item) {
        entity = item;
    }

    public int getEntity() {
        return entity;
    }

    @Override
    public void reset() { }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public Pool getPool() {
        return pool;
    }
}
