package games.rednblack.editor.renderer.lights.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import games.rednblack.editor.renderer.lights.RayHandler;

public final class LightShader {
    public static ShaderProgram createLightShader() {
        String gamma = RayHandler.getGammaCorrection() ? "sqrt" : "";

        final String vertexShader =
                "attribute vec4 a_position;\n" +
                        "attribute vec4 a_color;\n" +
                        "attribute float a_s;\n" +
                        "attribute float a_intensity;\n" +
                        "attribute vec3 a_falloff;\n" +
                        "attribute vec3 a_lightPos;\n" +

                        "uniform mat4 u_projTrans;\n" +

                        "varying vec4 v_color;\n" +
                        "varying float v_s;\n" +
                        "varying float v_intensity;\n" +
                        "varying vec3 v_falloff;\n" +
                        "varying vec3 v_lightPos;\n" +
                        "varying vec2 v_worldPos;\n" +

                        "void main()\n" +
                        "{\n" +
                        "   v_color = a_color;\n" +
                        "   v_s = a_s;\n" +
                        "   v_intensity = a_intensity;\n" +
                        "   v_falloff = a_falloff;\n" +
                        "   v_lightPos = a_lightPos;\n" +
                        "   v_worldPos = a_position.xy;\n" + // Save world coord before projection
                        "   gl_Position = u_projTrans * a_position;\n" +
                        "}\n";

        final String fragmentShader = "#ifdef GL_ES\n" +
                "precision highp float;\n" +
                "#endif\n" +

                "varying vec4 v_color;\n" +
                "varying float v_s;\n" +
                "varying float v_intensity;\n" +
                "varying vec3 v_falloff;\n" +
                "varying vec3 v_lightPos;\n" +
                "varying vec2 v_worldPos;\n" +

                "uniform sampler2D u_normals;\n" +
                "uniform vec2 u_resolution;\n" +
                "uniform int u_useNormals;\n" +

                "void main()\n" +
                "{\n" +
                // 1. Attenuation (Softer light: Quadratic)
                "  float Attenuation = 1.0 / (v_falloff.x + (v_falloff.y * v_s) + (v_falloff.z * v_s * v_s));\n" +

                // 2. Normal Map Logic
                "  float normalEffect = 1.0;\n" +
                "  if (u_useNormals == 1) {\n" +
                "      vec2 screenUV = gl_FragCoord.xy / u_resolution;\n" +
                "      vec4 NormalSample = texture2D(u_normals, screenUV);\n" +

                //     If there is a normal map (Alpha > 0)...
                "      if (NormalSample.a > 0.5) {\n" +
                "          vec3 Normal = NormalSample.rgb * 2.0 - 1.0;\n" +
                "          Normal.y = -Normal.y;\n" +
                "          Normal = normalize(Normal);\n" +
                "          vec3 LightDir = normalize(vec3(v_lightPos.xy - v_worldPos, v_lightPos.z));\n" +
                "          float NdotL = dot(Normal, LightDir);\n" +
                "          normalEffect = (NdotL * 0.6) + 0.4;\n" +
                "      }\n" +
                "  }\n" +

                // Add a final multiplier (1.2) to slightly "overexpose" and make colors vivid
                "  gl_FragColor = " + gamma + "(v_color * v_intensity * normalEffect * 1.2) * Attenuation;\n" +
                "}";

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
