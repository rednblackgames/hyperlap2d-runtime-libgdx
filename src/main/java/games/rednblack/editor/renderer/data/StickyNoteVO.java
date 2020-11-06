package games.rednblack.editor.renderer.data;

import java.util.Arrays;
import java.util.Objects;

public class StickyNoteVO {

    public String id;
    public float x, y;
    public float width, height;
    public String content;
    public float[] tint = {0.72f, 0.98f, 0.62f, 1};

    public StickyNoteVO() {
        width = 250;
        height = 100;
        content = "";
    }

    public StickyNoteVO(StickyNoteVO noteVO) {
        this.content = noteVO.content;
        this.height = noteVO.height;
        this.width = noteVO.width;
        this.x = noteVO.x;
        this.y = noteVO.y;
        this.id = noteVO.id;
        if(noteVO.tint != null) tint = Arrays.copyOf(noteVO.tint, noteVO.tint.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickyNoteVO noteVO = (StickyNoteVO) o;
        return Float.compare(noteVO.x, x) == 0 &&
                Float.compare(noteVO.y, y) == 0 &&
                Float.compare(noteVO.width, width) == 0 &&
                Float.compare(noteVO.height, height) == 0 &&
                Objects.equals(id, noteVO.id) &&
                Objects.equals(content, noteVO.content) &&
                Arrays.equals(tint, noteVO.tint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, width, height, content);
    }
}
