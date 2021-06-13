package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
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
	}
	
	public void update(CompositeItemVO vo) {
		composite = new CompositeVO(vo.composite);
	}
	
	public CompositeItemVO clone() {
		/*CompositeItemVO tmp = new CompositeItemVO();
		tmp.composite = composite;
        tmp.itemName = itemName;
        tmp.layerName = layerName;
        tmp.rotation = rotation;
        tmp.tint = tint;
        tmp.x = x;
        tmp.y = y;
        tmp.zIndex = zIndex;

		tmp.width = width;
		tmp.height = height;*/
		Json json = new Json(JsonWriter.OutputType.json);

		return json.fromJson(CompositeItemVO.class, json.toJson(this));
	}

	@Override
	public void loadFromEntity(int entity) {
		super.loadFromEntity(entity);

		composite = new CompositeVO();
		composite.loadFromEntity(entity);

		DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class);
		CompositeTransformComponent compositeTransformComponent = ComponentRetriever.get(entity, CompositeTransformComponent.class);

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
