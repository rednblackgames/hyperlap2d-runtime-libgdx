package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public interface ShaderUniformProvider {
    void applyUniforms(String shaderName, ShaderProgram shaderProgram);
}
