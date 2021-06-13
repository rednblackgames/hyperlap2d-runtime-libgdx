package games.rednblack.editor.renderer.scripts;

/**
 * Created by CyberJoe on 6/19/2015.
 */
public abstract class BasicScript implements IScript {

    protected int entity;

    @Override
    public void init(int item) {
        entity = item;
    }

    public int getEntity() {
        return entity;
    }
}
