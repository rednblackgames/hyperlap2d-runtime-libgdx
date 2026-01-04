package games.rednblack.editor.renderer.ecs;

import games.rednblack.editor.renderer.ecs.utils.Bag;
import games.rednblack.editor.renderer.ecs.utils.ImmutableBag;

import games.rednblack.editor.renderer.ecs.utils.BitVector;

/** Delegate for system invocation.
 *
 * Maybe you want to more granular control over system invocations, feed certain systems different deltas,
 * or completely rewrite processing in favor of events. Extending this class allows you to write your own
 * logic for processing system invocation.
 *
 * Register it with {@link EngineConfigurationBuilder#register(SystemInvocationStrategy)}
 * 
 * Be sure to call {@link #updateEntityStates()} after the engine dies.
 *
 * @see InvocationStrategy for the default strategy.
 */
public abstract class SystemInvocationStrategy {

	/** Engine to operate on. */
	protected Engine engine;
	protected final BitVector disabled = new BitVector();
	protected Bag<BaseSystem> systems;

	/** Engine to operate on. */
	protected final void setEngine(Engine engine) {
		this.engine = engine;
	}

	/**
	 * Called prior to {@link #initialize()}
	 */
	protected void setSystems(Bag<BaseSystem> systems) {
		this.systems = systems;
	}

	/** Called during engine initialization phase. */
	protected void initialize() {}

	/** Call to inform all systems and subscription of engine state changes. */
	protected final void updateEntityStates() {
		engine.batchProcessor.update();
	}

	/**
	 * Process all systems.
	 *
	 * @deprecated superseded by {@link #process()}
	 */
	@Deprecated
	protected final void process(Bag<BaseSystem> systems) {
		throw new RuntimeException("wrong process method");
	}

	protected abstract void process();

	public boolean isEnabled(BaseSystem system) {
		Class<? extends BaseSystem> target = system.getClass();
		ImmutableBag<BaseSystem> systems = engine.getSystems();
		for (int i = 0; i < systems.size(); i++) {
			if (target == systems.get(i).getClass())
				return !disabled.get(i);
		}

		throw new RuntimeException("huh?");
	}

	public void setEnabled(BaseSystem system, boolean value) {
		Class<? extends BaseSystem> target = system.getClass();
		ImmutableBag<BaseSystem> systems = engine.getSystems();
		for (int i = 0; i < systems.size(); i++) {
			if (target == systems.get(i).getClass())
				disabled.set(i, !value);
		}
	}
}
