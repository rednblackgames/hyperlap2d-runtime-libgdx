package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import games.rednblack.editor.renderer.scripts.IScript;

import java.util.Iterator;

/**
 * Created by azakhary on 6/19/2015.
 */
public class ScriptComponent  extends PooledComponent {

    public Array<IScript> scripts = new Array<>();

    public void addScript(IScript script) {
        scripts.add(script);
    }

    public void addScript(Class<? extends IScript> clazz) {
        try {
            IScript script = ClassReflection.newInstance(clazz);
            addScript(script);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    public void removeScript(IScript script) {
        Iterator<IScript> i = scripts.iterator();
        while (i.hasNext()) {
            IScript s = i.next();
            if(s == script) {
                i.remove();
            }
        }
    }

    public void removeScript(Class<? extends IScript> clazz) {
        Iterator<IScript> i = scripts.iterator();
        while (i.hasNext()) {
            IScript s = i.next();
            if(s.getClass() == clazz) {
                i.remove();
            }
        }
    }

    @Override
    public void reset() {
        scripts.clear();
    }
}
