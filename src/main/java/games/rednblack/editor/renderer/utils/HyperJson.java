package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import games.rednblack.editor.renderer.data.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class HyperJson {
    /**
     * Every class tag handed to any parser, so one created later still knows the types registered
     * before it — extensions add theirs (TalosVO, SpineVO, TinyVGVO, ...) while they boot.
     */
    private static final LinkedHashMap<String, Class> classTags = new LinkedHashMap<>();

    static {
        classTags.put(CompositeItemVO.class.getSimpleName(), CompositeItemVO.class);
        classTags.put(LightVO.class.getSimpleName(), LightVO.class);
        classTags.put(ParticleEffectVO.class.getSimpleName(), ParticleEffectVO.class);
        classTags.put(SimpleImageVO.class.getSimpleName(), SimpleImageVO.class);
        classTags.put(SpriteAnimationVO.class.getSimpleName(), SpriteAnimationVO.class);
        classTags.put(LabelVO.class.getSimpleName(), LabelVO.class);
        classTags.put(Image9patchVO.class.getSimpleName(), Image9patchVO.class);
        classTags.put(ColorPrimitiveVO.class.getSimpleName(), ColorPrimitiveVO.class);
    }

    private static Json json = null;

    public static Json getJson() {
        if (json == null){
            json = new TrackingJson();
            applyClassTags(json);
        }
        return json;
    }

    /**
     * A configured parser of its own, for callers that read off the main thread: {@link Json} caches
     * type information as it reads, so the shared instance cannot be used concurrently. It starts
     * with every class tag registered so far, the shared instance's included.
     */
    public static Json newJson() {
        Json instance = new Json();
        applyClassTags(instance);
        return instance;
    }

    private static void applyClassTags(Json instance) {
        instance.setIgnoreUnknownFields(true);
        instance.setOutputType(JsonWriter.OutputType.json);

        // Over a copy: the shared instance records every tag it is given, this loop included.
        LinkedHashMap<String, Class> snapshot;
        synchronized (classTags) {
            snapshot = new LinkedHashMap<>(classTags);
        }
        for (Map.Entry<String, Class> tag : snapshot.entrySet())
            instance.addClassTag(tag.getKey(), tag.getValue());
    }

    /**
     * The shared parser, which remembers the tags added to it. Extensions register through
     * {@code HyperJson.getJson().addClassTag(...)}, and that has to reach parsers created later too.
     */
    private static class TrackingJson extends Json {
        @Override
        public void addClassTag(String tag, Class type) {
            super.addClassTag(tag, type);
            synchronized (classTags) {
                classTags.put(tag, type);
            }
        }
    }

    private HyperJson() {

    }
}
