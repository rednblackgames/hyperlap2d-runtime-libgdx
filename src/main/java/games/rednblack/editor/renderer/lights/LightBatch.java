package games.rednblack.editor.renderer.lights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class LightBatch {
    // 11 floats per vertex: x, y, color, s, intensity, falloff(x,y,z), lightPos(x,y,z)
    static final int VERTEX_SIZE = 11;
    static final int MAX_VERTICES = 32_767 * VERTEX_SIZE;

    private final Mesh mesh;
    private final float[] vertices;
    private int idx = 0;

    private ShaderProgram shader;
    private boolean drawing = false;
    public int renderCalls = 0; // For debugging

    private Texture normalMap;
    private float viewWidth, viewHeight;

    public LightBatch(ShaderProgram shader) {
        this.shader = shader;
        this.vertices = new float[MAX_VERTICES];

        Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexBufferObject;
        if (Gdx.gl30 != null) {
            vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
        }

        mesh = new Mesh(vertexDataType, false, 32_767, 0,
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
        idx = 0;
    }

    public void end() {
        if (!drawing) throw new IllegalStateException("LightBatch.end() called before begin()");
        if (idx > 0) flush();
        drawing = false;
    }

    /**
     * Checks if there is space for N vertices. If not, it flushes.
     * Use this before drawing a triangle (3) or a quad (6).
     */
    public void checkSpace(int numVertices) {
        if (idx + (numVertices * VERTEX_SIZE) >= vertices.length) {
            flush();
        }
    }

    /**
     * Draws a light mesh based on pre-calculated segments (Fan format).
     * Automatically converts Triangle Fan -> Triangles for batching.
     */
    public void drawFan(float[] segments, int vertexCount, float intensity, float fx, float fy, float fz, float lx, float ly, float lz) {
        if (vertexCount < 3) return;

        int triangles = vertexCount - 1;
        int totalFloatsNeeded = triangles * 3 * VERTEX_SIZE;

        if (idx + totalFloatsNeeded > vertices.length) {
            flush();
        }

        float[] localVerts = this.vertices;
        int i = this.idx;

        // Center
        float cx = segments[0];
        float cy = segments[1];
        float cc = segments[2];
        float cs = segments[3];

        int limit = vertexCount - 1;
        int k = 1;
        int p_idx = 4;

        int unrollLimit = limit - 3;

        for (; k < unrollLimit; k += 4) {
            // V1
            localVerts[i] = cx; localVerts[i+1] = cy; localVerts[i+2] = cc; localVerts[i+3] = cs;
            localVerts[i+4] = intensity; localVerts[i+5] = fx; localVerts[i+6] = fy; localVerts[i+7] = fz; localVerts[i+8] = lx; localVerts[i+9] = ly; localVerts[i+10] = lz;

            // V2
            localVerts[i+11] = segments[p_idx]; localVerts[i+12] = segments[p_idx+1]; localVerts[i+13] = segments[p_idx+2]; localVerts[i+14] = segments[p_idx+3];
            localVerts[i+15] = intensity; localVerts[i+16] = fx; localVerts[i+17] = fy; localVerts[i+18] = fz; localVerts[i+19] = lx; localVerts[i+20] = ly; localVerts[i+21] = lz;

            // V3
            localVerts[i+22] = segments[p_idx+4]; localVerts[i+23] = segments[p_idx+5]; localVerts[i+24] = segments[p_idx+6]; localVerts[i+25] = segments[p_idx+7];
            localVerts[i+26] = intensity; localVerts[i+27] = fx; localVerts[i+28] = fy; localVerts[i+29] = fz; localVerts[i+30] = lx; localVerts[i+31] = ly; localVerts[i+32] = lz;

            // V1
            localVerts[i+33] = cx; localVerts[i+34] = cy; localVerts[i+35] = cc; localVerts[i+36] = cs;
            localVerts[i+37] = intensity; localVerts[i+38] = fx; localVerts[i+39] = fy; localVerts[i+40] = fz; localVerts[i+41] = lx; localVerts[i+42] = ly; localVerts[i+43] = lz;

            // V2
            localVerts[i+44] = segments[p_idx+4]; localVerts[i+45] = segments[p_idx+5]; localVerts[i+46] = segments[p_idx+6]; localVerts[i+47] = segments[p_idx+7];
            localVerts[i+48] = intensity; localVerts[i+49] = fx; localVerts[i+50] = fy; localVerts[i+51] = fz; localVerts[i+52] = lx; localVerts[i+53] = ly; localVerts[i+54] = lz;

            // V3
            localVerts[i+55] = segments[p_idx+8]; localVerts[i+56] = segments[p_idx+9]; localVerts[i+57] = segments[p_idx+10]; localVerts[i+58] = segments[p_idx+11];
            localVerts[i+59] = intensity; localVerts[i+60] = fx; localVerts[i+61] = fy; localVerts[i+62] = fz; localVerts[i+63] = lx; localVerts[i+64] = ly; localVerts[i+65] = lz;

            localVerts[i+66] = cx; localVerts[i+67] = cy; localVerts[i+68] = cc; localVerts[i+69] = cs;
            localVerts[i+70] = intensity; localVerts[i+71] = fx; localVerts[i+72] = fy; localVerts[i+73] = fz; localVerts[i+74] = lx; localVerts[i+75] = ly; localVerts[i+76] = lz;

            localVerts[i+77] = segments[p_idx+8]; localVerts[i+78] = segments[p_idx+9]; localVerts[i+79] = segments[p_idx+10]; localVerts[i+80] = segments[p_idx+11];
            localVerts[i+81] = intensity; localVerts[i+82] = fx; localVerts[i+83] = fy; localVerts[i+84] = fz; localVerts[i+85] = lx; localVerts[i+86] = ly; localVerts[i+87] = lz;

            localVerts[i+88] = segments[p_idx+12]; localVerts[i+89] = segments[p_idx+13]; localVerts[i+90] = segments[p_idx+14]; localVerts[i+91] = segments[p_idx+15];
            localVerts[i+92] = intensity; localVerts[i+93] = fx; localVerts[i+94] = fy; localVerts[i+95] = fz; localVerts[i+96] = lx; localVerts[i+97] = ly; localVerts[i+98] = lz;

            localVerts[i+99] = cx; localVerts[i+100] = cy; localVerts[i+101] = cc; localVerts[i+102] = cs;
            localVerts[i+103] = intensity; localVerts[i+104] = fx; localVerts[i+105] = fy; localVerts[i+106] = fz; localVerts[i+107] = lx; localVerts[i+108] = ly; localVerts[i+109] = lz;

            localVerts[i+110] = segments[p_idx+12]; localVerts[i+111] = segments[p_idx+13]; localVerts[i+112] = segments[p_idx+14]; localVerts[i+113] = segments[p_idx+15];
            localVerts[i+114] = intensity; localVerts[i+115] = fx; localVerts[i+116] = fy; localVerts[i+117] = fz; localVerts[i+118] = lx; localVerts[i+119] = ly; localVerts[i+120] = lz;

            localVerts[i+121] = segments[p_idx+16]; localVerts[i+122] = segments[p_idx+17]; localVerts[i+123] = segments[p_idx+18]; localVerts[i+124] = segments[p_idx+19];
            localVerts[i+125] = intensity; localVerts[i+126] = fx; localVerts[i+127] = fy; localVerts[i+128] = fz; localVerts[i+129] = lx; localVerts[i+130] = ly; localVerts[i+131] = lz;

            i += 132;
            p_idx += 16;
        }

        for (; k < limit; k++) {
            int next_p_idx = p_idx + 4;

            localVerts[i++] = cx; localVerts[i++] = cy; localVerts[i++] = cc; localVerts[i++] = cs;
            localVerts[i++] = intensity; localVerts[i++] = fx; localVerts[i++] = fy; localVerts[i++] = fz; localVerts[i++] = lx; localVerts[i++] = ly; localVerts[i++] = lz;

            localVerts[i++] = segments[p_idx]; localVerts[i++] = segments[p_idx+1]; localVerts[i++] = segments[p_idx+2]; localVerts[i++] = segments[p_idx+3];
            localVerts[i++] = intensity; localVerts[i++] = fx; localVerts[i++] = fy; localVerts[i++] = fz; localVerts[i++] = lx; localVerts[i++] = ly; localVerts[i++] = lz;

            localVerts[i++] = segments[next_p_idx]; localVerts[i++] = segments[next_p_idx+1]; localVerts[i++] = segments[next_p_idx+2]; localVerts[i++] = segments[next_p_idx+3];
            localVerts[i++] = intensity; localVerts[i++] = fx; localVerts[i++] = fy; localVerts[i++] = fz; localVerts[i++] = lx; localVerts[i++] = ly; localVerts[i++] = lz;

            p_idx += 4;
        }

        int last_idx = limit * 4;

        localVerts[i++] = cx; localVerts[i++] = cy; localVerts[i++] = cc; localVerts[i++] = cs;
        localVerts[i++] = intensity; localVerts[i++] = fx; localVerts[i++] = fy; localVerts[i++] = fz; localVerts[i++] = lx; localVerts[i++] = ly; localVerts[i++] = lz;

        localVerts[i++] = segments[last_idx]; localVerts[i++] = segments[last_idx+1]; localVerts[i++] = segments[last_idx+2]; localVerts[i++] = segments[last_idx+3];
        localVerts[i++] = intensity; localVerts[i++] = fx; localVerts[i++] = fy; localVerts[i++] = fz; localVerts[i++] = lx; localVerts[i++] = ly; localVerts[i++] = lz;

        localVerts[i++] = segments[4]; localVerts[i++] = segments[5]; localVerts[i++] = segments[6]; localVerts[i++] = segments[7];
        localVerts[i++] = intensity; localVerts[i++] = fx; localVerts[i++] = fy; localVerts[i++] = fz; localVerts[i++] = lx; localVerts[i++] = ly; localVerts[i++] = lz;

        this.idx = i;
    }

    /**
     * Adds a single vertex to the buffer.
     * Public method necessary for manual triangle construction (e.g. Pseudo3D).
     */
    public void drawVertex(float x, float y, float color, float s, float intensity, float fx, float fy, float fz, float lx, float ly, float lz) {
        float[] localVerts = this.vertices;
        int i = this.idx;

        localVerts[i++] = x;
        localVerts[i++] = y;
        localVerts[i++] = color;
        localVerts[i++] = s;
        localVerts[i++] = intensity;
        localVerts[i++] = fx;
        localVerts[i++] = fy;
        localVerts[i++] = fz;
        localVerts[i++] = lx;
        localVerts[i++] = ly;
        localVerts[i++] = lz;

        this.idx = i;
    }

    public void flush() {
        if (idx == 0) return;
        renderCalls++;
        mesh.setVertices(vertices, 0, idx);
        mesh.render(shader, GL20.GL_TRIANGLES);
        idx = 0;
    }

    public int getRenderCalls() {
        return renderCalls;
    }

    public void dispose() {
        mesh.dispose();
    }
}