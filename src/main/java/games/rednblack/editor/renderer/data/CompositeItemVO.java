package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

import java.util.ArrayList;

public class CompositeItemVO extends MainItemVO {

	public CompositeVO composite;

	public float width;
	public float height;
	public boolean automaticResize = true;
	public boolean scissorsEnabled = false;
	public boolean renderToFBO = false;
	
	public CompositeItemVO() {
		composite = new CompositeVO();
	}
	
	public CompositeItemVO(CompositeVO vo) {
		composite = new CompositeVO(vo);
	}
	
	public CompositeItemVO(CompositeItemVO vo) {
		super(vo);
		composite = new CompositeVO(vo.composite);
		width = vo.width;
		height = vo.height;
		automaticResize = vo.automaticResize;
		scissorsEnabled = vo.scissorsEnabled;
		renderToFBO = vo.renderToFBO;
	}
	
	public void update(CompositeItemVO vo) {
		composite = new CompositeVO(vo.composite);
	}
	
	public CompositeItemVO clone() {
		return new CompositeItemVO(this);
	}

	@Override
	public void loadFromEntity(int entity, com.artemis.World engine) {
		super.loadFromEntity(entity, engine);

		composite = new CompositeVO();
		composite.loadFromEntity(entity, engine);

		DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class, engine);
		CompositeTransformComponent compositeTransformComponent = ComponentRetriever.get(entity, CompositeTransformComponent.class, engine);

		width = dimensionsComponent.width;
		height = dimensionsComponent.height;
		automaticResize = compositeTransformComponent.automaticResize;
		scissorsEnabled = compositeTransformComponent.scissorsEnabled;
		renderToFBO = compositeTransformComponent.renderToFBO;
	}

	public void cleanIds() {
		uniqueId = -1;
		ArrayList<MainItemVO> items = composite.getAllItems();
		for(MainItemVO subItem: items) {
			subItem.uniqueId = -1;
		}
	}
}
