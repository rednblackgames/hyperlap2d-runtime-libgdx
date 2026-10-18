package games.rednblack.editor.renderer.input;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import games.rednblack.editor.renderer.components.CompositeTransformComponent;
import games.rednblack.editor.renderer.components.DimensionsComponent;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.NodeComponent;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.ViewPortComponent;
import games.rednblack.editor.renderer.components.ZIndexComponent;
import games.rednblack.editor.renderer.components.additional.InputTargetComponent;
import games.rednblack.editor.renderer.ecs.BaseSystem;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.EngineConfigurationBuilder;
import games.rednblack.editor.renderer.systems.UIInputSystem;

/**
 * A scene with a viewport over it, for tests that feed real input.
 *
 * Everything is said in scene coordinates, y up, the way the entities are placed: the scene turns
 * them into the screen coordinates libGDX would report.
 */
public class InputTestScene {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    public final Engine engine;
    public final UIInputSystem input;
    public final int root;

    public InputTestScene(BaseSystem... systems) {
        EngineConfigurationBuilder builder = new EngineConfigurationBuilder().with(new UIInputSystem());
        if (systems.length > 0) builder.with(systems);
        engine = new Engine(builder.build());
        input = engine.getSystem(UIInputSystem.class);

        Viewport viewport = new TestViewport();
        viewport.setScreenBounds(0, 0, WIDTH, HEIGHT);
        viewport.setWorldSize(WIDTH, HEIGHT);

        root = engine.create();
        engine.edit(root).create(NodeComponent.class);
        engine.edit(root).create(TransformComponent.class);
        engine.edit(root).create(ViewPortComponent.class).viewPort = viewport;
        engine.process();
    }

    /** A plain entity, one input never finds: only a composite to put things in, or something drawn. */
    public int entity(int parent, float x, float y, float width, float height) {
        int entity = engine.create();
        engine.edit(entity).create(NodeComponent.class);
        engine.edit(entity).create(MainItemComponent.class);
        engine.edit(entity).create(ZIndexComponent.class);
        TransformComponent transform = engine.edit(entity).create(TransformComponent.class);
        transform.x = x;
        transform.y = y;
        DimensionsComponent dimensions = engine.edit(entity).create(DimensionsComponent.class);
        dimensions.width = width;
        dimensions.height = height;

        engine.edit(entity).create(ParentNodeComponent.class).parentEntity = parent;
        engine.getMapper(NodeComponent.class).get(parent).addChild(entity);
        engine.process();
        return entity;
    }

    /** An entity input can find, the last one added being the one in front. */
    public int target(int parent, float x, float y, float width, float height) {
        int entity = entity(parent, x, y, width, height);
        engine.edit(entity).create(InputTargetComponent.class);
        engine.process();
        return entity;
    }

    public InputTargetComponent inputOf(int entity) {
        return engine.getMapper(InputTargetComponent.class).get(entity);
    }

    /** Makes the entity clip what it holds to its own rectangle. */
    public void clip(int entity) {
        engine.edit(entity).create(CompositeTransformComponent.class).scissorsEnabled = true;
        engine.process();
    }

    /** Lets input find an entity that was not made a target to begin with, such as the root. */
    public InputTargetComponent makeTarget(int entity) {
        InputTargetComponent target = engine.edit(entity).create(InputTargetComponent.class);
        engine.process();
        return target;
    }

    public void frame() {
        engine.process();
    }

    /** What the pointer would find at that place in the scene. */
    public int hitAt(float x, float y) {
        return input.hit(x, screenY(y));
    }

    public boolean move(float x, float y) {
        return input.mouseMoved((int) x, screenY(y));
    }

    public boolean press(float x, float y) {
        return press(x, y, 0, 0);
    }

    public boolean press(float x, float y, int pointer, int button) {
        return input.touchDown((int) x, screenY(y), pointer, button);
    }

    public boolean drag(float x, float y) {
        return drag(x, y, 0);
    }

    public boolean drag(float x, float y, int pointer) {
        return input.touchDragged((int) x, screenY(y), pointer);
    }

    public boolean release(float x, float y) {
        return release(x, y, 0, 0);
    }

    public boolean release(float x, float y, int pointer, int button) {
        return input.touchUp((int) x, screenY(y), pointer, button);
    }

    /** A whole click, mouse first: the pointer arrives, presses and lets go. */
    public void click(float x, float y) {
        move(x, y);
        press(x, y);
        release(x, y);
    }

    private int screenY(float y) {
        return (int) (HEIGHT - y);
    }

    /**
     * The scene fills the screen one unit per pixel, so a point only needs turning the right way up.
     * A real viewport would go through the camera, which needs the native libraries tests do without.
     */
    private static class TestViewport extends Viewport {
        @Override
        public Vector2 unproject(Vector2 screenCoords) {
            screenCoords.y = HEIGHT - screenCoords.y;
            return screenCoords;
        }
    }
}
