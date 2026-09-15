package games.rednblack.editor.renderer.tenpatch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntArray;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.TenPatchVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

/**
 * Helpers to build a {@link TenPatchDrawable} for a 9-patch region of the project.
 *
 * The stretch areas come from {@link ProjectInfoVO#tenPatches} (original resolution pixels); when the
 * project has no entry for the region, a single stretch area per axis is derived from the atlas
 * {@code split} value, which is the classic libGDX 9-patch behaviour.
 */
public class TenPatchUtils {

    private TenPatchUtils() {
    }

    /**
     * Configuration of a region as stored in the project, or derived from the atlas {@code split} value.
     * The derived configuration is expressed in the pixels of the atlas the region belongs to.
     *
     * @return a new instance, never null
     */
    public static TenPatchVO getConfiguration(ProjectInfoVO project, TextureAtlas.AtlasRegion region) {
        TenPatchVO vo = project == null || project.tenPatches == null ? null : project.tenPatches.get(region.name);
        if (vo != null) {
            return new TenPatchVO(vo);
        }
        return fromSplits(region.findValue("split"), region.originalWidth, region.originalHeight);
    }

    /**
     * Converts libGDX 9-patch splits (left, right, top, bottom) into ten patch stretch areas.
     *
     * @param splits the atlas {@code split} value, null means the whole graphic stretches
     * @param width  width of the graphic in pixels
     * @param height height of the graphic in pixels
     */
    public static TenPatchVO fromSplits(int[] splits, int width, int height) {
        if (splits == null) {
            splits = new int[]{0, 0, 0, 0};
        }
        int hStart = splits[0];
        int hEnd = width - 1 - splits[1];
        int vStart = splits[3];
        int vEnd = height - 1 - splits[2];

        int[] horizontal = hEnd >= hStart ? new int[]{hStart, hEnd} : new int[0];
        int[] vertical = vEnd >= vStart ? new int[]{vStart, vEnd} : new int[0];
        return new TenPatchVO(horizontal, vertical);
    }

    /**
     * Converts ten patch stretch areas back to libGDX splits (left, right, top, bottom) using the
     * first horizontal and vertical area, the only information a {@code split} entry can carry.
     */
    public static int[] toSplits(TenPatchVO vo, int width, int height) {
        int[] splits = new int[]{0, 0, 0, 0};
        if (vo.horizontalStretchAreas != null && vo.horizontalStretchAreas.length >= 2) {
            splits[0] = vo.horizontalStretchAreas[0];
            splits[1] = width - 1 - vo.horizontalStretchAreas[vo.horizontalStretchAreas.length - 1];
        }
        if (vo.verticalStretchAreas != null && vo.verticalStretchAreas.length >= 2) {
            splits[3] = vo.verticalStretchAreas[0];
            splits[2] = height - 1 - vo.verticalStretchAreas[vo.verticalStretchAreas.length - 1];
        }
        return splits;
    }

    /**
     * Scales stretch areas by a ratio keeping them inside the graphic, sorted and non overlapping.
     * The boundaries of every area are scaled, so an area covering pixels 0..3 scaled by 2 covers 0..7.
     *
     * @param areas pairs of inclusive indexes
     * @param ratio scale factor
     * @param size  size in pixels of the graphic on the scaled axis
     * @return a new array of pairs, areas collapsed by the scaling are dropped
     */
    public static int[] scaleAreas(int[] areas, float ratio, int size) {
        if (areas == null) return new int[0];
        IntArray result = new IntArray(areas.length);
        int previousEnd = -1;
        for (int i = 0; i + 1 < areas.length; i += 2) {
            int start = Math.round(areas[i] * ratio);
            int end = Math.round((areas[i + 1] + 1) * ratio) - 1;
            start = Math.max(start, previousEnd + 1);
            end = Math.min(Math.max(end, start), size - 1);
            if (start > size - 1 || start > end) continue;
            result.add(start);
            result.add(end);
            previousEnd = end;
        }
        return result.toArray();
    }

