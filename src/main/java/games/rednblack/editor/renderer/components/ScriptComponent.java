package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.artemis.World;
import com.artemis.annotations.Transient;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;

import java.util.Iterator;

/**
 * Created by azakhary on 6/19/2015.
 */
@Transient
public class ScriptComponent  extends PooledComponent {
    public static int SCRIPTS_POOL_SIZE = 100;

    public Array<IScript> scripts = new Array<>();
    public World engine;

    public void addScript(IScript script) {
        engine.inject(script);
        scripts.add(script);
    }

    public <T extends BasicScript> T addScript(Class<T> clazz) {
        Pool<T> pool = Pools.get(clazz, SCRIPTS_POOL_SIZE);
        T script = pool.obtain();
        script.setPool(pool);
        addScript(script);
        return script;
    }

    public void removeScript(IScript script) {
        Iterator<IScript> i = scripts.iterator();
        while (i.hasNext()) {
            IScript s = i.next();
            //Free script into pool
            if (s instanceof BasicScript) {
                BasicScript b = (BasicScript) s;
                if (b.getPool() != null)
                    b.getPool().free(b);
            }
            //Remove from scripts list
            if(s == script) {
                i.remove();
            }
        }
    }

    public void removeScript(Class<? extends IScript> clazz) {
        Iterator<IScript> i = scripts.iterator();
        while (i.hasNext()) {
            IScript s = i.next();
            //Free script into pool
            if (s instanceof BasicScript) {
                BasicScript b = (BasicScript) s;
                if (b.getPool() != null)
                    b.getPool().free(b);
            }
            //Remove from scripts list
            if(s.getClass() == clazz) {
                i.remove();
            }
        }
    }

    @Override
    public void reset() {
        for (IScript script : scripts) {
            //Free script into pool
            if (script instanceof BasicScript) {
                BasicScript b = (BasicScript) script;
                if (b.getPool() != null)
                    b.getPool().free(b);
            }
        }
        //Remove all scripts
        scripts.clear();
    }
}
