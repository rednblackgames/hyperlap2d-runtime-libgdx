package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Compiles shaders that read from the batch's texture array. Their {@code getTextureFromArray} is
 * unrolled into one branch per texture unit, so the unit count is part of the source: it is asked
 * of the batch at compile time, never assumed. A shader compiled before any batch exists — the
 * project's own, loaded with the assets — must still be able to read every unit the batch will
 * bind, or a region on any atlas page but the first draws as nothing.
 */
public class ShaderCompiler {
    public static final String GET_TEXTURE_FROM_ARRAY_PLACEHOLDER = "<GET_TEXTURE_FROM_ARRAY_PLACEHOLDER>";

    public static ShaderProgram compileShader(FileHandle vertex, FileHandle fragment) {
        return compileShader(vertex.readString(), fragment.readString());
    }

    public static ShaderProgram compileShader(String vertex, String fragment) {
        if (!fragment.contains(GET_TEXTURE_FROM_ARRAY_PLACEHOLDER)) return new ShaderProgram(vertex, fragment);

        return compileUnrolledArrayTextureShader(vertex, fragment);
    }

    /** For as many units as the batch takes on this device. */
    public static ShaderProgram compileUnrolledArrayTextureShader(String vertex, String fragment) {
        return compileUnrolledArrayTextureShader(vertex, fragment, TextureArrayPolygonSpriteBatch.getMaxTextureUnits());
    }

    /** With an explicit unit count: for the batch's own probe of how many units the device takes. */
    public static ShaderProgram compileUnrolledArrayTextureShader(String vertex, String fragment, int textureUnits) {
        return new ShaderProgram(vertex, unrollArrayTexture(fragment, textureUnits));
    }

    /** The fragment source with {@code MAX_TEXTURE_UNITS} defined and {@code getTextureFromArray} unrolled. */
    static String unrollArrayTexture(String fragment, int textureUnits) {
        StringBuilder funcConditional = new StringBuilder("vec4 getTextureFromArray(vec2 uv) {\n");
        for (int i = 0; i < textureUnits; i++) {
            if (i != 0) funcConditional.append(" else ");
            funcConditional.append("if (v_texture_index < ").append(i).append(".5) return texture2D(u_textures[").append(i).append("], uv);\n");
        }
        funcConditional.append("}\n");

        return "#define MAX_TEXTURE_UNITS " + textureUnits + "\n"
                + fragment.replace(GET_TEXTURE_FROM_ARRAY_PLACEHOLDER, funcConditional.toString());
    }
}
