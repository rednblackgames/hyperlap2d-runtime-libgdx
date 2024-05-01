package games.rednblack.editor.renderer.data;

public class ResolutionEntryVO {

    public String name = "";

    public int width;
    public int height;
    public int base;

    private int lastW, lastH;
    private String fullName;

    @Override
    public String toString() {
        if (width == 0 && height == 0) {
            return name;
        }
        if (width != lastW || height != lastH) {
            lastW = width;
            lastH = height;
            fullName = name + " (" + width + "x" + height + ")";
        }
        return fullName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResolutionEntryVO other = (ResolutionEntryVO) obj;
        return other.name.equals(name);
    }

    public float getMultiplier(ResolutionEntryVO originalResolution) {
        float mul;
        if(base == 0) {
            mul = (float)originalResolution.width/width;
        } else {
            mul = (float)originalResolution.height/height;
        }
        return mul;
    }
}
