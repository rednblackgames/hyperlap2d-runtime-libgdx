package games.rednblack.editor.renderer.factory.component;

import com.badlogic.gdx.graphics.Color;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EntityTransmuter;
import games.rednblack.editor.renderer.ecs.EntityTransmuterFactory;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import games.rednblack.editor.renderer.lights.RayHandler;
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

    public LabelComponentFactory(Engine engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
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
        labelComponent.fontName = vo.style;
        labelComponent.fontSize = vo.size;
        labelComponent.mono = vo.monoSpace;
        setBorder(labelComponent, vo);
        labelComponent.setStyle(generateStyle(rm, labelComponent));
        labelComponent.setAlignment(vo.align);
        labelComponent.setWrap(vo.wrap);
        labelComponent.fitWidth = vo.fitWidth;
        labelComponent.fitHeight = vo.fitHeight;

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        labelComponent.setFontScale(multiplier / projectInfoVO.pixelToWorld);
    }

    @Override
    protected void initializeTransientComponents(int entity) {
        super.initializeTransientComponents(entity);

        LabelComponent component = labelCM.get(entity);
        component.setStyle(generateStyle(rm, component));

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.setFontScale(multiplier / projectInfoVO.pixelToWorld);
    }

    public static Label.LabelStyle generateStyle(IResourceRetriever rManager, String fontName, int size, boolean mono) {
        return generateStyle(rManager, fontName, size, mono, 0, 0);
    }

    public static Label.LabelStyle generateStyle(IResourceRetriever rManager, String fontName, int size, boolean mono,
                                                 float borderWidth, int borderColor) {
        if (size == 0) {
            size = labelDefaultSize;
        }
        return new Label.LabelStyle(rManager.getFont(fontName, size, mono, borderWidth, borderColor), null);
    }

    /** The style a label's own settings ask for: its bitmap font, or its FreeType font with its outline. */
    public static Label.LabelStyle generateStyle(IResourceRetriever rManager, LabelComponent component) {
        if (component.bitmapFont != null) return generateStyle(rManager, component.bitmapFont);
        return generateStyle(rManager, component.fontName, component.fontSize, component.mono,
                component.borderWidth, Color.rgba8888(component.borderColor));
    }

    public static void setBorder(LabelComponent component, LabelVO vo) {
        component.borderWidth = vo.borderWidth;
        if (vo.borderColor != null) component.borderColor.set(vo.borderColor[0], vo.borderColor[1], vo.borderColor[2], vo.borderColor[3]);
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
