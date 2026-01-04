package games.rednblack.editor.renderer.components;

import games.rednblack.editor.renderer.ecs.PooledComponent;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

public class MainItemComponent extends PooledComponent {
    public String uniqueId = null;
	public String itemIdentifier = "";
	public String libraryLink = "";

	public int entityType;
	public boolean visible = true;
	public boolean culled = false;

	public ObjectSet<String> tags = new ObjectSet<>(0);
	public ObjectMap<String, String> customVariables = new ObjectMap<>(0);

	public void setCustomVars(String key, String value) {
		customVariables.put(key, value);
	}

	public void removeCustomVars(String key) {
		customVariables.remove(key);
	}

	@Override
	public void reset() {
		uniqueId = null;
		itemIdentifier = "";
		libraryLink = "";

		entityType = 0;
		visible = true;
		culled = false;

		tags.clear();
		customVariables.clear();
	}
}
