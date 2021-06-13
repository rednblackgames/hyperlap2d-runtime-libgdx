package games.rednblack.editor.renderer.scripts;

public interface IScript {
    void init(int entity);

    void act(float delta);

    void dispose();
}
