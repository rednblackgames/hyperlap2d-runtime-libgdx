package games.rednblack.editor.renderer.lights.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class DiffuseLightShader {
    static final public ShaderProgram createShader() {
        final String vertexShader =
                "attribute vec4 a_position;\n" +
                        "attribute vec2 a_texCoord;\n" +
                        "varying vec2 v_texCoords;\n" +
                        "void main()\n" +
                        "{\n" +
                        "   v_texCoords = a_texCoord;\n" +
                        "   gl_Position = a_position;\n" +
                        "}\n";

        final String fragmentShader = "#ifdef GL_ES\n" +
                "precision lowp float;\n" +
                "#define MED mediump\n" +
                "#else\n" +
                "#define MED \n" +
                "#endif\n" +
                "varying MED vec2 v_texCoords;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform vec4 ambient;\n" +

                "void main()\n" +
                "{\n" +
                "    vec4 c = texture2D(u_texture, v_texCoords);\n" +
                "    gl_FragColor.rgb = (ambient.rgb + c.rgb);\n" +
                "    gl_FragColor.a = 1.0;\n" +
                "}\n";

        ShaderProgram.pedantic = false;
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            shader = new ShaderProgram("#version 330 core\n" +vertexShader,
                    "#version 330 core\n" +fragmentShader);
            if(!shader.isCompiled()){
                Gdx.app.log("LightShader", "Error: " + shader.getLog());
            }
        }
        return shader;
    }
}
