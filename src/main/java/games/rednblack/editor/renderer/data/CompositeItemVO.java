package games.rednblack.editor.renderer.data;

import com.artemis.BaseComponentMapper;
import com.artemis.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import games.rednblack.editor.renderer.components.*;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.FontSizePair;
import games.rednblack.editor.renderer.utils.ComponentRetriever;
import games.rednblack.editor.renderer.utils.HyperJson;

public class CompositeItemVO extends MainItemVO {
	public float width;
	public float height;
	public boolean automaticResize = true;
	public boolean scissorsEnabled = false;
	public boolean renderToFBO = false;

	public ObjectMap<String, Array<MainItemVO>> content = new ObjectMap<>(0);

	public ObjectMap<String, StickyNoteVO> sStickyNotes = new ObjectMap<>(1);
	public Array<LayerItemVO> layers = new Array<>();
	
	public CompositeItemVO() {
	}
	
	public CompositeItemVO(CompositeItemVO vo) {
		super(vo);
		if (vo == null) return;
		width = vo.width;
		height = vo.height;
		automaticResize = vo.automaticResize;
		scissorsEnabled = vo.scissorsEnabled;
		renderToFBO = vo.renderToFBO;

		content.clear();
		try {
			for (String type : vo.content.keys()) {
				Array<MainItemVO> typeArray = new Array<>(ClassReflection.forName(type));
				typeArray.addAll(vo.content.get(type));
				content.put(type, typeArray);
			}
		} catch (ReflectionException e) {
			e.printStackTrace();
		}

		layers.clear();
		for (int i = 0; i < vo.layers.size; i++) {
			layers.add(new LayerItemVO(vo.layers.get(i)));
		}

		sStickyNotes.clear();
		for (String key : vo.sStickyNotes.keys()) {
			StickyNoteVO note = new StickyNoteVO(vo.sStickyNotes.get(key));
			sStickyNotes.put(note.id, note);
		}
	}

	public void addItem(MainItemVO vo) {
		String clazz = vo.getClass().getName();
		Array<MainItemVO> array = content.get(clazz);
		if (array == null) {
			try {
				array = new Array<>(ClassReflection.forName(clazz));
			} catch (ReflectionException e) {
				throw new RuntimeException(e);
			}
			content.put(clazz, array);
		}
		array.add(vo);
	}

	public void removeItem(MainItemVO vo) {
		String clazz = vo.getClass().getName();
		Array<MainItemVO> array = content.get(clazz);
		array.removeValue(vo, true);
	}
	
	public CompositeItemVO clone() {
		Json json = HyperJson.getJson();
		return json.fromJson(CompositeItemVO.class, json.toJson(this));
	}

	@Override
	public void loadFromEntity(int entity, World engine, EntityFactory entityFactory) {
		super.loadFromEntity(entity, engine, entityFactory);

		NodeComponent nodeComponent = ComponentRetriever.get(entity, NodeComponent.class, engine);
		if (nodeComponent == null) return;

		BaseComponentMapper<LayerMapComponent> layerMainItemComponentComponentMapper = ComponentRetriever.getMapper(LayerMapComponent.class, engine);
		LayerMapComponent layerMapComponent = layerMainItemComponentComponentMapper.get(entity);
		layers.clear();
		for (int i = 0; i < layerMapComponent.getLayers().size; i++) {
			layers.add(new LayerItemVO(layerMapComponent.getLayers().get(i)));
		}

		BaseComponentMapper<MainItemComponent> mainItemComponentMapper = ComponentRetriever.getMapper(MainItemComponent.class, engine);
		for (int child : nodeComponent.children) {
			int entityType = mainItemComponentMapper.get(child).entityType;
			try {
				MainItemVO entityVO = entityFactory.instantiateEmptyVO(entityType);
				entityVO.loadFromEntity(child, engine, entityFactory);
				addItem(entityVO);
			} catch (ReflectionException e) {
				e.printStackTrace();
			}
		}

		DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity, DimensionsComponent.class, engine);
		CompositeTransformComponent compositeTransformComponent = ComponentRetriever.get(entity, CompositeTransformComponent.class, engine);

