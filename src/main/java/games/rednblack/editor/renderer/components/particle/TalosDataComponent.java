package games.rednblack.editor.renderer.components.particle;

import games.rednblack.editor.renderer.components.BaseComponent;

public class TalosDataComponent implements BaseComponent {

    public String particleName = "";
    public boolean transform = true;

    @Override
    public void reset() {
        particleName = "";
        transform = true;
    }
}
