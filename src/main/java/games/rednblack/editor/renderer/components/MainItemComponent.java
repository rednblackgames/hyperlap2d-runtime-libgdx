package games.rednblack.editor.renderer.components;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import games.rednblack.editor.renderer.utils.CustomVariables;

import java.util.HashSet;
import java.util.Set;

public class MainItemComponent implements BaseComponent {
	public Affine2 worldTransform = new Affine2();
	public Matrix4 computedTransform = new Matrix4();
	public Matrix4 oldTransform = new Matrix4();

    public int uniqueId = 0;
	public String itemIdentifier = "";
	public String libraryLink = "";
    public Set<String> tags = new HashSet<>();
    private String customVars = "";
    public CustomVariables customVariables = new CustomVariables();
	public int entityType;
	public boolean visible = true;
	public boolean culled = false;

	public void setCustomVars(String key, String value) {
		customVariables.setVariable(key, value);
		setCustomVarString(customVariables.saveAsString());
	}

	public void removeCustomVars(String key) {
		customVariables.removeVariable(key);
		setCustomVarString(customVariables.saveAsString());
	}

	public String getCustomVarString() {
		return customVars;
	}

	public void setCustomVarString(String vars) {
		customVars = vars;
		if (customVariables.getCount() == 0) {
			customVariables.loadFromString(customVars);
		}
	}

	@Override
	public void reset() {
		uniqueId = 0;
		itemIdentifier = "";
		libraryLink = "";
		tags.clear();
		customVars = "";
		customVariables.clear();
		entityType = 0;
		visible = true;
		culled = false;

		worldTransform.idt();
		computedTransform.idt();
		oldTransform.idt();
	}
}
