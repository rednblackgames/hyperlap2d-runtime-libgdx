package games.rednblack.editor.renderer.data;

import java.util.HashSet;

public class TexturePackVO {
    public String name;
    public HashSet<String> regions = new HashSet<>();

    /**
     * Packs marked editor-only exist purely to author scenes against — placement guides, oversized
     * reference art, layout helpers — and are never shipped. The editor still packs them so they are
     * visible while editing, but omits both the atlas and this entry when exporting the project, so
     * the runtime never learns they existed and pays nothing for them.
     * <p>
     * Scenes may still reference their regions. Those lookups resolve to null at runtime and the
     * affected items simply draw nothing.
     */
    public boolean editorOnly = false;
}
