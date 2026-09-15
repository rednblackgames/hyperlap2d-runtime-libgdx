package games.rednblack.editor.renderer.tenpatch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import games.rednblack.editor.renderer.data.FrameRange;
import games.rednblack.editor.renderer.data.Image9patchVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.TenPatchVO;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

import java.util.Comparator;

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
     * Frames of a sprite animation sorted by index, or null when {@code name} is not a sprite animation.
     * A new array is returned, the resource manager's one is left untouched.
     */
    public static Array<TextureAtlas.AtlasRegion> getAnimationFrames(IResourceRetriever rm, String name) {
        Array<TextureAtlas.AtlasRegion> regions = rm.getSpriteAnimation(name);
        if (regions == null || regions.size == 0) return null;
        Array<TextureAtlas.AtlasRegion> frames = new Array<>(regions);
        frames.sort(FRAME_ORDER);
        return frames;
    }

    private static final Comparator<TextureAtlas.AtlasRegion> FRAME_ORDER = new Comparator<TextureAtlas.AtlasRegion>() {
        @Override
        public int compare(TextureAtlas.AtlasRegion a, TextureAtlas.AtlasRegion b) {
            return Integer.compare(a.index, b.index);
        }
    };

    /** True when {@code name} is a sprite animation rather than a single region. */
    public static boolean isAnimation(IResourceRetriever rm, String name) {
        Array<TextureAtlas.AtlasRegion> regions = rm.getSpriteAnimation(name);
        return regions != null && regions.size > 0;
    }

    /**
     * The atlas region a 9-patch name stands for: the first frame of a sprite animation, or the image
     * region itself. Null when the name is unknown or the region is not an atlas region.
     */
    public static TextureAtlas.AtlasRegion resolveRegion(IResourceRetriever rm, String name) {
        Array<TextureAtlas.AtlasRegion> frames = getAnimationFrames(rm, name);
        if (frames != null) return frames.first();
        TextureRegion region = rm.getTextureRegion(name);
        return region instanceof TextureAtlas.AtlasRegion ? (TextureAtlas.AtlasRegion) region : null;
    }

    /**
     * Makes a drawable play the frames of a range, the way {@link TenPatchDrawable} animates on its own:
     * it is what the scene2d actors and the editor preview use. ECS entities are animated by
     * {@code SpriteAnimationSystem} instead, which updates the region drawn every frame.
     *
     * @param range    frames to play, null for all of them
     * @param playMode as {@link Image9patchVO#playMode}
     */
    public static void setAnimation(TenPatchDrawable drawable, Array<TextureAtlas.AtlasRegion> frames, FrameRange range, int fps, int playMode) {
        int start = range == null ? 0 : Math.max(0, Math.min(range.startFrame, frames.size - 1));
        int end = range == null ? frames.size - 1 : Math.max(start, Math.min(range.endFrame, frames.size - 1));
        Array<TextureRegion> regions = new Array<>(end - start + 1);
        for (int i = start; i <= end; i++) {
            regions.add(frames.get(i));
        }
        drawable.setRegions(regions);
        drawable.setFrameDuration(fps > 0 ? 1f / fps : 1f);
        drawable.setPlayMode(playMode);
        drawable.setTime(0f);
    }

    /** {@link Animation.PlayMode} as the integer stored in VOs (see {@link Image9patchVO#playMode}). */
    public static int playModeToInt(Animation.PlayMode playMode) {
        if (playMode == null) return TenPatchDrawable.PlayMode.LOOP;
        switch (playMode) {
            case NORMAL: return TenPatchDrawable.PlayMode.NORMAL;
            case REVERSED: return TenPatchDrawable.PlayMode.REVERSED;
            case LOOP_REVERSED: return TenPatchDrawable.PlayMode.LOOP_REVERSED;
            case LOOP_PINGPONG: return TenPatchDrawable.PlayMode.LOOP_PINGPONG;
            case LOOP_RANDOM: return TenPatchDrawable.PlayMode.LOOP_RANDOM;
            case LOOP:
            default: return TenPatchDrawable.PlayMode.LOOP;
        }
    }

    /** The integer stored in VOs as {@link Animation.PlayMode}, unknown values play in a loop. */
    public static Animation.PlayMode playModeFromInt(int playMode) {
        switch (playMode) {
            case TenPatchDrawable.PlayMode.NORMAL: return Animation.PlayMode.NORMAL;
            case TenPatchDrawable.PlayMode.REVERSED: return Animation.PlayMode.REVERSED;
            case TenPatchDrawable.PlayMode.LOOP_REVERSED: return Animation.PlayMode.LOOP_REVERSED;
            case TenPatchDrawable.PlayMode.LOOP_PINGPONG: return Animation.PlayMode.LOOP_PINGPONG;
            case TenPatchDrawable.PlayMode.LOOP_RANDOM: return Animation.PlayMode.LOOP_RANDOM;
            default: return Animation.PlayMode.LOOP;
        }
    }

    /**
     * Builds the drawable of a region for the loaded resolution. Stretch areas and offsets stored in the
     * project are converted from original resolution pixels to the loaded resolution pixels; the drawable
     * itself is not scaled, use {@link #scaleDrawable(TenPatchDrawable, float, float)} to map it to world
     * units. For a sprite animation the first frame is used, see {@link #setAnimation} to play the others.
     */
    public static TenPatchDrawable createDrawable(IResourceRetriever rm, String regionName) {
        TextureAtlas.AtlasRegion region = resolveRegion(rm, regionName);
        if (region == null) {
            TextureRegion textureRegion = rm.getTextureRegion(regionName);
            int w = textureRegion.getRegionWidth();
            int h = textureRegion.getRegionHeight();
            return new TenPatchDrawable(new int[]{0, w - 1}, new int[]{0, h - 1}, false, textureRegion);
        }
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
