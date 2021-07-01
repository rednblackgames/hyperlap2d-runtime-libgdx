package games.rednblack.editor.renderer.scripts;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Pool;

/**
 * Created by CyberJoe on 6/19/2015.
 */
public abstract class BasicScript implements IScript, Pool.Poolable {
    private Pool pool;
    protected Entity entity;

    @Override
    public void init(Entity item) {
        entity = item;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void reset() {
        pool = null;
        entity = null;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public Pool getPool() {
        return pool;
    }
}
