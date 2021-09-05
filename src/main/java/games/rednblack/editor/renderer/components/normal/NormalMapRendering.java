package games.rednblack.editor.renderer.components.normal;

import com.artemis.PooledComponent;
import games.rednblack.editor.renderer.utils.value.DynamicValue;

public class NormalMapRendering extends PooledComponent implements DynamicValue<Boolean> {
    public Boolean useNormalMap = Boolean.FALSE;

    @Override
    public void reset() {
        useNormalMap = Boolean.FALSE;
    }

    @Override
    public Boolean get() {
        return useNormalMap;
    }
}