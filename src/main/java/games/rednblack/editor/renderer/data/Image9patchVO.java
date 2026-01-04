package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.NinePatchComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class Image9patchVO extends MainItemVO {

    public String imageName = "";
    public float width = 0;
    public float height = 0;

    public Image9patchVO() {
        super();
    }

    public Image9patchVO(Image9patchVO vo) {
        super(vo);
        imageName = new String(vo.imageName);
        width = vo.width;
        height = vo.height;
    }

    @Override
    public void loadFromEntity(int entity, Engine engine, EntityFactory entityFactory) {
        super.loadFromEntity(entity, engine, entityFactory);

        NinePatchComponent ninePatchComponent = ComponentRetriever.get(entity, NinePatchComponent.class, engine);
        DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class, engine);
        imageName = ninePatchComponent.textureRegionName;

        width = dimensionsComponent.width;
        height = dimensionsComponent.height;
    }

    @Override
    public String getResourceName() {
        return imageName;
    }
}
