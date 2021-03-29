package games.rednblack.editor.renderer.data;

public class GraphConnectionVO implements Comparable<GraphConnectionVO> {
    public String fromNode;
    public String fromField;
    public String toNode;
    public String toField;

    @Override
    public int compareTo(GraphConnectionVO o) {
        return toField.compareTo(o.toField);
    }
}
