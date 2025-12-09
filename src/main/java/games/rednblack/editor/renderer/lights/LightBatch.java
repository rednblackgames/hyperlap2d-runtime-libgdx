package games.rednblack.editor.renderer.lights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class LightBatch {
    // 11 floats per vertex: x, y, color, s, intensity, falloff(x,y,z), lightPos(x,y,z)
    static final int VERTEX_SIZE = 11;
    static final int MAX_VERTICES = 32767;
    static final int MAX_INDICES = MAX_VERTICES * 3;

    private final Mesh mesh;
    private final float[] vertices;
    private final short[] indices;

    private int vertexIdx = 0;
    private int indexIdx = 0;
    private int vertCount = 0;

    private ShaderProgram shader;
    private boolean drawing = false;
    public int renderCalls = 0; // For debugging

    private Texture normalMap;
    private float viewWidth, viewHeight;

    public LightBatch(ShaderProgram shader) {
        this.shader = shader;
        this.vertices = new float[MAX_VERTICES * VERTEX_SIZE];
        this.indices = new short[MAX_INDICES];

        Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexBufferObject;
        if (Gdx.gl30 != null) {
            vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
        }

        mesh = new Mesh(vertexDataType, false, MAX_VERTICES, MAX_INDICES,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_s"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_intensity"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_falloff"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_lightPos")
        );
    }

    public void setNormalMap(Texture normalMap) {
        // If we change texture during rendering, we should flush.
        // But in your case (Full Screen Rendering), the texture is global for the frame.
        if (drawing && this.normalMap != normalMap) flush();
        this.normalMap = normalMap;
    }

    public void resize(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
    }

    public void setShader(ShaderProgram shader) {
        if (drawing) flush();
        this.shader = shader;
    }

    public void begin(Matrix4 projectionMatrix) {
        if (drawing) throw new IllegalStateException("begin() called before end()");
        renderCalls = 0;
        drawing = true;
        shader.bind();
        shader.setUniformMatrix("u_projTrans", projectionMatrix);

        // We pass viewport resolution for normal map UV calculations
        shader.setUniformf("u_resolution", viewWidth, viewHeight);

        if (normalMap != null) {
            // Bind Texture unit 0
            normalMap.bind(0);
            shader.setUniformi("u_normals", 0);
            shader.setUniformi("u_useNormals", 1);
        } else {
            shader.setUniformi("u_useNormals", 0);
        }

        vertexIdx = 0;
        indexIdx = 0;
        vertCount = 0;
    }

    public void end() {
        if (!drawing) throw new IllegalStateException("LightBatch.end() called before begin()");
        if (vertexIdx > 0) flush();
        drawing = false;
    }

    /**
     * Draws a light mesh based on pre-calculated segments (Fan format).
     * Automatically converts Triangle Fan -> Triangles for batching.
     */
    public void drawFan(float[] segments, int vertexCount, float intensity, float fx, float fy, float fz, float lx, float ly, float lz, boolean wrapAround) {
        if (vertexCount < 3) return;

        int numTriangles = wrapAround ? vertexCount - 1 : vertexCount - 2;

        if (vertCount + vertexCount > MAX_VERTICES || indexIdx + (numTriangles * 3) > MAX_INDICES) {
            flush();
        }

        // Cache locals
        float[] verts = this.vertices;
        short[] inds = this.indices;
        int vIdx = this.vertexIdx;
        int iIdx = this.indexIdx;
        int startV = this.vertCount;

        verts[vIdx++] = segments[0]; verts[vIdx++] = segments[1]; verts[vIdx++] = segments[2]; verts[vIdx++] = segments[3];
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz; verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        int limit = vertexCount * 4;
        for (int k = 4; k < limit; k += 4) {
            verts[vIdx++] = segments[k];
            verts[vIdx++] = segments[k+1];
            verts[vIdx++] = segments[k+2];
            verts[vIdx++] = segments[k+3];
            verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz; verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;
        }

        int loopCount = wrapAround ? vertexCount - 1 : vertexCount - 2;
        for (int i = 0; i < loopCount; i++) {
            int currentRim = startV + 1 + i;
            int nextRim    = startV + 1 + i + 1;

            if (wrapAround && i == loopCount - 1) {
                nextRim = startV + 1;
            }

            inds[iIdx++] = (short) startV;
            inds[iIdx++] = (short) currentRim;  // P1
            inds[iIdx++] = (short) nextRim;     // P2
        }

        this.vertexIdx = vIdx;
        this.indexIdx = iIdx;
        this.vertCount += vertexCount;
    }

    public void drawQuad(
            // V1
            float x1, float y1, float c1, float s1,
            // V2
            float x2, float y2, float c2, float s2,
            // V3
            float x3, float y3, float c3, float s3,
            // V4
            float x4, float y4, float c4, float s4,
            // Common
            float intensity, float fx, float fy, float fz, float lx, float ly, float lz) {

        if (vertCount + 4 > MAX_VERTICES || indexIdx + 6 > MAX_INDICES) flush();

        float[] verts = this.vertices;
        short[] inds = this.indices;
        int vIdx = this.vertexIdx;
        int iIdx = this.indexIdx;
        int startV = this.vertCount;

        // V1
        verts[vIdx++] = x1; verts[vIdx++] = y1; verts[vIdx++] = c1; verts[vIdx++] = s1;
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz; verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        // V2
        verts[vIdx++] = x2; verts[vIdx++] = y2; verts[vIdx++] = c2; verts[vIdx++] = s2;
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz; verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        // V3
        verts[vIdx++] = x3; verts[vIdx++] = y3; verts[vIdx++] = c3; verts[vIdx++] = s3;
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz; verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        // V4
        verts[vIdx++] = x4; verts[vIdx++] = y4; verts[vIdx++] = c4; verts[vIdx++] = s4;
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz; verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        // V1-V2-V3
        inds[iIdx++] = (short) (startV);
        inds[iIdx++] = (short) (startV + 1);
        inds[iIdx++] = (short) (startV + 2);

        inds[iIdx++] = (short) (startV);
        inds[iIdx++] = (short) (startV + 2);
        inds[iIdx++] = (short) (startV + 3);

        this.vertexIdx = vIdx;
        this.indexIdx = iIdx;
        this.vertCount += 4;
    }

    public void drawTriangle(
            float x1, float y1, float c1, float s1,
            float x2, float y2, float c2, float s2,
            float x3, float y3, float c3, float s3,
            float intensity, float fx, float fy, float fz, float lx, float ly, float lz) {

        if (vertCount + 3 > MAX_VERTICES || indexIdx + 3 > MAX_INDICES) {
            flush();
        }

        float[] verts = this.vertices;
        short[] inds = this.indices;
        int vIdx = this.vertexIdx;
        int iIdx = this.indexIdx;
        int startV = this.vertCount;

        // V1
        verts[vIdx++] = x1; verts[vIdx++] = y1; verts[vIdx++] = c1; verts[vIdx++] = s1;
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz;
        verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        // V2
        verts[vIdx++] = x2; verts[vIdx++] = y2; verts[vIdx++] = c2; verts[vIdx++] = s2;
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz;
        verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        // V3
        verts[vIdx++] = x3; verts[vIdx++] = y3; verts[vIdx++] = c3; verts[vIdx++] = s3;
        verts[vIdx++] = intensity; verts[vIdx++] = fx; verts[vIdx++] = fy; verts[vIdx++] = fz;
        verts[vIdx++] = lx; verts[vIdx++] = ly; verts[vIdx++] = lz;

        inds[iIdx++] = (short) (startV);
        inds[iIdx++] = (short) (startV + 1);
        inds[iIdx++] = (short) (startV + 2);

        this.vertexIdx = vIdx;
        this.indexIdx = iIdx;
        this.vertCount += 3;
    }

    public void flush() {
        if (vertexIdx == 0) return;
        renderCalls++;

        mesh.setVertices(vertices, 0, vertexIdx);
        mesh.setIndices(indices, 0, indexIdx);

        mesh.render(shader, GL20.GL_TRIANGLES, 0, indexIdx);

        vertexIdx = 0;
        indexIdx = 0;
        vertCount = 0;
    }

    public int getRenderCalls() {
        return renderCalls;
    }

    public void dispose() {
        mesh.dispose();
    }
}