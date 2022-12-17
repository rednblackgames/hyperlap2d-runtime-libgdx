package games.rednblack.editor.renderer.utils;

import games.rednblack.editor.renderer.factory.component.ComponentFactory;

public interface AsyncEntityFactoryCallback {
    void onEntityCreated(int entity, ComponentFactory.InitialData data);
}
