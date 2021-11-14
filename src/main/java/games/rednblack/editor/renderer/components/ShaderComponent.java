package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ShaderUniformVO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderComponent  extends PooledComponent {
	public MainItemVO.RenderingLayer renderingLayer;
	public String shaderName = "";
	private transient ShaderProgram shaderProgram = null;

	private transient final Pattern pattern = Pattern.compile("uniform +(int|float|vec2|vec3|vec4) +([^\\s]*) *(=.*)?;", Pattern.MULTILINE);

	//Map that stores uniforms' name with their types
	public final ObjectMap<String, String> uniforms = new ObjectMap<>();

	public final ObjectMap<String, ShaderUniformVO> customUniforms = new ObjectMap<>();

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
