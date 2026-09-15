package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.tenpatch.TenPatchDrawable;

import java.util.Arrays;

/**
 * Per image configuration of a 9-patch region rendered with {@link TenPatchDrawable}.
 * Stored in {@link ProjectInfoVO#tenPatches} keyed by region name.
 *
 * Stretch areas and offsets are expressed in pixels of the original resolution, without the 1px 9-patch
 * border. Areas are pairs of inclusive indexes exactly like {@link TenPatchDrawable} expects them.
 */
public class TenPatchVO {

    /**
     * Pairs of values defining stretch areas from the left of the graphic in ascending order.
     * All values are inclusive with 0 being the left-most pixel (i.e. (0,2) defines a 3 pixel wide stretch area).
     */
    public int[] horizontalStretchAreas = new int[0];

    /**
     * Pairs of values defining stretch areas from the bottom of the graphic in ascending order.
     * All values are inclusive with 0 being the bottom-most pixel (i.e. (0,2) defines a 3 pixel high stretch area).
     */
    public int[] verticalStretchAreas = new int[0];

    /** Tile the stretch areas instead of stretching them. */
    public boolean tiling = false;

    /** Offset of the tiles inside the stretch areas, only used when {@link #tiling} is on. */
    public float offsetX = 0f;
    public float offsetY = 0f;

    /** Speed in pixels per second at which the tiles scroll, only used when {@link #tiling} is on. */
    public float offsetXspeed = 0f;
    public float offsetYspeed = 0f;

    /** One of {@link TenPatchDrawable.CrushMode}. */
    public int crushMode = TenPatchDrawable.CrushMode.SHRINK;

    /**
     * Gradient colors of every patch as RGBA arrays, null when the patch is drawn untinted.
     * color1 is the lower left corner, color2 upper left, color3 upper right and color4 lower right.
     */
    public float[] color1 = null;
    public float[] color2 = null;
    public float[] color3 = null;
    public float[] color4 = null;

    public TenPatchVO() {
    }

    public TenPatchVO(TenPatchVO other) {
        set(other);
    }

    public TenPatchVO(int[] horizontalStretchAreas, int[] verticalStretchAreas) {
        this.horizontalStretchAreas = horizontalStretchAreas;
        this.verticalStretchAreas = verticalStretchAreas;
    }

    public void set(TenPatchVO other) {
        horizontalStretchAreas = other.horizontalStretchAreas == null ? new int[0] : Arrays.copyOf(other.horizontalStretchAreas, other.horizontalStretchAreas.length);
        verticalStretchAreas = other.verticalStretchAreas == null ? new int[0] : Arrays.copyOf(other.verticalStretchAreas, other.verticalStretchAreas.length);
        tiling = other.tiling;
        offsetX = other.offsetX;
        offsetY = other.offsetY;
        offsetXspeed = other.offsetXspeed;
        offsetYspeed = other.offsetYspeed;
        crushMode = other.crushMode;
        color1 = copy(other.color1);
        color2 = copy(other.color2);
        color3 = copy(other.color3);
        color4 = copy(other.color4);
    }

    /** True when at least one gradient color is defined. */
    public boolean hasGradient() {
        return color1 != null || color2 != null || color3 != null || color4 != null;
    }

    private static float[] copy(float[] color) {
        return color == null ? null : Arrays.copyOf(color, color.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenPatchVO)) return false;
        TenPatchVO that = (TenPatchVO) o;
        return tiling == that.tiling
                && crushMode == that.crushMode
                && offsetX == that.offsetX
                && offsetY == that.offsetY
                && offsetXspeed == that.offsetXspeed
                && offsetYspeed == that.offsetYspeed
                && Arrays.equals(horizontalStretchAreas, that.horizontalStretchAreas)
                && Arrays.equals(verticalStretchAreas, that.verticalStretchAreas)
                && Arrays.equals(color1, that.color1)
                && Arrays.equals(color2, that.color2)
                && Arrays.equals(color3, that.color3)
                && Arrays.equals(color4, that.color4);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(horizontalStretchAreas);
        result = 31 * result + Arrays.hashCode(verticalStretchAreas);
        result = 31 * result + (tiling ? 1 : 0);
        result = 31 * result + crushMode;
        result = 31 * result + Float.floatToIntBits(offsetX);
        result = 31 * result + Float.floatToIntBits(offsetY);
        result = 31 * result + Float.floatToIntBits(offsetXspeed);
        result = 31 * result + Float.floatToIntBits(offsetYspeed);
        result = 31 * result + Arrays.hashCode(color1);
        result = 31 * result + Arrays.hashCode(color2);
        result = 31 * result + Arrays.hashCode(color3);
        result = 31 * result + Arrays.hashCode(color4);
        return result;
    }
}
