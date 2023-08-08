package games.rednblack.editor.renderer.systems.action.data.physics;

import games.rednblack.editor.renderer.systems.action.data.RelativeTemporalData;

public class TransformByData extends RelativeTemporalData {
    public float amountX;
    public float amountY;
    public float amountAngle;

    public void setAmountX(float amountX) {
        this.amountX = amountX;
    }

    public void setAmountY(float amountY) {
        this.amountY = amountY;
    }

    public void setAmountAngle(float amountAngle) {
        this.amountAngle = amountAngle;
    }

    @Override
    public void reset() {
        super.reset();

        amountX = 0;
        amountY = 0;
        amountAngle = 0;
    }
}
