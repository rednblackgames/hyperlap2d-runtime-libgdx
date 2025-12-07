package games.rednblack.editor.renderer.lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Abstract base class for all positional lights
 * 
 * <p>Extends {@link Light}
 * 
 * @author kalle_h
 */
public abstract class PositionalLight extends Light {

    Color tmpColor = new Color();

    protected final Vector2 tmpEnd = new Vector2();
    protected final Vector2 start = new Vector2();

    protected Body body;
    protected float bodyOffsetX;
    protected float bodyOffsetY;
    protected float bodyAngleOffset;

    protected float[] sin;
    protected float[] cos;

    protected float[] endX;
    protected float[] endY;

    public PositionalLight(RayHandler rayHandler, int rays, Color color, float distance, float x, float y, float directionDegree) {
        super(rayHandler, rays, color, distance, directionDegree);
        start.x = x;
        start.y = y;

        setMesh();
    }

    @Override
    void update() {
        updateBody();
        if (cull()) return;
        if (staticLight && !dirty) return;
        dirty = false;
        updateMesh();
    }

    @Override
    void draw(LightBatch batch) {
        if (rayHandler.culling && culled) return;

        rayHandler.lightRenderedLastFrame++;

        float lx = getX();
        float ly = getY();
        float lz = pseudo3dHeight;

        //Main light
        batch.drawFan(segments, vertexNum, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);

        if (soft && !xray && !rayHandler.pseudo3d) {
            for (int i = 0; i < rayNum; i++) {
                if (!(this instanceof PointLight) && i == rayNum - 1) continue;

                batch.checkSpace(6);

                int nextI = (i + 1) % rayNum;

                int currentIdx = (i + 1) * 4;
                int nextIdx = (nextI + 1) * 4;

                float cx = segments[currentIdx];
                float cy = segments[currentIdx + 1];
                float cs = segments[currentIdx + 3];

                float nx = segments[nextIdx];
                float ny = segments[nextIdx + 1];
                float ns = segments[nextIdx + 3];

                float softCX = cx + cs * softShadowLength * cos[i];
                float softCY = cy + cs * softShadowLength * sin[i];

                float softNX = nx + ns * softShadowLength * cos[nextI];
                float softNY = ny + ns * softShadowLength * sin[nextI];

                batch.drawVertex(cx, cy, colorF, cs, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(softCX, softCY, zeroColorBits, 0f, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(nx, ny, colorF, ns, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);

                batch.drawVertex(nx, ny, colorF, ns, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(softCX, softCY, zeroColorBits, 0f, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(softNX, softNY, zeroColorBits, 0f, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            }
        }
    }

    @Override
    void drawDynamicShadows(LightBatch batch) {
        float colBits = rayHandler.ambientLight.toFloatBits();

        for (Fixture fixture : affectedFixtures) {
            Object userData = fixture.getUserData();
            if (!(userData instanceof LightData) || fixture.isSensor()) continue;

            LightData data = (LightData) userData;
            // if (data.shadowsDropped >= rayHandler.shadowsDroppedLimit) continue;

            Shape fixtureShape = fixture.getShape();
            Shape.Type type = fixtureShape.getType();
            Body body = fixture.getBody();
            center.set(body.getWorldCenter());

            if (type == Shape.Type.Polygon || type == Shape.Type.Chain) {
                drawShadowPolygon(batch, fixture, data, colBits, fixtureShape, type, body);
            } else if (type == Shape.Type.Circle) {
                drawShadowCircle(batch, fixture, data, colBits, fixtureShape, body);
            } else if (type == Shape.Type.Edge) {
                drawShadowEdge(batch, fixture, data, colBits, fixtureShape, body);
            }
        }
    }

    private void drawShadowPolygon(LightBatch batch, Fixture fixture, LightData data, float colBits, Shape fixtureShape, Shape.Type type, Body body) {
        boolean isPolygon = (type == Shape.Type.Polygon);
        ChainShape cShape = isPolygon ? null : (ChainShape) fixtureShape;
        PolygonShape pShape = isPolygon ? (PolygonShape) fixtureShape : null;
        int vertexCount = isPolygon ? pShape.getVertexCount() : cShape.getVertexCount();

        int minN = -1;
        int maxN = -1;
        int minDstN = -1;
        float minDst = Float.POSITIVE_INFINITY;
        boolean hasGasp = false;

        tmpVerts.clear();
        for (int n = 0; n < vertexCount; n++) {
            if (isPolygon) {
                pShape.getVertex(n, tmpVec);
            } else {
                cShape.getVertex(n, tmpVec);
            }
            tmpVec.set(body.getWorldPoint(tmpVec));
            tmpVerts.add(tmpVec.cpy());
            tmpEnd.set(tmpVec).sub(start).limit2(0.0001f).add(tmpVec);

            if (fixture.testPoint(tmpEnd)) {
                if (minN == -1) minN = n;
                maxN = n;
                hasGasp = true;
                continue;
            }

            float currDist = tmpVec.dst2(start);
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

            if (Intersector.pointLineSide(start, center, tmpVec) > 0) {
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

        float lx = getX();
        float ly = getY();
        float lz = pseudo3dHeight;

        for (int n : ind.toArray()) {
            tmpVec.set(tmpVerts.get(n));

            float dst = tmpVec.dst(start);
            float l = data.getLimit(dst, pseudo3dHeight, distance);
            tmpEnd.set(tmpVec).sub(start).setLength(l).add(tmpVec);

            float f1 = 1f - dst / distance;
            float f2 = 1f - (dst + l) / distance;

            tmpColor.set(Color.BLACK);
            float startColBits = rayHandler.shadowColorInterpolation ?
                    tmpColor.lerp(rayHandler.ambientLight, f1).toFloatBits() : oneColorBits;
            tmpColor.set(Color.WHITE);
            float endColBits = rayHandler.shadowColorInterpolation ?
                    tmpColor.lerp(rayHandler.ambientLight, f2).toFloatBits() : colBits;

            float currX = tmpVec.x;
            float currY = tmpVec.y;
            float currEndX = tmpEnd.x;
            float currEndY = tmpEnd.y;

            if (!first) {
                batch.checkSpace(6);

                batch.drawVertex(prevX, prevY, startColBits, prevF1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(prevEndX, prevEndY, endColBits, prevF2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(currX, currY, startColBits, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);

                batch.drawVertex(currX, currY, startColBits, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(prevEndX, prevEndY, endColBits, prevF2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(currEndX, currEndY, endColBits, f2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            }

            prevX = currX; prevY = currY;
            prevEndX = currEndX; prevEndY = currEndY;
            prevStartCol = startColBits; prevEndCol = endColBits;
            prevF1 = f1; prevF2 = f2;
            first = false;
        }

        if (data.roofShadow || pseudo3dHeight <= data.height) {
            for (int n = 0; n < vertexCount; n++) {
                batch.checkSpace(3);

                if (n < 2) continue;

                tmpVec.set(tmpVerts.get(0));
                float dst = tmpVec.dst(start);
                float f1 = 1f - dst / distance;
                float col1 = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1).toFloatBits() : oneColorBits;

                tmpEnd.set(tmpVerts.get(n-1));
                float dst2 = tmpEnd.dst(start);
                float f2 = 1f - dst2 / distance;
                float col2 = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2).toFloatBits() : oneColorBits;

                Vector2 v3 = tmpVerts.get(n);
                float dst3 = v3.dst(start);
                float f3 = 1f - dst3 / distance;
                float col3 = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f3).toFloatBits() : oneColorBits;

                batch.drawVertex(tmpVec.x, tmpVec.y, col1, f1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(tmpEnd.x, tmpEnd.y, col2, f2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
                batch.drawVertex(v3.x, v3.y, col3, f3, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
            }
        }
    }

    private void drawShadowCircle(LightBatch batch, Fixture fixture, LightData data, float colBits, Shape fixtureShape, Body body) {
        CircleShape shape = (CircleShape) fixtureShape;
        float r = shape.getRadius();
        if (!contains(tmpVec.set(center).add(r, r)) && !contains(tmpVec.set(center).add(-r, -r))
                && !contains(tmpVec.set(center).add(r, -r)) && !contains(tmpVec.set(center).add(-r, r))) {
            return;
        }

        float lx = getX();
        float ly = getY();
        float lz = pseudo3dHeight;

        float dst = tmpVec.set(center).dst(start);
        float a = (float) Math.acos(r / dst);
        float l = data.getLimit(dst, pseudo3dHeight, distance);
        float f1 = 1f - dst / distance;
        float f2 = 1f - (dst + l) / distance;

        tmpColor.set(Color.BLACK);
        float startColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1).toFloatBits() : oneColorBits;
        tmpColor.set(Color.WHITE);
        float endColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2).toFloatBits() : colBits;

        tmpVec.set(start).sub(center).clamp(r, r).rotateRad(a);
        tmpStart.set(center).add(tmpVec);

        float angle = (MathUtils.PI2 - 2f * a) / RayHandler.CIRCLE_APPROX_POINTS;

        tmpStart.set(center).add(tmpVec);
        tmpEnd.set(tmpStart).sub(start).setLength(l).add(tmpStart);

        float prevX = tmpStart.x;
        float prevY = tmpStart.y;
        float prevEndX = tmpEnd.x;
        float prevEndY = tmpEnd.y;

        for (int k = 0; k < RayHandler.CIRCLE_APPROX_POINTS; k++) {
            batch.checkSpace(6);
            tmpVec.rotateRad(angle);

            tmpStart.set(center).add(tmpVec);
            tmpEnd.set(tmpStart).sub(start).setLength(l).add(tmpStart);

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

    private void drawShadowEdge(LightBatch batch, Fixture fixture, LightData data, float colBits, Shape fixtureShape, Body body) {
        EdgeShape shape = (EdgeShape) fixtureShape;
        float lx = getX();
        float ly = getY();
        float lz = pseudo3dHeight;

        // P1
        shape.getVertex1(tmpVec);
        tmpVec.set(body.getWorldPoint(tmpVec));
        if (!contains(tmpVec)) return;

        batch.checkSpace(6);

        float dst1 = tmpVec.dst(start);
        float l1 = data.getLimit(dst1, pseudo3dHeight, distance);
        float f1_1 = 1f - dst1 / distance;
        float f2_1 = 1f - (dst1 + l1) / distance;

        float x1 = tmpVec.x;
        float y1 = tmpVec.y;

        tmpEnd.set(tmpVec).sub(start).setLength(l1).add(tmpVec);
        float ex1 = tmpEnd.x;
        float ey1 = tmpEnd.y;

        // P2
        shape.getVertex2(tmpVec);
        tmpVec.set(body.getWorldPoint(tmpVec));
        if (!contains(tmpVec)) return;

        float dst2 = tmpVec.dst(start);
        float l2 = data.getLimit(dst2, pseudo3dHeight, distance);
        float f1_2 = 1f - dst2 / distance;
        float f2_2 = 1f - (dst2 + l2) / distance;

        float x2 = tmpVec.x;
        float y2 = tmpVec.y;

        tmpEnd.set(tmpVec).sub(start).setLength(l2).add(tmpVec);
        float ex2 = tmpEnd.x;
        float ey2 = tmpEnd.y;

        // Colors
        tmpColor.set(Color.BLACK);
        float c1_start = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f1_1).toFloatBits() : oneColorBits;
        tmpColor.set(Color.WHITE);
        float c1_end = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, f2_1).toFloatBits() : colBits;

        batch.drawVertex(x1, y1, c1_start, f1_1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(ex1, ey1, c1_end, f2_1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(x2, y2, c1_start, f1_2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz); // Uso c1_start approx o ricalcola

        batch.drawVertex(x2, y2, c1_start, f1_2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(ex1, ey1, c1_end, f2_1, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz);
        batch.drawVertex(ex2, ey2, c1_end, f2_2, intensity, falloff.x, falloff.y, falloff.z, lx, ly, lz); // Uso c1_end approx
    }

    @Override
    protected void setRayNum(int rays) {
        super.setRayNum(rays);
        sin = new float[rays];
        cos = new float[rays];
        endX = new float[rays];
        endY = new float[rays];
    }

    protected boolean cull() {
        culled = rayHandler.culling && !rayHandler.intersect(
                start.x, start.y, distance + softShadowLength);
        return culled;
    }

    protected void updateBody() {
        if (body == null || staticLight) return;

        final Vector2 vec = body.getPosition();
        float angle = body.getAngle();
        final float cos = MathUtils.cos(angle);
        final float sin = MathUtils.sin(angle);
        final float dX = bodyOffsetX * cos - bodyOffsetY * sin;
        final float dY = bodyOffsetX * sin + bodyOffsetY * cos;
        start.x = vec.x + dX;
        start.y = vec.y + dY;
        setDirection(bodyAngleOffset + angle * MathUtils.radiansToDegrees);
    }

    protected void updateMesh() {
        for (int i = 0; i < rayNum; i++) {
            m_index = i;
            f[i] = 1f;
            tmpEnd.x = endX[i] + start.x;
            mx[i] = tmpEnd.x;
            tmpEnd.y = endY[i] + start.y;
            my[i] = tmpEnd.y;
            if (rayHandler.world != null && !xray && !rayHandler.pseudo3d) {
                rayHandler.world.rayCast(ray, start, tmpEnd);
            }
        }
        setMesh();
    }

    protected void prepareFixtureData() {
        rayHandler.world.QueryAABB(
                dynamicShadowCallback,
                start.x - distance, start.y - distance,
                start.x + distance, start.y + distance);
    }

    protected void setMesh() {
        // ray starting point
        int size = 0;
        segments[size++] = start.x;
        segments[size++] = start.y;
        segments[size++] = colorF;
        segments[size++] = 0f;

        // rays ending points.
        for (int i = 0; i < rayNum; i++) {
            segments[size++] = mx[i];
            segments[size++] = my[i];
            segments[size++] = colorF;
            segments[size++] = f[i];
        }
    }

    @Override
    public void attachToBody(Body body) {
        attachToBody(body, 0f, 0f, 0f);
    }

    public void attachToBody(Body body, float offsetX, float offsetY) {
        attachToBody(body, offsetX, offsetY, 0f);
    }

    public void attachToBody(Body body, float offsetX, float offsetY, float degrees) {
        this.body = body;
        bodyOffsetX = offsetX;
        bodyOffsetY = offsetY;
        bodyAngleOffset = degrees;
        if (staticLight) dirty = true;
    }

    @Override
    public Vector2 getPosition() {
        tmpPosition.x = start.x;
        tmpPosition.y = start.y;
        tmpPosition.y = start.y;
        return tmpPosition;
    }

    public Body getBody() {
        return body;
    }

    @Override
    public float getX() {
        return start.x;
    }

    @Override
    public float getY() {
        return start.y;
    }

    @Override
    public void setPosition(float x, float y) {
        start.x = x;
        start.y = y;
        if (staticLight) dirty = true;
    }

    @Override
    public void setPosition(Vector2 position) {
        start.x = position.x;
        start.y = position.y;
        if (staticLight) dirty = true;
    }

    public boolean contains(Vector2 pos) {
        return contains(pos.x, pos.y);
    }

    @Override
    public boolean contains(float x, float y) {
        // fast fail
        final float x_d = start.x - x;
        final float y_d = start.y - y;
        final float dst2 = x_d * x_d + y_d * y_d;
        if (distance * distance <= dst2) return false;

        // actual check
        boolean oddNodes = false;
        float x2 = mx[rayNum] = start.x;
        float y2 = my[rayNum] = start.y;
        float x1, y1;
        for (int i = 0; i <= rayNum; x2 = x1, y2 = y1, ++i) {
            x1 = mx[i];
            y1 = my[i];
            if (((y1 < y) && (y2 >= y)) || (y1 >= y) && (y2 < y)) {
                if ((y - y1) / (y2 - y1) * (x2 - x1) < (x - x1)) oddNodes = !oddNodes;
            }
        }
        return oddNodes;
    }

    public float getBodyOffsetX() { return bodyOffsetX; }
    public float getBodyOffsetY() { return bodyOffsetY; }
    public float getBodyAngleOffset() { return bodyAngleOffset; }
    public void setBodyOffsetX(float bodyOffsetX) { this.bodyOffsetX = bodyOffsetX; }
    public void setBodyOffsetY(float bodyOffsetY) { this.bodyOffsetY = bodyOffsetY; }
    public void setBodyAngleOffset(float bodyAngleOffset) { this.bodyAngleOffset = bodyAngleOffset; }
}
