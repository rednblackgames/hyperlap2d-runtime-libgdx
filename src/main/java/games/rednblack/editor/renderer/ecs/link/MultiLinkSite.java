package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.ComponentType;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.annotations.LinkPolicy;
import com.badlogic.gdx.utils.reflect.Field;

class MultiLinkSite extends LinkSite {
	MultiFieldMutator fieldMutator;

	protected MultiLinkSite(Engine engine,
							ComponentType type,
							Field field) {

		super(engine, type, field, LinkPolicy.Policy.CHECK_SOURCE);
	}

	@Override
	protected void check(int id) {
		Object collection = fieldMutator.read(mapper.get(id), field);
		fieldMutator.validate(id, collection, listener);
	}

	@Override
	protected void insert(int id) {
		if (listener != null)
			listener.onLinkEstablished(id, -1);
	}

	@Override
	protected void removed(int id) {
		if (listener != null)
			listener.onLinkKilled(id, -1);
	}
}
