package games.rednblack.editor.renderer.scripts;

import com.badlogic.ashley.core.Entity;


public interface IScript {
    void init(Entity entity);

    void act(float delta);

    void dispose();
}
