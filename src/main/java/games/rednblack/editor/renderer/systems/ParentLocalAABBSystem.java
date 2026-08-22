package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.math.MathUtils;

import games.rednblack.editor.renderer.components.BoundingBoxComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;

/**
 * Fills {@link BoundingBoxComponent#parentLocalAABB} before {@link LayoutSystem} resolves its
 * constraints against it. {@link BoundingBoxSystem} runs after the layout, so constrained
 * entities used to resolve against a zero box on their first frame.
 * <p>
 * Split off because the box is an offset from (x, y): it depends on rotation, scale, origin and
 * dimensions, not on the position the layout picks. Scene-space corners stay in
 * {@link BoundingBoxSystem}, which does need that position.
 */
@All(BoundingBoxComponent.class)
public class ParentLocalAABBSystem extends IteratingSystem {

    protected ComponentMapper<BoundingBoxComponent> boundingBoxMapper;
    protected ComponentMapper<DimensionsComponent> dimensionsMapper;
    protected ComponentMapper<TransformComponent> transformMapper;

    @Override
    protected void process(int entity) {
        TransformComponent t = transformMapper.get(entity);
        DimensionsComponent d = dimensionsMapper.get(entity);
        if (t == null || d == null) return;

        BoundingBoxComponent b = boundingBoxMapper.get(entity);

        int checksum = calcCheckSum(t, d);
        if (checksum == b.aabbChecksum) return;

        computeParentLocalAABB(t, d.width, d.height, b);
        b.aabbChecksum = checksum;
    }

    /** AABB in parent space, as offsets from (t.x, t.y). Uses plain dimensions, not the
     * polygon-adjusted ones, so constraints reference the entity's logical bounds. */
    private void computeParentLocalAABB(TransformComponent t, float width, float height, BoundingBoxComponent b) {
        float rotation = -t.rotation;
        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);
        float originX = Float.isNaN(t.originX) ? 0 : t.originX;
        float originY = Float.isNaN(t.originY) ? 0 : t.originY;

        if (rotation == 0 && scaleX == 1 && scaleY == 1) {
            b.parentLocalAABB.set(0, 0, width, height);
            return;
        }

        float cos = MathUtils.cosDeg(rotation);
        float sin = MathUtils.sinDeg(rotation);

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            float lx = (i == 1 || i == 2) ? width : 0;
            float ly = (i == 2 || i == 3) ? height : 0;
            float tox = (lx - originX) * scaleX;
            float toy = (ly - originY) * scaleY;
            float px = tox * cos + toy * sin + originX;
            float py = tox * -sin + toy * cos + originY;
            minX = Math.min(minX, px); minY = Math.min(minY, py);
            maxX = Math.max(maxX, px); maxY = Math.max(maxY, py);
        }

        b.parentLocalAABB.set(minX, minY, maxX - minX, maxY - minY);
    }

    /** Excludes x and y: the box is an offset from them, so moving must not force a recompute. */
    private int calcCheckSum(TransformComponent t, DimensionsComponent d) {
        float scaleX = t.scaleX * (t.flipX ? -1 : 1);
        float scaleY = t.scaleY * (t.flipY ? -1 : 1);

        return Float.floatToRawIntBits(t.rotation) * 3
                + Float.floatToRawIntBits(scaleX) * 5
                + Float.floatToRawIntBits(scaleY) * 7
                + Float.floatToRawIntBits(t.originX) * 17
                + Float.floatToRawIntBits(t.originY) * 19
                + Float.floatToRawIntBits(d.width) * 23
                + Float.floatToRawIntBits(d.height) * 29;
    }
}
