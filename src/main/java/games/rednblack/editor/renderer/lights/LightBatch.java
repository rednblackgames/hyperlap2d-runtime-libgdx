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

        // Buffer check (rough estimate: vertexCount * 3 vertices per triangle)
        if (idx + (vertexCount * 3 * VERTEX_SIZE) >= vertices.length) flush();

        float cx = segments[0];
        float cy = segments[1];
        float cc = segments[2];
        float cs = segments[3];

        for (int i = 1; i < vertexCount; i++) {
            checkSpace(3);

            int p1_idx = i * 4;
            int next_idx = ((i + 1) == vertexCount) ? 4 : (i + 1) * 4;

            // Center
            putVertex(cx, cy, cc, cs, intensity, fx, fy, fz, lx, ly, lz);
            // P1
            putVertex(segments[p1_idx], segments[p1_idx+1], segments[p1_idx+2], segments[p1_idx+3], intensity, fx, fy, fz, lx, ly, lz);

            // P2 (handling pointlight loop closure)
            if (i == vertexCount - 1) {
                putVertex(segments[4], segments[5], segments[6], segments[7], intensity, fx, fy, fz, lx, ly, lz);
            } else {
                putVertex(segments[next_idx], segments[next_idx+1], segments[next_idx+2], segments[next_idx+3], intensity, fx, fy, fz, lx, ly, lz);
            }
        }
    }

    /** * Generic method to draw triangles (used for Pseudo3D shadows and generic geometry)
     * verticesInput must be a multiple of 4 floats (x,y,col,s)
     */
    public void drawVerts(float[] verticesInput, int offset, int count, float intensity, float fx, float fy, float fz, float lx, float ly, float lz) {
        int numVerts = count / 4;
        if (idx + (numVerts * VERTEX_SIZE) >= vertices.length) flush();

        for (int i = 0; i < count; i+=4) {
            putVertex(verticesInput[offset+i], verticesInput[offset+i+1], verticesInput[offset+i+2], verticesInput[offset+i+3], intensity, fx, fy, fz, lx, ly, lz);
        }
    }

    /**
     * Adds a single vertex to the buffer.
     * Public method necessary for manual triangle construction (e.g. Pseudo3D).
     */
    public void drawVertex(float x, float y, float color, float s, float intensity, float fx, float fy, float fz, float lx, float ly, float lz) {
        if (idx + VERTEX_SIZE >= vertices.length) flush();
        putVertex(x, y, color, s, intensity, fx, fy, fz, lx, ly, lz);
    }

    private void putVertex(float x, float y, float color, float s, float i, float fx, float fy, float fz, float lx, float ly, float lz) {
        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = s;
        vertices[idx++] = i;
        vertices[idx++] = fx;
        vertices[idx++] = fy;
        vertices[idx++] = fz;
        vertices[idx++] = lx;
        vertices[idx++] = ly;
        vertices[idx++] = lz;
    }

    public void flush() {
        if (idx == 0) return;
        renderCalls++;
        mesh.setVertices(vertices, 0, idx);
        mesh.render(shader, GL20.GL_TRIANGLES);
        idx = 0;
    }

    public void dispose() {
        mesh.dispose();
    }
}