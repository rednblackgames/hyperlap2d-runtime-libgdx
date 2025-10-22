package games.rednblack.editor.renderer.factory.component;

import com.artemis.ComponentMapper;
import com.artemis.EntityTransmuter;
import com.artemis.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.data.LabelVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class LabelComponentFactory extends ComponentFactory {
    protected GlyphLayout glyphLayout = new GlyphLayout();
    protected ComponentMapper<LabelComponent> labelCM;

    private static int labelDefaultSize = 12;

    private final EntityTransmuter transmuter;

    public LabelComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
        transmuter = new EntityTransmuterFactory(engine)
                .add(LabelComponent.class)
                .build();
    }

    @Override
    public void transmuteEntity(int entity) {
        transmuter.transmute(entity);
    }

    @Override
    public int getEntityType() {
        return EntityFactory.LABEL_TYPE;
    }

    @Override
    public void setInitialData(int entity, Object data) {
        LabelComponent component = labelCM.get(entity);
        Object[] params = (Object[]) data;
        component.setText((String) params[0]);
        component.fontName = (String) params[1];
        component.fontSize = (int) params[2];
        component.mono = (boolean) params[3];
    }

    @Override
    public Class<LabelVO> getVOType() {
        return LabelVO.class;
    }

    @Override
    public void initializeSpecialComponentsFromVO(int entity, MainItemVO voG) {
        LabelVO vo = (LabelVO) voG;
        DimensionsComponent dimensionsComponent = dimensionsCM.get(entity);
        dimensionsComponent.height = vo.height;
        dimensionsComponent.width = vo.width;

        LabelComponent labelComponent = labelCM.get(entity);
        labelComponent.setText(vo.text);
        labelComponent.bitmapFont = vo.bitmapFont;
        if (vo.bitmapFont != null) {
            labelComponent.setStyle(generateStyle(rm, vo.bitmapFont));
        } else {
            labelComponent.setStyle(generateStyle(rm, vo.style, vo.size, vo.monoSpace));
        }
        labelComponent.fontName = vo.style;
        labelComponent.fontSize = vo.size;
        labelComponent.mono = vo.monoSpace;
        labelComponent.setAlignment(vo.align);
        labelComponent.setWrap(vo.wrap);

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        labelComponent.setFontScale(multiplier / projectInfoVO.pixelToWorld);
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        LabelComponent component = labelCM.get(entity);
        if (component.bitmapFont != null) {
            component.setStyle(generateStyle(rm, component.bitmapFont));
        } else {
            component.setStyle(generateStyle(rm, component.fontName, component.fontSize, component.mono));
        }

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.setFontScale(multiplier / projectInfoVO.pixelToWorld);
    }

    public static Label.LabelStyle generateStyle(IResourceRetriever rManager, String fontName, int size, boolean mono) {
        if (size == 0) {
            size = labelDefaultSize;
        }
        return new Label.LabelStyle(rManager.getFont(fontName, size, mono), null);
    }

    public static Label.LabelStyle generateStyle(IResourceRetriever rManager, String fontName) {
        return new Label.LabelStyle(rManager.getBitmapFont(fontName), null);
    }

    @Override
    protected void initializeDimensionsComponent(int entity) {
        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        LabelComponent labelComponent = labelCM.get(entity);
        DimensionsComponent component = dimensionsCM.get(entity);
        if (component.width == 0 && component.height == 0) {
            glyphLayout.setText(labelComponent.cache.getFont(), labelComponent.getText());
            component.width = glyphLayout.width / projectInfoVO.pixelToWorld;
            component.width += (component.width * 20) / 100;
            component.height = glyphLayout.height / projectInfoVO.pixelToWorld;
            component.height += (component.height * 40) / 100;
            glyphLayout.reset();
        }
    }
}
