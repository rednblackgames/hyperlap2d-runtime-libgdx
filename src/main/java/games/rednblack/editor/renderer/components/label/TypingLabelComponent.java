package games.rednblack.editor.renderer.components.label;

import com.artemis.PooledComponent;
import com.rafaskoberg.gdx.typinglabel.TypingLabel;
import games.rednblack.editor.renderer.components.RemovableComponent;

public class TypingLabelComponent extends PooledComponent {

    public TypingLabel typingLabel;

    @Override
    public void reset() {
        if (typingLabel != null)
            typingLabel.remove();
        typingLabel = null;
    }

    // TODO: onRemove thingy
    public void onRemove() {
        reset();
    }
}
