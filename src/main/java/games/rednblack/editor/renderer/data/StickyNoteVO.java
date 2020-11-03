package games.rednblack.editor.renderer.data;

public class StickyNoteVO {

    public String id;
    public float x, y;
    public float width, height;
    public String content;

    public StickyNoteVO() {

    }

    public StickyNoteVO(StickyNoteVO noteVO) {
        this.content = noteVO.content;
        this.height = noteVO.height;
        this.width = noteVO.width;
        this.x = noteVO.x;
        this.y = noteVO.y;
        this.id = noteVO.id;
    }
}
