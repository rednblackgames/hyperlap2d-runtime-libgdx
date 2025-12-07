package games.rednblack.editor.renderer.lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.PoolManager;

/**
 * A light whose ray starting points are evenly distributed along a chain of vertices
 */
public class ChainLight extends Light {
    static private final PoolManager POOLS = new PoolManager(Vector2::new, FloatArray::new, Spinor::new);

    public static float defaultRayStartOffset = 0.001f;
    public float rayStartOffset;
    public final FloatArray chain;

    protected int rayDirection;
    protected float bodyAngle;
    protected float bodyAngleOffset;

    protected Body body;

    protected final FloatArray segmentAngles = new FloatArray();
    protected final FloatArray segmentLengths = new FloatArray();

    protected final float[] startX;
    protected final float[] startY;
    protected final float[] endX;
    protected final float[] endY;

    protected final Vector2 bodyPosition = new Vector2();
    protected final Vector2 tmpVec = new Vector2();

    protected final Matrix3 zeroPosition = new Matrix3();
    protected final Matrix3 rotateAroundZero = new Matrix3();
    protected final Matrix3 restorePosition = new Matrix3();

    protected final Rectangle chainLightBounds = new Rectangle();
    protected final Rectangle rayHandlerBounds = new Rectangle();

    Color tmpColor = new Color();

    public ChainLight(RayHandler rayHandler, int rays, Color color, float distance, int rayDirection, float[] chain) {
        super(rayHandler, rays, color, distance, 0f);
        rayStartOffset = ChainLight.defaultRayStartOffset;
        this.rayDirection = rayDirection;
        // Vertex num is different here, we handle segments manually
        endX = new float[rays];
        endY = new float[rays];
        startX = new float[rays];
        startY = new float[rays];
        this.chain = (chain != null) ? new FloatArray(chain) : new FloatArray();

        setMesh(); // Initialize data
    }

    public ChainLight(RayHandler rayHandler, int rays, Color color, float distance, int rayDirection) {
        this(rayHandler, rays, color, distance, rayDirection, null);
    }

    @Override
    public void update() {
        if (dirty) {
            updateChain();
            applyAttachment();
        } else {
            updateBody();
        }

        updateBoundingRects();

        if (rayHandler.pseudo3d) {
            prepareFixtureData();
        }

        if (cull()) return;
        if (staticLight && !dirty) return;
        dirty = false;

        updateMesh();
    }

    protected void prepareFixtureData() {
        rayHandler.world.QueryAABB(
                dynamicShadowCallback,
                chainLightBounds.x - distance, chainLightBounds.y - distance,
                chainLightBounds.x + chainLightBounds.width + distance, chainLightBounds.y + chainLightBounds.height + distance);
    }