		width = dimensionsComponent.width;
		height = dimensionsComponent.height;
		automaticResize = compositeTransformComponent.automaticResize;
		scissorsEnabled = compositeTransformComponent.scissorsEnabled;
		renderToFBO = compositeTransformComponent.renderToFBO;
	}

	@Override
	public String getResourceName() {
		throw new RuntimeException("Composite doesn't have resources to load.");
	}

	public void cleanIds() {
		uniqueId = null;
		for (MainItemVO subItem : getAllItems()) {
			subItem.uniqueId = null;
		}
	}

	private Array<MainItemVO> getAllItemsRecursive(Array<MainItemVO> itemsArray, CompositeItemVO compositeItemVO) {
		for (String type : compositeItemVO.content.keys()) {
			Array<MainItemVO> array = compositeItemVO.content.get(type);
			if (!type.equals(CompositeItemVO.class.getName())) {
				itemsArray.addAll(array);
			} else {
				for (MainItemVO c : array) {
					CompositeItemVO composite = (CompositeItemVO) c;
					getAllItemsRecursive(itemsArray, composite);
					itemsArray.add(composite);
				}
			}
		}
		return itemsArray;
	}

	public Array<FontSizePair> getRecursiveFontList() {
		ObjectSet<FontSizePair> list = new ObjectSet<>();
		Array<MainItemVO> sLabels = content.get(LabelVO.class.getName());
		if (sLabels != null) {
			for (MainItemVO elem : sLabels) {
				LabelVO sLabel = (LabelVO) elem;
				if (sLabel.bitmapFont != null) continue;
				list.add(new FontSizePair(sLabel.style.isEmpty() ? "lsans" : sLabel.style, sLabel.size == 0 ? 12 : sLabel.size, sLabel.monoSpace));
			}
		}

		Array<MainItemVO> sComposites = content.get(CompositeItemVO.class.getName());
		if (sComposites != null) {
			for (MainItemVO elem : sComposites) {
				CompositeItemVO sComposite = (CompositeItemVO) elem;
				list.addAll(sComposite.getRecursiveFontList());
			}
		}

		Array<FontSizePair> finalList = new Array<>(list.size);
		for (FontSizePair elem : list) {
			finalList.add(elem);
		}
		return finalList;
	}

	public Array<String> getRecursiveTypeNamesList(Class<? extends MainItemVO> type) {
		if (type == CompositeItemVO.class) throw new IllegalArgumentException("Can be retrieved only resource");

		ObjectSet<String> list = new ObjectSet<>();
		Array<MainItemVO> sElements = content.get(type.getName());
		if (sElements != null) {
			for (MainItemVO elem : sElements) {
				String resName = elem.getResourceName();
				if (resName != null)
					list.add(resName);
			}
		}

		Array<MainItemVO> sComposites = content.get(CompositeItemVO.class.getName());
		if (sComposites != null) {
			for (MainItemVO elem : sComposites) {
				CompositeItemVO sComposite = (CompositeItemVO) elem;
				list.addAll(sComposite.getRecursiveTypeNamesList(type));
			}
		}

		Array<String> finalList = new Array<>(list.size);
		for (String elem : list) {
			finalList.add(elem);
		}
		return finalList;
	}

	public Array<String> getRecursiveShaderList() {
		ObjectSet<String> list = new ObjectSet<>();
		for (MainItemVO item : getAllItems()) {
			if (item.shader.shaderName != null && !item.shader.shaderName.isEmpty()) {
				list.add(item.shader.shaderName);
			}
		}
		Array<String> finalList = new Array<>(list.size);
		for (String shader : list) {
			finalList.add(shader);
		}
		return finalList;
	}

	public Array<MainItemVO> getAllItems() {
		return getAllItemsRecursive(new Array<MainItemVO>(), this);
	}

	public <T extends MainItemVO> Array<T> getElementsArray(Class<T> type) {
		Array<T> array = (Array<T>) content.get(type.getName());
		return array != null ? array : new Array<T>();
	}
}
