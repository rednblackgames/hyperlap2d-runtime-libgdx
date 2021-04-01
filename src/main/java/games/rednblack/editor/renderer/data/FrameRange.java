package games.rednblack.editor.renderer.data;

import java.util.Objects;

/**
 * Created by CyberJoe on 6/18/2015.
 */
public class FrameRange {
    public String name;
    public int startFrame;
    public int endFrame;

    public FrameRange() {

    }

    public FrameRange(String name, int startFrame, int endFrame) {
        this.name = name;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrameRange that = (FrameRange) o;
        return startFrame == that.startFrame && endFrame == that.endFrame && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, startFrame, endFrame);
    }
}
