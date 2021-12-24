package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

public class HyperJson {
    private static Json json = null;

    public static Json getJson() {
        if (json == null){
            json = new Json();
            json.setIgnoreUnknownFields(true);
            json.setOutputType(JsonWriter.OutputType.json);
        }
        return json;
    }

    private HyperJson() {

    }
}
