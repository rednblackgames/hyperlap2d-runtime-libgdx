package games.rednblack.editor.renderer.systems;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.ScriptComponent;
import games.rednblack.editor.renderer.scripts.BasicScript;
import games.rednblack.editor.renderer.scripts.IScript;

@All(ScriptComponent.class)
public class ScriptSystem extends IteratingSystem {

    protected ComponentMapper<ScriptComponent> scriptComponentMapper;

    @Override
    protected void process(int entity) {
        Array<IScript> scripts = scriptComponentMapper.get(entity).scripts;
        for (int i = 0; i < scripts.size; i++) {
            IScript script = scripts.get(i);
            if (script instanceof BasicScript) {
                BasicScript basicScript = (BasicScript) script;
                if (!basicScript.isInit()) {
                    basicScript.doInit(entity);
                } else {
                    script.act(engine.getDelta());
                }
            } else {
                script.act(engine.getDelta());
            }
        }
    }
}
