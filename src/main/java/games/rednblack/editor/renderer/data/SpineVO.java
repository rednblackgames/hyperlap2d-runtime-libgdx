package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.SpineDataComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class SpineVO extends MainItemVO {

    public String animationName = "";
    public String currentAnimationName = "";

    public SpineVO() {

    }

    public SpineVO(SpineVO vo) {
        super(vo);
        animationName = vo.animationName;
        currentAnimationName = vo.currentAnimationName;
    }

    @Override
    public void loadFromEntity(int entity, com.artemis.World engine) {
        super.loadFromEntity(entity, engine);

        SpineDataComponent spineDataComponent = ComponentRetriever.get(entity, SpineDataComponent.class, engine);
        animationName = spineDataComponent.animationName;
        currentAnimationName = spineDataComponent.currentAnimationName;
    }
}
