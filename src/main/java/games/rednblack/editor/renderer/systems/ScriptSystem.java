package games.rednblack.editor.renderer.systems;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;
import games.rednblack.editor.renderer.systems.strategy.RendererSystem;

/**
 * Created by azakhary on 6/19/2015.
 */
@All(ScriptComponent.class)
public class ScriptSystem extends IteratingSystem implements RendererSystem {

    protected ComponentMapper<ScriptComponent> scriptComponentMapper;

    @Override
    protected void process(int entity) {
        Array<IScript> scripts = scriptComponentMapper.get(entity).scripts;
        for (int i = 0; i < scripts.size; i++) {
            IScript script = scripts.get(i);
            if (script instanceof BasicScript) {
                ((BasicScript) script).doInit(entity);
            }
            script.act(world.delta);
        }
    }
}
