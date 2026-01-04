package games.rednblack.editor.renderer.ecs;

public class FakeEntityFactory {
	private FakeEntityFactory() {}

	public static Entity create(Engine engine, int entityId) {
		return new Entity(engine, entityId);
	}
}
