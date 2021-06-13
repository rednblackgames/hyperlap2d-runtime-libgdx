package games.rednblack.editor.renderer.factory.component;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import games.rednblack.editor.renderer.box2dLight.RayHandler;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.label.TypingLabelComponent;
import games.rednblack.editor.renderer.data.LabelVO;
import games.rednblack.editor.renderer.data.MainItemVO;
import games.rednblack.editor.renderer.data.ProjectInfoVO;
import games.rednblack.editor.renderer.data.ResolutionEntryVO;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.resources.IResourceRetriever;

public class LabelComponentFactory extends ComponentFactory {

    private static int labelDefaultSize = 12;

    public LabelComponentFactory(com.artemis.World engine, RayHandler rayHandler, World world, IResourceRetriever rm) {
        super(engine, rayHandler, world, rm);
    }

    @Override
    public void createComponents(int root, int entity, MainItemVO vo) {
        createCommonComponents(entity, vo, EntityFactory.LABEL_TYPE);
        createParentNodeComponent(root, entity);
        createNodeComponent(root, entity);
        createLabelComponent(entity, (LabelVO) vo);
    }

    @Override
    protected DimensionsComponent createDimensionsComponent(int entity, MainItemVO vo) {
        DimensionsComponent component = engine.edit(entity).create(DimensionsComponent.class);
        component.height = ((LabelVO) vo).height;
        component.width = ((LabelVO) vo).width;

        return component;
    }

    protected LabelComponent createLabelComponent(int entity, LabelVO vo) {
        LabelComponent component = engine.edit(entity).create(LabelComponent.class);
        component.setText(vo.text);
        component.setStyle(generateStyle(rm, vo.style, vo.size));
        component.fontName = vo.style;
        component.fontSize = vo.size;
        component.setAlignment(vo.align);
        component.setWrap(vo.wrap);

        ProjectInfoVO projectInfoVO = rm.getProjectVO();
        ResolutionEntryVO resolutionEntryVO = rm.getLoadedResolution();
        float multiplier = resolutionEntryVO.getMultiplier(rm.getProjectVO().originalResolution);

        component.setFontScale(multiplier / projectInfoVO.pixelToWorld);

        if (vo.isTyping) {
            TypingLabelComponent typingLabelComponent = engine.edit(entity).create(TypingLabelComponent.class);
        }
        return component;
    }


    public static LabelStyle generateStyle(IResourceRetriever rManager, String fontName, int size) {

        if (size == 0) {
            size = labelDefaultSize;
        }
        return new LabelStyle(rManager.getBitmapFont(fontName, size), null);
    }

}
