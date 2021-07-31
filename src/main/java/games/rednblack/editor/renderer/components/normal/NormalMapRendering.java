package games.rednblack.editor.renderer.components.normal;

import games.rednblack.editor.renderer.components.BaseComponent;
import games.rednblack.editor.renderer.utils.value.DynamicValue;

public class NormalMapRendering implements BaseComponent, DynamicValue<Boolean> {
    public DynamicValue<Boolean> useNormalMap;

    @Override
    public void reset() {
        useNormalMap = null;
    }

    @Override
    public Boolean get() {
        return useNormalMap != null && useNormalMap.get();
    }
}
