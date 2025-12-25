package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.ObjectMap;

public class GraphNodeVO {
    public String id = "";
    public String type = "";
    public float x, y;
    public ObjectMap<String, String> data = new ObjectMap<>();
}
