package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderCompiler {
    public static boolean FORCE_UNROLLED = true;
    public static int MAX_TEXTURE_UNIT = 1;

    public static final String GET_TEXTURE_FROM_ARRAY_PLACEHOLDER = "<GET_TEXTURE_FROM_ARRAY_PLACEHOLDER>";

    public static ShaderProgram compileShader(FileHandle vertex, FileHandle fragment) {
        return compileShader(vertex.readString(), fragment.readString());
    }

    public static ShaderProgram compileShader(String vertex, String fragment) {
        if (!fragment.contains(GET_TEXTURE_FROM_ARRAY_PLACEHOLDER)) return new ShaderProgram(vertex, fragment);

        String version = Gdx.graphics.getGL20().glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        if (!FORCE_UNROLLED && (version.contains("ES 3.20") || version.contains("4.6"))) {
            return compileArrayTextureShader(vertex, fragment);
        }
        return compileUnrolledArrayTextureShader(vertex, fragment);
    }

    public static ShaderProgram compileArrayTextureShader(String vertex, String fragment) {
        String funcConditional = "vec4 getTextureFromArray(vec2 uv) {\n"
                + "  int index = int(v_texture_index);\n"
                + "  return texture2D(u_textures[index], uv);\n"
                + "}";

        fragment = "#define MAX_TEXTURE_UNITS " + MAX_TEXTURE_UNIT + "\n" + fragment;
        fragment = fragment.replace(GET_TEXTURE_FROM_ARRAY_PLACEHOLDER, funcConditional);
        return new ShaderProgram(vertex, fragment);
    }

    public static ShaderProgram compileUnrolledArrayTextureShader(String vertex, String fragment) {
        String funcConditional = "vec4 getTextureFromArray(vec2 uv) {\n" +
                "  int index = int(v_texture_index);";
        for (int i = 0; i < MAX_TEXTURE_UNIT; i++) {
            if (i != 0) funcConditional += " else ";
            funcConditional += "if (index == " + i + ") return texture2D(u_textures[" + i + "], uv);\n";
        }
        funcConditional += "}\n";

        fragment = "#define MAX_TEXTURE_UNITS " + MAX_TEXTURE_UNIT + "\n" + fragment;
        fragment = fragment.replace(GET_TEXTURE_FROM_ARRAY_PLACEHOLDER, funcConditional);
        return new ShaderProgram(vertex, fragment);
    }
}
