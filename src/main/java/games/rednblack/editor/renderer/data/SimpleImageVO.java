package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class SimpleImageVO extends MainItemVO {
	public String imageName = "";
    public boolean isRepeat = false;
    public boolean isPolygon = false;
	
	public SimpleImageVO() {
		super();
	}
	
	public SimpleImageVO(SimpleImageVO vo) {
		super(vo);
		imageName = vo.imageName;
		isRepeat = vo.isRepeat;
		isPolygon = vo.isPolygon;
	}

	@Override
	public void loadFromEntity(int entity) {
		super.loadFromEntity(entity);

		TextureRegionComponent textureRegionComponent = ComponentRetriever.get(entity, TextureRegionComponent.class);
		loadFromComponent(textureRegionComponent);
	}

	public void loadFromComponent(TextureRegionComponent textureRegionComponent) {
		imageName = textureRegionComponent.regionName;
		isRepeat = textureRegionComponent.isRepeat;
		isPolygon = textureRegionComponent.isPolygon;
	}
}
