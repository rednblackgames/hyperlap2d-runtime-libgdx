package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.artemis.World;
import com.artemis.annotations.Transient;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;

import java.util.Iterator;

/**
 * Created by azakhary on 6/19/2015.
 */
@Transient
public class ScriptComponent extends PooledComponent {
    public static final PoolManager SCRIPTS_POOLS = new PoolManager();

    private static <T extends BasicScript> Pool<T> getOrCreatePool(Class<T> clazz) {
        Pool<T> pool = SCRIPTS_POOLS.getPoolOrNull(clazz);
        if (pool == null) {
            pool = new DefaultPool<>(() -> {
                try {
                    return ClassReflection.newInstance(clazz);
                } catch (ReflectionException e) {
                    throw new RuntimeException(e);
                }
            });
            SCRIPTS_POOLS.addPool(pool);
        }
        return pool;
    }

    public Array<IScript> scripts = new Array<>();
    public World engine;

    public void addScript(IScript script) {
        engine.inject(script);
        scripts.add(script);
    }

    public <T extends BasicScript> T addScript(Class<T> clazz) {
        Pool<T> pool = getOrCreatePool(clazz);
        T script = pool.obtain();
        script.setPool(pool);
        addScript(script);
        return script;
    }

    public void removeScript(IScript script) {
        Iterator<IScript> i = scripts.iterator();
        while (i.hasNext()) {
            IScript s = i.next();
            //Remove from scripts list
            if(s == script) {
                s.dispose();
                //Free script into pool
                if (s instanceof BasicScript) {
                    BasicScript b = (BasicScript) s;
                    if (b.getPool() != null)
                        b.getPool().free(b);
                }

                i.remove();
            }
        }
    }

    public void removeScript(Class<? extends IScript> clazz) {
        Iterator<IScript> i = scripts.iterator();
        while (i.hasNext()) {
            IScript s = i.next();
            //Remove from scripts list
            if(s.getClass() == clazz) {
                s.dispose();
                //Free script into pool
                if (s instanceof BasicScript) {
                    BasicScript b = (BasicScript) s;
                    if (b.getPool() != null)
                        b.getPool().free(b);
                }

                i.remove();
            }
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < scripts.size; i++) {
            IScript script = scripts.get(i);
            script.dispose();
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
