package games.rednblack.editor.renderer.data;

import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.utils.ComponentRetriever;

public class LabelVO extends MainItemVO {
	
	public String 	text 	= "Label";
	public String	style	=  "";
	public int		size;
	public int		align;

    public float width = 0;
    public float height = 0;

    public boolean wrap = false;
    public boolean isTyping = false;
	public boolean monoSpace = false;
	
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
		isTyping  = vo.isTyping;
		monoSpace = vo.monoSpace;
	}

	@Override
	public void loadFromEntity(int entity, com.artemis.World engine) {
		super.loadFromEntity(entity, engine);
		LabelComponent labelComponent = ComponentRetriever.get(entity, LabelComponent.class, engine);
		DimensionsComponent dimensionsComponent = ComponentRetriever.get(entity,DimensionsComponent.class, engine);

		text = labelComponent.getText().toString();
		style = labelComponent.fontName;
		size = labelComponent.fontSize;
		align = labelComponent.labelAlign;
		wrap = labelComponent.wrap;
		monoSpace = labelComponent.mono;

		isTyping = labelComponent.typingEffect;

		width = dimensionsComponent.width;
		height = dimensionsComponent.height;
	}
}