    /**
     * Scales a configuration to another pixel space: stretch areas and tile offsets are multiplied by
     * {@code ratio}, everything else is copied.
     *
     * @param width  width of the graphic in the target pixel space
     * @param height height of the graphic in the target pixel space
     */
    public static TenPatchVO scale(TenPatchVO vo, float ratio, int width, int height) {
        TenPatchVO scaled = new TenPatchVO(vo);
        if (ratio == 1f) return scaled;
        scaled.horizontalStretchAreas = scaleAreas(vo.horizontalStretchAreas, ratio, width);
        scaled.verticalStretchAreas = scaleAreas(vo.verticalStretchAreas, ratio, height);
        scaled.offsetX = vo.offsetX * ratio;
        scaled.offsetY = vo.offsetY * ratio;
        scaled.offsetXspeed = vo.offsetXspeed * ratio;
        scaled.offsetYspeed = vo.offsetYspeed * ratio;
        return scaled;
    }

    /**
     * Builds a drawable from a configuration expressed in pixels of {@code region}. Nothing is scaled.
     */
    public static TenPatchDrawable createDrawable(TextureRegion region, TenPatchVO vo) {
        int[] horizontal = vo.horizontalStretchAreas == null ? new int[0] : vo.horizontalStretchAreas.clone();
        int[] vertical = vo.verticalStretchAreas == null ? new int[0] : vo.verticalStretchAreas.clone();
        TenPatchDrawable drawable = new TenPatchDrawable(horizontal, vertical, vo.tiling, region);
        drawable.setCrushMode(vo.crushMode);
        drawable.setOffset(vo.offsetX, vo.offsetY);
        drawable.setOffsetSpeed(vo.offsetXspeed, vo.offsetYspeed);
        if (vo.color1 != null) drawable.setColor1(toColor(vo.color1));
        if (vo.color2 != null) drawable.setColor2(toColor(vo.color2));
        if (vo.color3 != null) drawable.setColor3(toColor(vo.color3));
        if (vo.color4 != null) drawable.setColor4(toColor(vo.color4));
        return drawable;
    }

    private static Color toColor(float[] rgba) {
        if (rgba.length >= 4) return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
        if (rgba.length == 3) return new Color(rgba[0], rgba[1], rgba[2], 1f);
        return new Color(Color.WHITE);
    }

    /**
     * Builds the drawable of a region for the loaded resolution. Stretch areas and offsets stored in the
     * project are converted from original resolution pixels to the loaded resolution pixels; the drawable
     * itself is not scaled, use {@link TenPatchDrawable#scale(float, float)} to map it to world units
     * (remember to scale offsets and speeds by the same factor).
     */
    public static TenPatchDrawable createDrawable(IResourceRetriever rm, String regionName) {
        TextureRegion textureRegion = rm.getTextureRegion(regionName);
        if (!(textureRegion instanceof TextureAtlas.AtlasRegion)) {
            int w = textureRegion.getRegionWidth();
            int h = textureRegion.getRegionHeight();
            return new TenPatchDrawable(new int[]{0, w - 1}, new int[]{0, h - 1}, false, textureRegion);
        }
        TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion) textureRegion;
        ProjectInfoVO project = rm.getProjectVO();
        TenPatchVO vo = project == null || project.tenPatches == null ? null : project.tenPatches.get(regionName);

        if (vo != null) {
            float multiplier = rm.getLoadedResolution().getMultiplier(project.originalResolution);
            float ratio = multiplier == 0 ? 1f : 1f / multiplier;
            vo = scale(vo, ratio, region.originalWidth, region.originalHeight);
        } else {
            vo = fromSplits(region.findValue("split"), region.originalWidth, region.originalHeight);
        }
        return createDrawable(region, vo);
    }

    /**
     * Scales a drawable to world units: the graphic, its stretch areas, tile offsets and speeds.
     */
    public static void scaleDrawable(TenPatchDrawable drawable, float scaleX, float scaleY) {
        drawable.scale(scaleX, scaleY);
        drawable.offsetX *= scaleX;
        drawable.offsetY *= scaleY;
        drawable.offsetXspeed *= scaleX;
        drawable.offsetYspeed *= scaleY;
    }
}
