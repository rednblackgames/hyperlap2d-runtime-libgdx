package games.rednblack.editor.renderer.scripts;

import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.components.physics.PhysicsBodyComponent;
import games.rednblack.editor.renderer.physics.PhysicsContact;

/**
 * A Basic Script that guarantee init function called when body is ready and not null
 */
public abstract class PhysicsBodyScript extends BasicScript implements PhysicsContact {

    protected ComponentMapper<PhysicsBodyComponent> physicsMapper;
    protected PhysicsBodyComponent physicsBodyComponent;

    @Override
    public void doInit(int item) {
        if (isInit()) return;

        physicsBodyComponent = physicsMapper.get(item);
        if (physicsBodyComponent == null || physicsBodyComponent.body != null) {
            super.doInit(item);
        }
    }
}
