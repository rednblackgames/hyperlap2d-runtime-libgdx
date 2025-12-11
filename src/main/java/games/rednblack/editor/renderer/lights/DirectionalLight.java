package games.rednblack.editor.renderer.lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Light which source is at infinite distance
 * 
 * <p>Extends {@link Light}
 * 
 * @author kalle_h
 */
public class DirectionalLight extends Light {
    protected boolean flipDirection = false;
    Color tmpColor = new Color();

    protected final Vector2[] start;
    protected final Vector2[] end;
    protected float sin;
    protected float cos;

    protected Body body;

    // Dynamic shadows variables
    protected final Vector2 lstart = new Vector2();
    protected float xDisp;
    protected float yDisp;

    public DirectionalLight(RayHandler rayHandler, int rays, Color color, float directionDegree) {
        super(rayHandler, rays, color, Float.POSITIVE_INFINITY, directionDegree);

        setFalloff(1f, 0f, 0f);

        start = new Vector2[rayNum];
        end = new Vector2[rayNum];
        for (int i = 0; i < rayNum; i++) {
            start[i] = new Vector2();
            end[i] = new Vector2();
        }

        update();
    }

    @Override
    public void setDirection(float direction) {
        if (flipDirection) direction += 180;
        this.direction = direction;
        sin = MathUtils.sinDeg(direction);
        cos = MathUtils.cosDeg(direction);
        if (staticLight) dirty = true;
    }

    @Override
    void update() {
        if (rayHandler.pseudo3d) {
            float width = (rayHandler.x2 - rayHandler.x1);
            float height = (rayHandler.y2 - rayHandler.y1);
            float sizeOfScreen = Math.max(width, height);
            xDisp = -sizeOfScreen * cos;
            yDisp = -sizeOfScreen * sin;

            prepareFixtureData();
        }

        if (staticLight && !dirty) return;
        dirty = false;

        final float width = (rayHandler.x2 - rayHandler.x1);
        final float height = (rayHandler.y2 - rayHandler.y1);
        final float sizeOfScreen = Math.max(width, height);

        float xAxelOffSet = sizeOfScreen * cos;
        float yAxelOffSet = sizeOfScreen * sin;

        if ((xAxelOffSet * xAxelOffSet < 0.1f) && (yAxelOffSet * yAxelOffSet < 0.1f)) {
            xAxelOffSet = 1;
            yAxelOffSet = 1;
        }

        final float widthOffSet = sizeOfScreen * -sin;
        final float heightOffSet = sizeOfScreen * cos;

        float x = (rayHandler.x1 + rayHandler.x2) * 0.5f - widthOffSet;
        float y = (rayHandler.y1 + rayHandler.y2) * 0.5f - heightOffSet;

        final float portionX = 2f * widthOffSet / (rayNum - 1);
        x = (MathUtils.floor(x / (portionX * 2))) * portionX * 2;
        final float portionY = 2f * heightOffSet / (rayNum - 1);
        y = (MathUtils.ceil(y / (portionY * 2))) * portionY * 2;

        for (int i = 0; i < rayNum; i++) {
            final float steppedX = i * portionX + x;
            final float steppedY = i * portionY + y;
            m_index = i;
            start[i].x = steppedX - xAxelOffSet;
            start[i].y = steppedY - yAxelOffSet;

            mx[i] = end[i].x = steppedX + xAxelOffSet;
            my[i] = end[i].y = steppedY + yAxelOffSet;

            if (rayHandler.world != null && !xray && !rayHandler.pseudo3d) {
                rayHandler.world.rayCast(ray, start[i], end[i]);
            }
        }

        int size = 0;
        for (int i = 0; i < rayNum; i++) {
            segments[size++] = start[i].x;
            segments[size++] = start[i].y;
            segments[size++] = colorF;
            segments[size++] = 1f;

            segments[size++] = mx[i];
            segments[size++] = my[i];
            segments[size++] = colorF;
            segments[size++] = 1f;
        }
    }

