package games.rednblack.editor.renderer.data;

import com.badlogic.ashley.core.Entity;
import games.rednblack.editor.renderer.components.particle.TalosDataComponent;

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
    public void loadFromEntity(Entity entity) {
        super.loadFromEntity(entity);

        TalosDataComponent talosComponent = entity.getComponent(TalosDataComponent.class);
        particleName = talosComponent.particleName;
        transform = talosComponent.transform;
    }
}
