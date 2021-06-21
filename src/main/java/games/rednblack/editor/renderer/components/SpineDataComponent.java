package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;

public class SpineDataComponent extends PooledComponent {
    public String animationName = "";
    public String currentAnimationName = "";

    @Override
    public void reset() {
        animationName = "";
        currentAnimationName = "";
    }
}
