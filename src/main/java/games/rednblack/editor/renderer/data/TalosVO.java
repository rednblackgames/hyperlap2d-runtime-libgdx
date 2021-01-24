package games.rednblack.editor.renderer.data;

import com.badlogic.ashley.core.Entity;
import games.rednblack.editor.renderer.components.additional.TalosComponent;

public class TalosVO extends MainItemVO {
    public String particleName = "";

    public TalosVO() {
        super();
    }

    public TalosVO(TalosVO vo) {
        super(vo);
        particleName = vo.particleName;
    }

    @Override
    public void loadFromEntity(Entity entity) {
        super.loadFromEntity(entity);

        TalosComponent particleComponent = entity.getComponent(TalosComponent.class);
        particleName = particleComponent.particleName;
    }
}
