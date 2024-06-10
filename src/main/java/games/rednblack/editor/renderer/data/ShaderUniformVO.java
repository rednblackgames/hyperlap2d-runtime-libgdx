package games.rednblack.editor.renderer.data;

import com.badlogic.gdx.utils.Pool;

public class ShaderUniformVO implements Pool.Poolable {
    protected String type;

    public int intValue;

    public float floatValue;

    public float floatValue2;
    public float floatValue3;
    public float floatValue4;

    public ShaderUniformVO() {
        type = null;
    }

    public ShaderUniformVO(ShaderUniformVO vo) {
        type = vo.type;

        intValue = vo.intValue;

        floatValue = vo.floatValue;
        floatValue2 = vo.floatValue2;
        floatValue3 = vo.floatValue3;
        floatValue4 = vo.floatValue4;
    }

    public void set(ShaderUniformVO vo) {
        type = vo.type;

        intValue = vo.intValue;

        floatValue = vo.floatValue;
        floatValue2 = vo.floatValue2;
        floatValue3 = vo.floatValue3;
        floatValue4 = vo.floatValue4;
    }

    public void set(int intVal) {
        type = checkType("int");
        intValue = intVal;
    }

    public void set(float floatVal) {
        type = checkType("float");
        floatValue = floatVal;
    }

    public void set(float floatVal, float floatVal2) {
        type = checkType("vec2");
        floatValue = floatVal;
        floatValue2 = floatVal2;
    }

    public void set(float floatVal, float floatVal2, float floatVal3) {
        type = checkType("vec3");
        floatValue = floatVal;
        floatValue2 = floatVal2;
        floatValue3 = floatVal3;
    }

    public void set(float floatVal, float floatVal2, float floatVal3, float floatVal4) {
        type = checkType("vec4");
        floatValue = floatVal;
        floatValue2 = floatVal2;
        floatValue3 = floatVal3;
        floatValue4 = floatVal4;
    }

    public String getType() {
        return type;
    }

    private String checkType(String newType) {
        if (type != null && !type.equals(newType))
            throw new IllegalArgumentException("Cannot modify Uniform type once assigned.");
        return newType;
    }

    @Override
    public void reset() {
        type = null;
        intValue = 0;

        floatValue = 0;
        floatValue2 = 0;
        floatValue3 = 0;
        floatValue4 = 0;
    }
}
