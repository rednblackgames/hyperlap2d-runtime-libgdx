package games.rednblack.editor.renderer.data;

public class ShaderUniformVO {
    protected String type;

    public int intValue;

    public float floatValue;

    public float floatValue2;
    public float floatValue3;
    public float floatValue4;

    public ShaderUniformVO() {
        type = null;
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
        floatValue3 = floatVal4;
    }

    public String getType() {
        return type;
    }

    private String checkType(String newType) {
        if (type != null && !type.equals(newType))
            throw new IllegalArgumentException("Cannot modify Uniform type once assigned.");
        return newType;
    }
}