    @Override
    void draw(LightBatch batch) {
        if (rayHandler.culling && culled) return;
        rayHandler.lightRenderedLastFrame++;

        for (int i = 0; i < rayNum - 1; i++) {
            batch.checkSpace(6);

            float sx1 = startX[i];
            float sy1 = startY[i];
            float ex1 = mx[i];
            float ey1 = my[i];
            float f1 = f[i]; // fraction

            float sx2 = startX[i+1];
            float sy2 = startY[i+1];
            float ex2 = mx[i+1];
            float ey2 = my[i+1];
            float f2 = f[i+1];

            float z = pseudo3dHeight != 0 ? pseudo3dHeight : 50f;

            batch.drawVertex(sx1, sy1, colorF, 0f, intensity, falloff.x, falloff.y, falloff.z, sx1, sy1, z);
            batch.drawVertex(ex1, ey1, colorF, f1, intensity, falloff.x, falloff.y, falloff.z, sx1, sy1, z);
            batch.drawVertex(sx2, sy2, colorF, 0f, intensity, falloff.x, falloff.y, falloff.z, sx2, sy2, z);

            batch.drawVertex(sx2, sy2, colorF, 0f, intensity, falloff.x, falloff.y, falloff.z, sx2, sy2, z);
            batch.drawVertex(ex1, ey1, colorF, f1, intensity, falloff.x, falloff.y, falloff.z, sx1, sy1, z);
            batch.drawVertex(ex2, ey2, colorF, f2, intensity, falloff.x, falloff.y, falloff.z, sx2, sy2, z);
        }

        // Soft Shadows
        if (soft && !xray && !rayHandler.pseudo3d) {
            for (int i = 0; i < rayNum - 1; i++) {
                batch.checkSpace(6);

                float ex1 = mx[i];
                float ey1 = my[i];
                float s1 = f[i];

                float ex2 = mx[i+1];
                float ey2 = my[i+1];
                float s2 = f[i+1];

                float dirX1 = ex1 - startX[i];
                float dirY1 = ey1 - startY[i];
                float len1 = (float)Math.sqrt(dirX1*dirX1 + dirY1*dirY1);
                if (len1 > 0.001f) { dirX1 /= len1; dirY1 /= len1; }

                float dirX2 = ex2 - startX[i+1];
                float dirY2 = ey2 - startY[i+1];
                float len2 = (float)Math.sqrt(dirX2*dirX2 + dirY2*dirY2);
                if (len2 > 0.001f) { dirX2 /= len2; dirY2 /= len2; }

                float scale1 = 1f - s1;
                float scale2 = 1f - s2;

                float softX1 = ex1 + dirX1 * softShadowLength * scale1;
                float softY1 = ey1 + dirY1 * softShadowLength * scale1;
                float softX2 = ex2 + dirX2 * softShadowLength * scale2;
                float softY2 = ey2 + dirY2 * softShadowLength * scale2;

                float z = pseudo3dHeight != 0 ? pseudo3dHeight : 50f;
                float lx = startX[i]; float ly = startY[i]; // Approx light pos

                // Quad Soft
                batch.drawVertex(ex1, ey1, colorF, s1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, z);
                batch.drawVertex(softX1, softY1, zeroColorBits, 1f, intensity, falloff.x, falloff.y, falloff.z, lx, ly, z);
                batch.drawVertex(ex2, ey2, colorF, s2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, z);

                batch.drawVertex(ex2, ey2, colorF, s2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, z);
                batch.drawVertex(softX1, softY1, zeroColorBits, 1f, intensity, falloff.x, falloff.y, falloff.z, lx, ly, z);
                batch.drawVertex(softX2, softY2, zeroColorBits, 1f, intensity, falloff.x, falloff.y, falloff.z, lx, ly, z);
            }
        }
    }

    @Override
    void drawDynamicShadows(LightBatch batch) {
        float colBits = rayHandler.ambientLight.toFloatBits();
        float z = pseudo3dHeight != 0 ? pseudo3dHeight : 50f;

        Vector2 specificLightPos = POOLS.obtain(Vector2.class);

        for (Fixture fixture : affectedFixtures) {
            Object userData = fixture.getUserData();
            if (!(userData instanceof LightData) || fixture.isSensor()) continue;

            LightData data = (LightData) userData;
            // if (data.shadowsDropped >= rayHandler.shadowsDroppedLimit) continue;

            Shape fixtureShape = fixture.getShape();
            Shape.Type type = fixtureShape.getType();
            Body body = fixture.getBody();
            center.set(body.getWorldCenter());

            getClosestPointOnChain(center.x, center.y, specificLightPos);
            float lx = specificLightPos.x;
            float ly = specificLightPos.y;

            if (type == Shape.Type.Polygon || type == Shape.Type.Chain) {
                drawShadowPolygon(batch, fixture, data, colBits, fixtureShape, type, body, lx, ly, z);
            } else if (type == Shape.Type.Circle) {
                drawShadowCircle(batch, fixture, data, colBits, fixtureShape, body, lx, ly, z);
            } else if (type == Shape.Type.Edge) {
                drawShadowEdge(batch, fixture, data, colBits, fixtureShape, body, lx, ly, z);
            }
        }
        POOLS.free(specificLightPos);
    }

