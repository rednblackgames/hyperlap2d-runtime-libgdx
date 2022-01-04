package games.rednblack.editor.renderer.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

public class MainItemComponent extends PooledComponent {
    public transient int uniqueId = -1;
	public String itemIdentifier = "";
	public String libraryLink = "";

	public int entityType;
	public boolean visible = true;
	public boolean culled = false;

	public ObjectSet<String> tags = new ObjectSet<>();
	public ObjectMap<String, String> customVariables = new ObjectMap<>();

	public void setCustomVars(String key, String value) {
		customVariables.put(key, value);
	}

	public void removeCustomVars(String key) {
		customVariables.remove(key);
	}

	@Override
	public void reset() {
		uniqueId = -1;
		itemIdentifier = "";
		libraryLink = "";

		entityType = 0;
		visible = true;
		culled = false;

		tags.clear();
		customVariables.clear();
	}
}