    @Override
    void draw(LightBatch batch) {
        if (rayHandler.pseudo3d && pseudo3dHeight <= 0) return;

        rayHandler.lightRenderedLastFrame++;

        float dist = 10000f;
        float centerX = (rayHandler.x1 + rayHandler.x2) * 0.5f;
        float centerY = (rayHandler.y1 + rayHandler.y2) * 0.5f;
        float virtualLightX = centerX - cos * dist;
        float virtualLightY = centerY - sin * dist;
        float virtualLightZ = pseudo3dHeight;

        for (int i = 0; i < rayNum - 1; i++) {
            int idx_s1 = i * 8;      // Start Ray i
            int idx_e1 = i * 8 + 4;  // End Ray i

            int idx_s2 = (i + 1) * 8;     // Start Ray i+1
            int idx_e2 = (i + 1) * 8 + 4; // End Ray i+1

            batch.drawQuad(
                    segments[idx_s1], segments[idx_s1+1], segments[idx_s1+2], segments[idx_s1+3],
                    segments[idx_e1], segments[idx_e1+1], segments[idx_e1+2], segments[idx_e1+3],
                    segments[idx_e2], segments[idx_e2+1], segments[idx_e2+2], segments[idx_e2+3],
                    segments[idx_s2], segments[idx_s2+1], segments[idx_s2+2], segments[idx_s2+3],
                    intensity, falloff.x, falloff.y, falloff.z, virtualLightX, virtualLightY, virtualLightZ
            );
        }

        // 2. Soft Shadows
        if (soft && !xray && !rayHandler.pseudo3d) {
            for (int i = 0; i < rayNum - 1; i++) {
                int idx_e1 = i * 8 + 4;
                int idx_e2 = (i + 1) * 8 + 4;

                float e1x = segments[idx_e1];
                float e1y = segments[idx_e1+1];
                float s1 = segments[idx_e1+3];

                float e2x = segments[idx_e2];
                float e2y = segments[idx_e2+1];
                float s2 = segments[idx_e2+3];

                // Extrude
                float soft1x = e1x + softShadowLength * cos;
                float soft1y = e1y + softShadowLength * sin;
                float soft2x = e2x + softShadowLength * cos;
                float soft2y = e2y + softShadowLength * sin;

                // Quad Soft
                batch.drawQuad(
                        e1x, e1y, colorF, s1,
                        soft1x, soft1y, zeroColorBits, 1f,
                        soft2x, soft2y, zeroColorBits, 1f,
                        e2x, e2y, colorF, s2,
                        intensity, falloff.x, falloff.y, falloff.z, virtualLightX, virtualLightY, virtualLightZ
                );
            }
        }
    }

