package games.rednblack.editor.renderer.data;

import java.util.Objects;

public class StickyNoteVO {

    public String id;
    public float x, y;
    public float width, height;
    public String content;

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
                Objects.equals(content, noteVO.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, width, height, content);
    }
}
