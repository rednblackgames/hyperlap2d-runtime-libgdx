package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.Json;
import games.rednblack.editor.renderer.utils.HyperJson;

import java.util.ArrayList;

public class SceneVO {

    public String sceneName = "";

    public CompositeItemVO composite;

    public PhysicsPropertiesVO physicsPropertiesVO = new PhysicsPropertiesVO();
    public LightsPropertiesVO lightsPropertiesVO = new LightsPropertiesVO();

    public ArrayList<Float> verticalGuides = new ArrayList<Float>();
    public ArrayList<Float> horizontalGuides = new ArrayList<Float>();

    public SceneVO() {

    }

	@Override
	public String toString () {
		return sceneName;
	}

	public SceneVO(SceneVO vo) {
        sceneName = new String(vo.sceneName);
        composite = new CompositeItemVO(vo.composite);
        physicsPropertiesVO = new PhysicsPropertiesVO(vo.physicsPropertiesVO);
        lightsPropertiesVO = vo.lightsPropertiesVO;
    }

    public String constructJsonString() {
        String str = "";
        Json json = HyperJson.getJson();
        str = json.toJson(this);
        return str;
    }
}