    @Override
    void drawDynamicShadows(LightBatch batch) {
        if (pseudo3dHeight <= 0f) return;

        float colBits = rayHandler.ambientLight.toFloatBits();
        // Virtual Position per shadows
        float dist = 10000f;
        float centerX = (rayHandler.x1 + rayHandler.x2) * 0.5f;
        float centerY = (rayHandler.y1 + rayHandler.y2) * 0.5f;
        float vlx = centerX - cos * dist;
        float vly = centerY - sin * dist;
        float vlz = pseudo3dHeight;
        float tan = (float) Math.tan(pseudo3dHeight * MathUtils.degRad);

        for (Fixture fixture : affectedFixtures) {
            Object userData = fixture.getUserData();
            if (!(userData instanceof LightData)) continue;

            LightData data = (LightData) userData;
            // if (data.shadowsDropped >= rayHandler.shadowsDroppedLimit) continue;

            Shape fixtureShape = fixture.getShape();
            Shape.Type type = fixtureShape.getType();
            Body body = fixture.getBody();
            center.set(body.getWorldCenter());
            lstart.set(center).add(xDisp, yDisp);

            float l = data.height / tan;
            float f = 1f;

            tmpColor.set(SHADOW_STRENGTH);
            float startColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, 1 - f).toFloatBits() : shadowStrengthBits;
            tmpColor.set(Color.BLACK);
            float endColBits = rayHandler.shadowColorInterpolation ? tmpColor.lerp(rayHandler.ambientLight, 1 - f).toFloatBits() : colBits;

            if (type == Shape.Type.Polygon || type == Shape.Type.Chain) {
                drawShadowPolygon(batch, fixture, data, startColBits, endColBits, f, l, fixtureShape, type, body, vlx, vly, vlz);
            } else if (type == Shape.Type.Circle) {
                drawShadowCircle(batch, fixture, data, startColBits, endColBits, f, l, fixtureShape, body, vlx, vly, vlz);
            } else if (type == Shape.Type.Edge) {
                drawShadowEdge(batch, fixture, data, startColBits, endColBits, f, l, fixtureShape, body, vlx, vly, vlz);
            }
        }
    }

    private void drawShadowPolygon(LightBatch batch, Fixture fixture, LightData data, float startCol, float endCol, float f, float l, Shape fixtureShape, Shape.Type type, Body body, float vlx, float vly, float vlz) {
        boolean isPolygon = (type == Shape.Type.Polygon);
        ChainShape cShape = isPolygon ? null : (ChainShape) fixtureShape;
        PolygonShape pShape = isPolygon ? (PolygonShape) fixtureShape : null;
        int vertexCount = isPolygon ? pShape.getVertexCount() : cShape.getVertexCount();

        int minN = -1; int maxN = -1; int minDstN = -1;
        float minDst = Float.POSITIVE_INFINITY;
        boolean hasGasp = false;

        tmpVerts.clear();
        for (int n = 0; n < vertexCount; n++) {
            if (isPolygon) pShape.getVertex(n, tmpVec);
            else cShape.getVertex(n, tmpVec);
            tmpVec.set(body.getWorldPoint(tmpVec));
            tmpVerts.add(tmpVec.cpy());

            tmpEnd.set(tmpVec).sub(lstart).limit2(0.0001f).add(tmpVec);
            if (fixture.testPoint(tmpEnd)) {
                if (minN == -1) minN = n;
                maxN = n;
                hasGasp = true;
                continue;
            }
            float currDist = tmpVec.dst2(lstart);
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
            if (Intersector.pointLineSide(lstart, center, tmpVec) > 0) {
                ind.reverse();
                ind.insert(0, ind.pop());
            }
        } else if (minN == 0 && maxN == vertexCount - 1) {
            for (int n = maxN - 1; n > minN; n--) ind.add(n);
        } else {
            for (int n = minN - 1; n > -1; n--) ind.add(n);
            for (int n = vertexCount - 1; n > maxN; n--) ind.add(n);
        }

        float prevX = 0, prevY = 0, prevEndX = 0, prevEndY = 0;
        boolean first = true;

        for (int n : ind.toArray()) {
            tmpVec.set(tmpVerts.get(n));
            tmpEnd.set(tmpVec).sub(lstart).setLength(l).add(tmpVec); // Directional uses lstart (virtual source)

            if (!first) {
                batch.drawQuad(
                        prevEndX, prevEndY, endCol, f,
                        tmpEnd.x, tmpEnd.y, endCol, f,
                        tmpVec.x, tmpVec.y, startCol, f,
                        prevX, prevY, startCol, f,
                        intensity, falloff.x, falloff.y, falloff.z, vlx, vly, vlz
                );
            }
            prevX = tmpVec.x; prevY = tmpVec.y;
            prevEndX = tmpEnd.x; prevEndY = tmpEnd.y;
            first = false;
        }

        if (data.roofShadow) {
            int vCount = tmpVerts.size;

            if (vCount >= 3) {
                Vector2 v0 = tmpVerts.get(0);

                for (int i = 1; i < vCount - 1; i++) {
                    Vector2 v1 = tmpVerts.get(i);
                    Vector2 v2 = tmpVerts.get(i + 1);

                    batch.drawTriangle(
                            v0.x, v0.y, startCol, f,
                            v1.x, v1.y, startCol, f,
                            v2.x, v2.y, startCol, f,
                            intensity, falloff.x, falloff.y, falloff.z, vlx, vly, vlz
                    );
                }
            }
        }
    }

    private void drawShadowCircle(LightBatch batch, Fixture fixture, LightData data, float startCol, float endCol, float f, float l, Shape fixtureShape, Body body, float vlx, float vly, float vlz) {
        CircleShape shape = (CircleShape) fixtureShape;
        float r = shape.getRadius();
        float dst = tmpVec.set(center).dst(lstart);
        float a = (float) Math.acos(r / dst);

        tmpVec.set(lstart).sub(center).clamp(r, r).rotateRad(a);
        tmpStart.set(center).add(tmpVec);

        float angle = (MathUtils.PI2 - 2f * a) / RayHandler.CIRCLE_APPROX_POINTS;

        tmpEnd.set(tmpStart).sub(lstart).setLength(l).add(tmpStart);
        float prevX = tmpStart.x, prevY = tmpStart.y, prevEndX = tmpEnd.x, prevEndY = tmpEnd.y;

        for (int k = 0; k < RayHandler.CIRCLE_APPROX_POINTS; k++) {
            tmpVec.rotateRad(angle);
            tmpStart.set(center).add(tmpVec);
            tmpEnd.set(tmpStart).sub(lstart).setLength(l).add(tmpStart);

            batch.drawQuad(
                    prevX, prevY, startCol, f,
                    prevEndX, prevEndY, endCol, f,
                    tmpEnd.x, tmpEnd.y, endCol, f,
                    tmpStart.x, tmpStart.y, startCol, f,
                    intensity, falloff.x, falloff.y, falloff.z, vlx, vly, vlz
            );

            prevX = tmpStart.x; prevY = tmpStart.y; prevEndX = tmpEnd.x; prevEndY = tmpEnd.y;
        }
    }

    private void drawShadowEdge(LightBatch batch, Fixture fixture, LightData data, float startCol, float endCol, float f, float l, Shape fixtureShape, Body body, float vlx, float vly, float vlz) {
        EdgeShape shape = (EdgeShape) fixtureShape;
        shape.getVertex1(tmpVec); tmpVec.set(body.getWorldPoint(tmpVec));
        float x1 = tmpVec.x, y1 = tmpVec.y;
        tmpEnd.set(tmpVec).sub(lstart).setLength(l).add(tmpVec);
        float ex1 = tmpEnd.x, ey1 = tmpEnd.y;

        shape.getVertex2(tmpVec); tmpVec.set(body.getWorldPoint(tmpVec));
        float x2 = tmpVec.x, y2 = tmpVec.y;
        tmpEnd.set(tmpVec).sub(lstart).setLength(l).add(tmpVec);
        float ex2 = tmpEnd.x, ey2 = tmpEnd.y;

        batch.drawQuad(
                x1, y1, startCol, f,
                ex1, ey1, endCol, f,
                ex2, ey2, endCol, f,
                x2, y2, startCol, f,
                intensity, falloff.x, falloff.y, falloff.z, vlx, vly, vlz
        );
    }

    protected void prepareFixtureData() {
        rayHandler.world.QueryAABB(
                dynamicShadowCallback,
                rayHandler.x1, rayHandler.y1,
                rayHandler.x2, rayHandler.y2);
    }

    @Override public void setPosition(float x, float y) {}
    @Override public void setPosition(Vector2 position) {}
    @Override public float getX() { return 0; }
    @Override public float getY() { return 0; }
    @Override public void setDistance(float dist) {}
    @Override public void setIgnoreAttachedBody(boolean flag) {}
    @Override public boolean getIgnoreAttachedBody() { return false; }
    @Override public Body getBody() { return body; }
    @Override public void attachToBody(Body body) {} // Not supported
    public void setIgnoreBody(Body body) { this.body = body; ignoreBody = (body != null); }

    @Override public void setHeight(float degrees) {
        degrees = degrees % 360;
        if (degrees < 0) degrees += 360;

        boolean oldFlip = flipDirection;

        if (degrees > 180f) {
            pseudo3dHeight = -0.01f;
            flipDirection = false;
        } else if (degrees > 90f) {
            pseudo3dHeight = 180f - degrees;
            flipDirection = true;
        } else {
            pseudo3dHeight = degrees;
            flipDirection = false;
        }

        if (oldFlip != flipDirection) {
            setDirection(this.direction);
        }

        if (staticLight) dirty = true;
    }
}