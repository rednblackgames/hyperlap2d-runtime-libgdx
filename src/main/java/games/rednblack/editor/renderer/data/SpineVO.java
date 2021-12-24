package games.rednblack.editor.renderer.data;

import com.artemis.World;
import games.rednblack.editor.renderer.components.SpineDataComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
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
    public void loadFromEntity(int entity, World engine, EntityFactory entityFactory) {
        super.loadFromEntity(entity, engine, entityFactory);

        SpineDataComponent spineDataComponent = ComponentRetriever.get(entity, SpineDataComponent.class, engine);
        animationName = spineDataComponent.animationName;
        currentAnimationName = spineDataComponent.currentAnimationName;
    }

    @Override
    public String getResourceName() {
        return animationName;
    }
}