    private void drawShadowPolygon(LightBatch batch, Fixture fixture, LightData data, float colBits, Shape fixtureShape, Shape.Type type, Body body, float lx, float ly, float lz) {
        boolean isPolygon = (type == Shape.Type.Polygon);
        ChainShape cShape = isPolygon ? null : (ChainShape) fixtureShape;
        PolygonShape pShape = isPolygon ? (PolygonShape) fixtureShape : null;
        int vertexCount = isPolygon ? pShape.getVertexCount() : cShape.getVertexCount();

        int minN = -1; int maxN = -1; int minDstN = -1;
        float minDst = Float.POSITIVE_INFINITY;
        boolean hasGasp = false;

        tmpVerts.clear();
        for (int n = 0; n < vertexCount; n++) {
            if (isPolygon) pShape.getVertex(n, tmpVec); else cShape.getVertex(n, tmpVec);
            tmpVec.set(body.getWorldPoint(tmpVec));
            tmpVerts.add(tmpVec.cpy());

            tmpEnd.set(tmpVec).sub(lx, ly).limit2(0.0001f).add(tmpVec);

            if (fixture.testPoint(tmpEnd)) {
                if (minN == -1) minN = n;
                maxN = n;
                hasGasp = true;
                continue;
            }

            // Usa lx, ly per la distanza
            float currDist = Vector2.dst2(tmpVec.x, tmpVec.y, lx, ly);
            if (currDist < minDst) {
                minDst = currDist;
                minDstN = n;
            }
        }

        ind.clear();
        if (!hasGasp) {
            tmpVec.set(tmpVerts.get(minDstN));
            for (int n = minDstN; n < vertexCount; n++) ind.add(n);
            for (int n = 0; n < minDstN; n++) ind.add(n);
            // Intersector check using lx, ly as starting point
            if (Intersector.pointLineSide(lx, ly, center.x, center.y, tmpVec.x, tmpVec.y) > 0) {
                ind.reverse();
                ind.insert(0, ind.pop());
            }
        } else if (minN == 0 && maxN == vertexCount - 1) {
            for (int n = maxN - 1; n > minN; n--) ind.add(n);
        } else {
            for (int n = minN - 1; n > -1; n--) ind.add(n);
            for (int n = vertexCount - 1; n > maxN; n--) ind.add(n);
        }

        boolean contained = false;
        for (int n : ind.toArray()) {
            tmpVec.set(tmpVerts.get(n));
            if (contains(tmpVec.x, tmpVec.y)) {
                contained = true;
                break;
            }
        }
        if (!contained) return;

        float prevX = 0, prevY = 0, prevEndX = 0, prevEndY = 0;
        float prevStartCol = 0, prevEndCol = 0, prevF1 = 0, prevF2 = 0;
        boolean first = true;

        for (int n : ind.toArray()) {
            tmpVec.set(tmpVerts.get(n));

            float dst = Vector2.dst(tmpVec.x, tmpVec.y, lx, ly);
            float l = data.getLimit(dst, pseudo3dHeight, distance);

            tmpEnd.set(tmpVec).sub(lx, ly).setLength(l).add(tmpVec);

            float f1 = 1f - dst / distance;
            float f2 = 1f - (dst + l) / distance;

            tmpColor.set(Color.BLACK);
            float startColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1).toFloatBits() : oneColorBits;
            tmpColor.set(Color.WHITE);
            float endColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2).toFloatBits() : colBits;

