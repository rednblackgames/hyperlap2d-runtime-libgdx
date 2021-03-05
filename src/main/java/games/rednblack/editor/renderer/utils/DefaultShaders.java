package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class DefaultShaders {
    public static String DEFAULT_VERTEX_SHADER = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
            + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
            + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
            + "uniform mat4 u_projTrans;\n" //
            + "varying vec4 v_color;\n" //
            + "varying vec2 v_texCoords;\n" //
            + "\n" //
            + "void main()\n" //
            + "{\n" //
            + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
            + "   v_color.a = v_color.a * (255.0/254.0);\n" //
            + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
            + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
            + "}";
    public static String DEFAULT_FRAGMENT_SHADER = "#ifdef GL_ES\n" //
            + "#define LOWP lowp\n" //
            + "precision mediump float;\n" //
            + "#else\n" //
            + "#define LOWP \n" //
            + "#endif\n" //
            + "varying LOWP vec4 v_color;\n" //
            + "varying vec2 v_texCoords;\n" //
            + "uniform sampler2D u_texture;\n" //
            + "uniform int wtf;\n" //
            + "void main()\n"//
            + "{\n" //
            //TODO the most weird bug I've ever found!
            //+ "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n"
            + "  if (wtf == 0) { gl_FragColor = v_color * texture2D(u_texture, v_texCoords); } else { gl_FragColor = vec4(0.0); }\n" //
            + "}";
}
