package games.rednblack.editor.renderer.components;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import games.rednblack.editor.renderer.data.ShaderUniformVO;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderComponent implements BaseComponent {
	public String shaderName = "";
	private ShaderProgram shaderProgram = null;

	private final Pattern pattern = Pattern.compile("uniform +(int|float|vec2|vec3|vec4) +([^\\s]*) *(=.*)?;", Pattern.MULTILINE);

	//Map that stores uniforms' name with their types
	public final HashMap<String, String> uniforms = new HashMap<>();

	public final HashMap<String, ShaderUniformVO> customUniforms = new HashMap<>();

	public void setShader(String name, ShaderProgram program) {
		shaderName = name;
		shaderProgram = program;

		uniforms.clear();
		Matcher matcher = pattern.matcher(shaderProgram.getFragmentShaderSource());
		while (matcher.find()) {
			uniforms.put(matcher.group(2), matcher.group(1));
		}
	}

	public ShaderProgram getShader() {
		return shaderProgram;
	}

	public void clear() {
		shaderName = "";
		shaderProgram = null;

		uniforms.clear();
		customUniforms.clear();
	}

	@Override
	public void reset() {
		clear();
	}
}
