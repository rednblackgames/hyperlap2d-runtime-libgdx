package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import games.rednblack.editor.renderer.tenpatch.TenPatchDrawable;

public class NinePatchComponent  extends PooledComponent {
	public String textureRegionName;
	/** Drawable built from the region's {@link games.rednblack.editor.renderer.data.TenPatchVO}, scaled to world units. */
	public transient TenPatchDrawable tenPatch;

	@Override
	public void reset() {
		textureRegionName = null;
		tenPatch = null;
	}
}
