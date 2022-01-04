package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Created by Osman on 20.08.2015.
 *
 */
public class CoreActorData {
    public String id = null;
    public String[] tags = null;
    public int layerIndex = 0;

    public ObjectMap<String, Object> customVariables = new ObjectMap<>();
}
