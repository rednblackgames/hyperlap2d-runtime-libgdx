package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pools;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ShaderUniformVO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderComponent  extends PooledComponent {
	public static int UNIFORMS_POOL_SIZE = 100;

	public MainItemVO.RenderingLayer renderingLayer = MainItemVO.RenderingLayer.SCREEN;
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
		matcher = pattern.matcher(shaderProgram.getVertexShaderSource());
		while (matcher.find()) {
			uniforms.put(matcher.group(2), matcher.group(1));
		}

		for (String uniformName : customUniforms.keys()) {
			if (!uniforms.containsKey(uniformName)) {
				ShaderUniformVO vo = customUniforms.remove(uniformName);
				Pools.free(vo);
			}
		}
	}

	public ShaderProgram getShader() {
		return shaderProgram;
	}

	public void clear() {
		shaderName = "";
		shaderProgram = null;
		renderingLayer = MainItemVO.RenderingLayer.SCREEN;

		uniforms.clear();
		for (ShaderUniformVO vo : customUniforms.values()) {
			Pools.free(vo);
		}
		customUniforms.clear();
	}

	@Override
	public void reset() {
		clear();
	}
}