            if (!first) {
                batch.checkSpace(6);

                batch.drawVertex(prevX, prevY, prevStartCol, prevF1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(prevEndX, prevEndY, prevEndCol, prevF2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(tmpVec.x, tmpVec.y, startColBits, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);

                batch.drawVertex(tmpVec.x, tmpVec.y, startColBits, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(prevEndX, prevEndY, prevEndCol, prevF2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(tmpEnd.x, tmpEnd.y, endColBits, f2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            }
            prevX = tmpVec.x; prevY = tmpVec.y;
            prevEndX = tmpEnd.x; prevEndY = tmpEnd.y;
            prevStartCol = startColBits; prevEndCol = endColBits;
            prevF1 = f1; prevF2 = f2;
            first = false;
        }

        // Roof logic
        if (data.roofShadow || pseudo3dHeight <= data.height) {
            int vCount = tmpVerts.size;
            if (vCount >= 3) {
                Vector2 v0 = tmpVerts.get(0);
                float d0 = Vector2.dst(v0.x, v0.y, lx, ly);
                float f0 = 1f - d0 / distance;
                float c0 = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f0).toFloatBits() : oneColorBits;

                for (int i = 1; i < vCount - 1; i++) {
                    batch.checkSpace(3);

                    Vector2 v1 = tmpVerts.get(i);
                    float d1 = Vector2.dst(v1.x, v1.y, lx, ly);
                    float f1_roof = 1f - d1 / distance;
                    float c1 = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1_roof).toFloatBits() : oneColorBits;

                    Vector2 v2 = tmpVerts.get(i + 1);
                    float d2 = Vector2.dst(v2.x, v2.y, lx, ly);
                    float f2_roof = 1f - d2 / distance;
                    float c2 = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2_roof).toFloatBits() : oneColorBits;

                    batch.drawVertex(v0.x, v0.y, c0, f0, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                    batch.drawVertex(v1.x, v1.y, c1, f1_roof, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                    batch.drawVertex(v2.x, v2.y, c2, f2_roof, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                }
            }
        }
    }

    private void drawShadowCircle(LightBatch batch, Fixture fixture, LightData data, float colBits, Shape fixtureShape, Body body, float lx, float ly, float lz) {
        CircleShape shape = (CircleShape) fixtureShape;
        float r = shape.getRadius();

        float dst = Vector2.dst(center.x, center.y, lx, ly);

        if (dst <= r) return;

        float a = (float) Math.acos(r / dst);
        float l = data.getLimit(dst, pseudo3dHeight, distance);
        float f1 = 1f - dst / distance;
        float f2 = 1f - (dst + l) / distance;

        tmpColor.set(Color.BLACK);
        float startColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1).toFloatBits() : oneColorBits;
        tmpColor.set(Color.WHITE);
        float endColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2).toFloatBits() : colBits;

        tmpVec.set(lx, ly).sub(center).clamp(r, r).rotateRad(a);
        tmpStart.set(center).add(tmpVec);

        float angle = (MathUtils.PI2 - 2f * a) / RayHandler.CIRCLE_APPROX_POINTS;

        tmpEnd.set(tmpStart).sub(lx, ly).setLength(l).add(tmpStart);

        float prevX = tmpStart.x;
        float prevY = tmpStart.y;
        float prevEndX = tmpEnd.x;
        float prevEndY = tmpEnd.y;

        for (int k = 0; k < RayHandler.CIRCLE_APPROX_POINTS; k++) {
            batch.checkSpace(6);

            tmpVec.rotateRad(angle);

            tmpStart.set(center).add(tmpVec);
            tmpEnd.set(tmpStart).sub(lx, ly).setLength(l).add(tmpStart);

            float currX = tmpStart.x;
            float currY = tmpStart.y;
            float currEndX = tmpEnd.x;
            float currEndY = tmpEnd.y;

            batch.drawVertex(prevX, prevY, startColBits, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            batch.drawVertex(prevEndX, prevEndY, endColBits, f2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            batch.drawVertex(currX, currY, startColBits, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);

            batch.drawVertex(currX, currY, startColBits, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            batch.drawVertex(prevEndX, prevEndY, endColBits, f2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            batch.drawVertex(currEndX, currEndY, endColBits, f2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);

            prevX = currX; prevY = currY;
            prevEndX = currEndX; prevEndY = currEndY;
        }
    }

    private void drawShadowEdge(LightBatch batch, Fixture fixture, LightData data, float colBits, Shape fixtureShape, Body body, float lx, float ly, float lz) {
        batch.checkSpace(6);

        EdgeShape shape = (EdgeShape) fixtureShape;

        shape.getVertex1(tmpVec);
        tmpVec.set(body.getWorldPoint(tmpVec));
        float x1 = tmpVec.x;
        float y1 = tmpVec.y;

        float dst1 = Vector2.dst(x1, y1, lx, ly);
        float l1 = data.getLimit(dst1, pseudo3dHeight, distance);
        float f1_start = 1f - dst1 / distance;
        float f1_end = 1f - (dst1 + l1) / distance;

        tmpEnd.set(x1, y1).sub(lx, ly).setLength(l1).add(x1, y1);
        float ex1 = tmpEnd.x;
        float ey1 = tmpEnd.y;

        shape.getVertex2(tmpVec);
        tmpVec.set(body.getWorldPoint(tmpVec));
        float x2 = tmpVec.x;
        float y2 = tmpVec.y;

        float dst2 = Vector2.dst(x2, y2, lx, ly);
        float l2 = data.getLimit(dst2, pseudo3dHeight, distance);
        float f2_start = 1f - dst2 / distance;
        float f2_end = 1f - (dst2 + l2) / distance;

        tmpEnd.set(x2, y2).sub(lx, ly).setLength(l2).add(x2, y2);
        float ex2 = tmpEnd.x;
        float ey2 = tmpEnd.y;

        tmpColor.set(Color.BLACK);
        float c1_start = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1_start).toFloatBits() : oneColorBits;
        tmpColor.set(Color.WHITE);
        float c1_end = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1_end).toFloatBits() : colBits;

        tmpColor.set(Color.BLACK);
        float c2_start = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2_start).toFloatBits() : oneColorBits;
        tmpColor.set(Color.WHITE);
        float c2_end = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2_end).toFloatBits() : colBits;

        batch.drawVertex(x1, y1, c1_start, f1_start, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(ex1, ey1, c1_end, f1_end, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(x2, y2, c2_start, f2_start, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);

        batch.drawVertex(x2, y2, c2_start, f2_start, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(ex1, ey1, c1_end, f1_end, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(ex2, ey2, c2_end, f2_end, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
    }

    public void updateChain() {
        Vector2 v1 = POOLS.obtain(Vector2.class);
        Vector2 v2 = POOLS.obtain(Vector2.class);
        Vector2 vSegmentStart = POOLS.obtain(Vector2.class);
        Vector2 vDirection = POOLS.obtain(Vector2.class);
        Vector2 vRayOffset = POOLS.obtain(Vector2.class);
        Spinor tmpAngle = POOLS.obtain(Spinor.class);
        Spinor previousAngle = POOLS.obtain(Spinor.class);
        Spinor currentAngle = POOLS.obtain(Spinor.class);
        Spinor nextAngle = POOLS.obtain(Spinor.class);
        Spinor startAngle = POOLS.obtain(Spinor.class);
        Spinor endAngle = POOLS.obtain(Spinor.class);
        Spinor rayAngle = POOLS.obtain(Spinor.class);

        int segmentCount = chain.size / 2 - 1;
        segmentAngles.clear();
        segmentLengths.clear();
        float remainingLength = 0;

        for (int i = 0, j = 0; i < chain.size - 2; i += 2, j++) {
            v1.set(chain.items[i + 2], chain.items[i + 3]).sub(chain.items[i], chain.items[i + 1]);
            segmentLengths.add(v1.len());
            segmentAngles.add(v1.rotate90(rayDirection).angleRad());
            remainingLength += segmentLengths.items[j];
        }

        int rayNumber = 0;
        int remainingRays = rayNum;

        for (int i = 0; i < segmentCount; i++) {
            previousAngle.set((i == 0) ? segmentAngles.items[i] : segmentAngles.items[i - 1]);
            currentAngle.set(segmentAngles.items[i]);
            nextAngle.set((i == segmentAngles.size - 1) ? segmentAngles.items[i] : segmentAngles.items[i + 1]);

            startAngle.set(previousAngle).slerp(currentAngle, 0.5f);
            endAngle.set(currentAngle).slerp(nextAngle, 0.5f);

            int segmentVertex = i * 2;
            vSegmentStart.set(chain.items[segmentVertex], chain.items[segmentVertex + 1]);
            vDirection.set(chain.items[segmentVertex + 2], chain.items[segmentVertex + 3]).sub(vSegmentStart).nor();

            float raySpacing = remainingLength / remainingRays;
            int segmentRays = (i == segmentCount - 1) ? remainingRays : (int) ((segmentLengths.items[i] / remainingLength) * remainingRays);

            for (int j = 0; j < segmentRays; j++) {
                float position = j * raySpacing;
                rayAngle.set(startAngle).slerp(endAngle, position / segmentLengths.items[i]);
                float angle = rayAngle.angle();
                vRayOffset.set(this.rayStartOffset, 0).rotateRad(angle);
                v1.set(vDirection).scl(position).add(vSegmentStart).add(vRayOffset);
                this.startX[rayNumber] = v1.x;
                this.startY[rayNumber] = v1.y;
                v2.set(distance, 0).rotateRad(angle).add(v1);
                this.endX[rayNumber] = v2.x;
                this.endY[rayNumber] = v2.y;
                rayNumber++;
            }
            remainingRays -= segmentRays;
            remainingLength -= segmentLengths.items[i];
        }
        POOLS.free(v1); POOLS.free(v2); POOLS.free(vSegmentStart); POOLS.free(vDirection);
        POOLS.free(vRayOffset); POOLS.free(previousAngle); POOLS.free(currentAngle);
        POOLS.free(nextAngle); POOLS.free(startAngle); POOLS.free(endAngle); POOLS.free(rayAngle);
        POOLS.free(tmpAngle);
    }

    void applyAttachment() {
        if (body == null || staticLight) return;
        restorePosition.setToTranslation(bodyPosition);
        rotateAroundZero.setToRotationRad(bodyAngle + bodyAngleOffset);
        for (int i = 0; i < rayNum; i++) {
            tmpVec.set(startX[i], startY[i]).mul(rotateAroundZero).mul(restorePosition);
            startX[i] = tmpVec.x; startY[i] = tmpVec.y;
            tmpVec.set(endX[i], endY[i]).mul(rotateAroundZero).mul(restorePosition);
            endX[i] = tmpVec.x; endY[i] = tmpVec.y;
        }
    }

    protected boolean cull() {
        if (!rayHandler.culling) { culled = false; }
        else {
            updateBoundingRects();
            culled = chainLightBounds.width > 0 && chainLightBounds.height > 0 && !chainLightBounds.overlaps(rayHandlerBounds);
        }
        return culled;
    }

    void updateBody() {
        if (body == null || staticLight) return;
        final Vector2 vec = body.getPosition();
        tmpVec.set(0, 0).sub(bodyPosition);
        bodyPosition.set(vec);
        zeroPosition.setToTranslation(tmpVec);
        restorePosition.setToTranslation(bodyPosition);
        rotateAroundZero.setToRotationRad(bodyAngle).inv().rotateRad(body.getAngle());
        bodyAngle = body.getAngle();
        for (int i = 0; i < rayNum; i++) {
            tmpVec.set(startX[i], startY[i]).mul(zeroPosition).mul(rotateAroundZero).mul(restorePosition);
            startX[i] = tmpVec.x; startY[i] = tmpVec.y;
            tmpVec.set(endX[i], endY[i]).mul(zeroPosition).mul(rotateAroundZero).mul(restorePosition);
            endX[i] = tmpVec.x; endY[i] = tmpVec.y;
        }
    }

    protected void updateMesh() {
        for (int i = 0; i < rayNum; i++) {
            m_index = i;
            f[i] = 1f;
            Vector2 tmpEnd = new Vector2(endX[i], endY[i]);
            mx[i] = tmpEnd.x; my[i] = tmpEnd.y;
            Vector2 tmpStart = new Vector2(startX[i], startY[i]);
            if (rayHandler.world != null && !xray && !rayHandler.pseudo3d) {
                rayHandler.world.rayCast(ray, tmpStart, tmpEnd);
            }
        }
        setMesh();
    }

    protected void setMesh() {
        // segments array not strictly used for ChainLight draw() as we use arrays directly,
        // but kept for compatibility if needed.
    }

    protected void updateBoundingRects() {
        float maxX = startX[0]; float minX = startX[0];
        float maxY = startY[0]; float minY = startY[0];
        for (int i = 0; i < rayNum; i++) {
            maxX = Math.max(maxX, startX[i]); maxX = Math.max(maxX, mx[i]);
            minX = Math.min(minX, startX[i]); minX = Math.min(minX, mx[i]);
            maxY = Math.max(maxY, startY[i]); maxY = Math.max(maxY, my[i]);
            minY = Math.min(minY, startY[i]); minY = Math.min(minY, my[i]);
        }
        chainLightBounds.set(minX, minY, maxX - minX, maxY - minY);
        rayHandlerBounds.set(rayHandler.x1, rayHandler.y1, rayHandler.x2 - rayHandler.x1, rayHandler.y2 - rayHandler.y1);
    }

    private void getClosestPointOnChain(float targetX, float targetY, Vector2 result) {
        float minDst2 = Float.MAX_VALUE;
        result.set(startX[0], startY[0]);

        for (int i = 0; i < rayNum - 1; i++) {
            float x1 = startX[i];
            float y1 = startY[i];
            float x2 = startX[i+1];
            float y2 = startY[i+1];

            float x21 = x2 - x1;
            float y21 = y2 - y1;
            float xP1 = targetX - x1;
            float yP1 = targetY - y1;

            float segLen2 = x21 * x21 + y21 * y21;
            float t = (xP1 * x21 + yP1 * y21) / segLen2;

            t = MathUtils.clamp(t, 0f, 1f);

            float closestX = x1 + t * x21;
            float closestY = y1 + t * y21;

            float dst2 = Vector2.dst2(targetX, targetY, closestX, closestY);

            if (dst2 < minDst2) {
                minDst2 = dst2;
                result.set(closestX, closestY);
            }
        }
    }

    @Override public void setDistance(float dist) { dist *= RayHandler.gammaCorrectionParameter; this.distance = Math.max(dist, 0.01f); dirty = true; }
    @Override public void setDirection(float directionDegree) {}
    @Override public void attachToBody(Body body) { attachToBody(body, 0f); }
    public void attachToBody(Body body, float degrees) { this.body = body; this.bodyPosition.set(body.getPosition()); bodyAngleOffset = MathUtils.degreesToRadians * degrees; bodyAngle = body.getAngle(); applyAttachment(); if (staticLight) dirty = true; }
    @Override public Body getBody() { return body; }
    @Override public float getX() { return tmpPosition.x; }
    @Override public float getY() { return tmpPosition.y; }
    @Override public void setPosition(float x, float y) { tmpPosition.x = x; tmpPosition.y = y; if (staticLight) dirty = true; }
    @Override public void setPosition(Vector2 position) { tmpPosition.x = position.x; tmpPosition.y = position.y; if (staticLight) dirty = true; }
    @Override public boolean contains(float x, float y) { if (!this.chainLightBounds.contains(x, y)) return false; FloatArray vertices = POOLS.obtain(FloatArray.class); vertices.clear(); for (int i = 0; i < rayNum; i++) vertices.addAll(mx[i], my[i]); for (int i = rayNum - 1; i > -1; i--) vertices.addAll(startX[i], startY[i]); int intersects = 0; for (int i = 0; i < vertices.size; i += 2) { float x1 = vertices.items[i]; float y1 = vertices.items[i + 1]; float x2 = vertices.items[(i + 2) % vertices.size]; float y2 = vertices.items[(i + 3) % vertices.size]; if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) && x < ((x2 - x1) / (y2 - y1) * (y - y1) + x1)) intersects++; } boolean result = (intersects & 1) == 1; POOLS.free(vertices); return result; }
}