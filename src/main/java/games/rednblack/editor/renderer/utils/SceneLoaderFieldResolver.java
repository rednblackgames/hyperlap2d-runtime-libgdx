package games.rednblack.editor.renderer.utils;

import com.artemis.World;
import com.artemis.injection.FieldResolver;
import com.artemis.utils.reflect.Field;
import games.rednblack.editor.renderer.SceneLoader;
import games.rednblack.editor.renderer.factory.ActionFactory;
import games.rednblack.editor.renderer.factory.EntityFactory;

public class SceneLoaderFieldResolver implements FieldResolver {

    private final SceneLoader sceneLoader;

    public SceneLoaderFieldResolver(SceneLoader sceneLoader) {
        this.sceneLoader = sceneLoader;
    }

    @Override
    public void initialize(World world) {

    }

    @Override
    public Object resolve(Object target, Class<?> fieldType, Field field) {
        if (fieldType == SceneLoader.class)
            return sceneLoader;
        else if (fieldType == com.badlogic.gdx.physics.box2d.World.class)
            return sceneLoader.getWorld();
        else if (fieldType == EntityFactory.class)
            return sceneLoader.getEntityFactory();
        else if (fieldType == ActionFactory.class)
            return sceneLoader.getActionFactory();
        return null;
    }
}
