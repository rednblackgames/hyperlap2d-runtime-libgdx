package games.rednblack.editor.renderer.box2dLight.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import games.rednblack.editor.renderer.box2dLight.RayHandler;

/**
 * Shader code adapted from https://github.com/mattdesl/lwjgl-basics/wiki/ShaderLesson6
 */
public class LightWithNormalMapShader {
    static final public ShaderProgram createLightShader() {
        String gamma = "";
        if (RayHandler.getGammaCorrection())
            gamma = "sqrt";

        final String vertexShader =
                "attribute vec4 vertex_positions;\n" //
                        + "attribute vec4 quad_colors;\n" //
                        + "attribute float s;\n"
                        + "uniform mat4 u_projTrans;\n" //
                        + "varying vec4 v_color;\n" //
                        + "void main()\n" //
                        + "{\n" //
                        + "   v_color = s * quad_colors;\n" //
                        + "   gl_Position =  u_projTrans * vertex_positions;\n" //
                        + "}\n";
        final String fragmentShader = "#ifdef GL_ES\n" //
                + "precision lowp float;\n" //
                + "#define MED mediump\n"
                + "#else\n"
                + "#define MED \n"
                + "#endif\n" //
                + "varying vec4 v_color;\n" //
                + "uniform sampler2D u_normals;\n" //
                + "uniform vec3 u_lightpos;\n" //
                + "uniform vec2 u_resolution;\n" //
                + "uniform float u_intensity;\n" //
                + "uniform vec3 u_falloff;"
                + "void main()\n"//
                + "{\n" //
                + "  vec2 screenPos = gl_FragCoord.xy / u_resolution.xy;\n"
                + "  vec4 NormalMapTexture = texture2D(u_normals, screenPos);\n"
                + "  vec3 NormalMap = NormalMapTexture.rgb;\n"
                + "  float alpha = NormalMapTexture.a;\n"
                + "  vec3 LightDir = vec3(u_lightpos.xy - screenPos, u_lightpos.z);\n"

                + "  LightDir.x *= u_resolution.x / u_resolution.y;\n"
                + "  float D = length(LightDir);\n"
                + "  float Attenuation = 1.0 / (u_falloff.x + (u_falloff.y*D) + (u_falloff.z*D*D));\n"

                + "  vec3 N = normalize(NormalMap * 2.0 - 1.0);\n"
                + "  vec3 L = normalize(LightDir);\n"
                + "  float maxProd = (max(dot(N, L), 0.0) * Attenuation - 1.0) * alpha + 1.0;\n"
                + "  gl_FragColor = "+gamma+"(v_color * maxProd * u_intensity);\n" //
                + "}";

        ShaderProgram.pedantic = false;
        ShaderProgram lightShader = new ShaderProgram(vertexShader,
                fragmentShader);
        if (!lightShader.isCompiled()) {
            lightShader = new ShaderProgram("#version 330 core\n" +vertexShader,
                    "#version 330 core\n" +fragmentShader);
            if(!lightShader.isCompiled()){
                Gdx.app.log("ERROR", lightShader.getLog());
            }
        }

        return lightShader;
    }
}
