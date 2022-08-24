package games.rednblack.editor.renderer.box2dLight.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import games.rednblack.editor.renderer.box2dLight.RayHandler;

public final class LightShader {
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
				+ "varying float D;\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = s * quad_colors;\n" //
				+ "   D = s;\n" //
				+ "   gl_Position =  u_projTrans * vertex_positions;\n" //
				+ "}\n";
		final String fragmentShader = "#ifdef GL_ES\n" //
			+ "precision lowp float;\n" //
			+ "#define MED mediump\n"
			+ "#else\n"
			+ "#define MED \n"
			+ "#endif\n" //
				+ "varying vec4 v_color;\n" //
				+ "varying float D;\n" //
				+ "uniform float u_intensity;\n" //
				+ "uniform vec3 u_falloff;"
				+ "void main()\n"//
				+ "{\n" //
				+ "  float Attenuation = 1.0 / (u_falloff.x + (u_falloff.y*D) + (u_falloff.z*D*D));\n"
				+ "  gl_FragColor = "+gamma+"(v_color * u_intensity) * Attenuation;\n" //
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
