package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.ObjectMap;

public class ShaderVO {
    public String shaderName = "";
    public ObjectMap<String, ShaderUniformVO> shaderUniforms = new ObjectMap<>(0);

    public ShaderVO(){

    }

    public ShaderVO(ShaderVO vo) {
        set(vo);
    }

    public void set(ShaderVO vo) {
        shaderName = vo.shaderName;
        for (String uniformName : vo.shaderUniforms.keys()) {
            shaderUniforms.put(uniformName, new ShaderUniformVO(vo.shaderUniforms.get(uniformName)));
        }
    }
}
