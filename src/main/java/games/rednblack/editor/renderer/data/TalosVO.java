package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.particle.TalosDataComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class TalosVO extends MainItemVO {
    public String particleName = "";
    public boolean transform = true;

    public TalosVO() {
        super();
    }

    public TalosVO(TalosVO vo) {
        super(vo);
        particleName = vo.particleName;
        transform = vo.transform;
    }

    @Override
    public void loadFromEntity(int entity) {
        super.loadFromEntity(entity);

        TalosDataComponent talosComponent = ComponentRetriever.get(entity, TalosDataComponent.class);
        particleName = talosComponent.particleName;
        transform = talosComponent.transform;
    }
}
