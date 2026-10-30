package games.rednblack.editor.renderer.data;

import java.util.Arrays;
import com.badlogic.gdx.graphics.Color;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.components.AutoLayoutOwnSizeComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.factory.EntityFactory;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class LabelVO extends MainItemVO {
	
	public String 	text 	= "Label";
	public String	style	=  "";
	public int		size;
	public int		align;

    public float width = 0;
    public float height = 0;

    public boolean wrap = false;
    /** As wide as its text: see {@link LabelComponent#fitWidth}. */
    public boolean fitWidth = false;
    /** As tall as its text: see {@link LabelComponent#fitHeight}. */
    public boolean fitHeight = false;
    public boolean isTyping = false;
	public boolean monoSpace = false;
	/** See {@link LabelComponent#borderWidth}. */
	public float borderWidth = 0;
	public float[] borderColor = {0, 0, 0, 1};

	public String bitmapFont = null;
	
	public LabelVO() {
		super();
	}
	
	public LabelVO(LabelVO vo) {
		super(vo);
		text 	  = new String(vo.text);
		style 	  = new String(vo.style);
		size 	  = vo.size;
		align 	  = vo.align;
        width 	  = vo.width;
        height 	  = vo.height;
        wrap      = vo.wrap;
        fitWidth  = vo.fitWidth;
        fitHeight = vo.fitHeight;
		isTyping  = vo.isTyping;
		monoSpace = vo.monoSpace;
		borderWidth = vo.borderWidth;
		borderColor = vo.borderColor == null ? null : Arrays.copyOf(vo.borderColor, vo.borderColor.length);
		bitmapFont = vo.bitmapFont;
	}

	@Override
	public void loadFromEntity(int entity, Engine engine, EntityFactory entityFactory) {
		super.loadFromEntity(entity, engine, entityFactory);
		LabelComponent labelComponent = ComponentRetriever.get(entity, LabelComponent.class, engine);
		DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity,DimensionsComponent.class, engine);

		text = labelComponent.getText().toString();
		style = labelComponent.fontName;
		size = labelComponent.fontSize;
		align = labelComponent.labelAlign;
		wrap = labelComponent.wrap;
		fitWidth = labelComponent.fitWidth;
		fitHeight = labelComponent.fitHeight;
		monoSpace = labelComponent.mono;
		borderWidth = labelComponent.borderWidth;
		Color border = labelComponent.borderColor;
		borderColor = new float[]{border.r, border.g, border.b, border.a};
		bitmapFont = labelComponent.bitmapFont;

		isTyping = labelComponent.typingEffect;

		// the size of its own, not the one an auto layout is giving it
		AutoLayoutOwnSizeComponent ownSize = ComponentRetriever.get(entity, AutoLayoutOwnSizeComponent.class, engine);
		width = AutoLayoutOwnSizeComponent.ownWidth(ownSize, dimensionsComponent);
		height = AutoLayoutOwnSizeComponent.ownHeight(ownSize, dimensionsComponent);
	}

	@Override
	public String getResourceName() {
		return bitmapFont;
	}
}
