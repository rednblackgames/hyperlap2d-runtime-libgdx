package games.rednblack.editor.renderer.lights.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import games.rednblack.editor.renderer.lights.RayHandler;

public class OnePassGaussian {

    public static ShaderProgram createBlurShader(int width, int heigth) {
        final String FBO_W = Integer.toString(width);
        final String FBO_H = Integer.toString(heigth);
        final String rgb = RayHandler.isDiffuseLight() ? ".rgb" : "";

        // VERTEX SHADER
        final String vertexShader = "attribute vec4 a_position;\n"
                + "attribute vec2 a_texCoord;\n"
                + "varying vec2 v_texCoords0;\n"
                + "varying vec2 v_texCoords1;\n"
                + "varying vec2 v_texCoords2;\n"
                + "varying vec2 v_texCoords3;\n"
                + "varying vec2 v_texCoords4;\n"
                + "#define FBO_W " + FBO_W + ".0\n"
                + "#define FBO_H " + FBO_H + ".0\n"
                + "const float offset = 1.4;\n"
                + "void main()\n"
                + "{\n"
                + "   vec2 xOff = vec2(offset / FBO_W, 0.0);\n"
                + "   vec2 yOff = vec2(0.0, offset / FBO_H);\n"
                + "   v_texCoords0 = a_texCoord;\n"
                + "   v_texCoords1 = a_texCoord - xOff;\n"
                + "   v_texCoords2 = a_texCoord + xOff;\n"
                + "   v_texCoords3 = a_texCoord - yOff;\n"
                + "   v_texCoords4 = a_texCoord + yOff;\n"
                + "   gl_Position = a_position;\n"
                + "}\n";

        // FRAGMENT SHADER
        final String fragmentShader = "#ifdef GL_ES\n"
                + "precision lowp float;\n"
                + "#define MED mediump\n"
                + "#else\n"
                + "#define MED \n"
                + "#endif\n"
                + "uniform sampler2D u_texture;\n"
                + "varying MED vec2 v_texCoords0;\n"
                + "varying MED vec2 v_texCoords1;\n"
                + "varying MED vec2 v_texCoords2;\n"
                + "varying MED vec2 v_texCoords3;\n"
                + "varying MED vec2 v_texCoords4;\n"
                + "const float w_center = 0.30;\n"
                + "const float w_side   = 0.175;\n"
                + "void main()\n"
                + "{    \n"
                + "gl_FragColor"+rgb+" = w_center * texture2D(u_texture, v_texCoords0)"+rgb+"\n"
                + "              + w_side   * texture2D(u_texture, v_texCoords1)"+rgb+"\n"
                + "              + w_side   * texture2D(u_texture, v_texCoords2)"+rgb+"\n"
                + "              + w_side   * texture2D(u_texture, v_texCoords3)"+rgb+"\n"
                + "              + w_side   * texture2D(u_texture, v_texCoords4)"+rgb+";\n"
                + "}\n";

        ShaderProgram.pedantic = false;
        ShaderProgram blurShader = new ShaderProgram(vertexShader, fragmentShader);
        if (!blurShader.isCompiled()) {
            blurShader = new ShaderProgram("#version 330 core\n" + vertexShader,
                    "#version 330 core\n" + fragmentShader);
            if (!blurShader.isCompiled()) {
                Gdx.app.log("ERROR", blurShader.getLog());
            }
        }

        return blurShader;
    }
}