package games.rednblack.editor.renderer.systems.action.data.physics;

import games.rednblack.editor.renderer.systems.action.data.TemporalData;

public class TransformToData extends TemporalData {

    public float startX;
    public float startY;
    public float startAngle;
    public float endX;
    public float endY;
    public float endAngle;

    public void setEndX(float endX) {
        this.endX = endX;
    }

    public void setEndY(float endY) {
        this.endY = endY;
    }

    public void setEndAngle(float endAngle) {
        this.endAngle = endAngle;
    }

    @Override
    public void reset() {
        super.reset();

        startX = 0;
        startY = 0;
        startAngle = 0;
        endX = 0;
        endY = 0;
        endAngle = 0;
    }
}
