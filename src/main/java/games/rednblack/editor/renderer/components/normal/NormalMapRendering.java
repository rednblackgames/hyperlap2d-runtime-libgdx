package games.rednblack.editor.renderer.components.normal;

import com.artemis.PooledComponent;
import games.rednblack.editor.renderer.utils.value.DynamicValue;

public class NormalMapRendering extends PooledComponent implements DynamicValue<Boolean> {
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