package games.rednblack.editor.renderer.ecs.utils;

import games.rednblack.editor.renderer.ecs.BaseSystem;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.annotations.Profile;


/**
 * @see Profile
 */
public interface ArtemisProfiler {
	void start();
	void stop();
	void initialize(BaseSystem owner, Engine engine);
}