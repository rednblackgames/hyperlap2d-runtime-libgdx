package games.rednblack.editor.renderer.components;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;

public class TransformComponent implements BaseComponent {
    public Affine2 worldTransform = new Affine2();
    public Matrix4 computedTransform = new Matrix4();
    public Matrix4 oldTransform = new Matrix4();

    public float x;
    public float y;
    public float scaleX = 1f;
    public float scaleY = 1f;
    public float rotation;
    public float originX;
    public float originY;
    public boolean flipX = false;
    public boolean flipY = false;

    private TransformComponent backup = null;

    public TransformComponent() {

    }

    public TransformComponent(TransformComponent component) {
        x = component.x;
        y = component.y;
        scaleX = component.scaleX;
        scaleY = component.scaleY;
        rotation = component.rotation;
        originX = component.originX;
        originY = component.originY;
        flipX = component.flipX;
        flipY = component.flipY;

        worldTransform.set(component.worldTransform);
        computedTransform.set(component.computedTransform);
        oldTransform.set(component.oldTransform);

        backup = null;
    }

    public void disableTransform() {
        backup = new TransformComponent(this);
        x = 0;
        y = 0;
        scaleX = 1f;
        scaleY = 1f;
        rotation = 0;
        flipX = false;
        flipY = false;
    }

    public void enableTransform() {
        if (backup == null) return;
        x = backup.x;
        y = backup.y;
        scaleX = backup.scaleX;
        scaleY = backup.scaleY;
        rotation = backup.rotation;
        originX = backup.originX;
        originY = backup.originY;
        flipX = backup.flipX;
        flipY = backup.flipY;
        backup = null;
    }

    public boolean shouldTransform() {
        return (rotation != 0 || scaleX != 1 || scaleY != 1 || flipY || flipX);
    }

    public TransformComponent getRealComponent() {
        if (backup != null)
            return backup;
        return this;
    }

    @Override
    public void reset() {
        x = 0;
        y = 0;
        scaleX = 1f;
        scaleY = 1f;
        rotation = 0;
        originX = 0;
        originY = 0;
        flipX = false;
        flipY = false;

        worldTransform.idt();
        computedTransform.idt();
        oldTransform.idt();

        backup = null;
    }
}
