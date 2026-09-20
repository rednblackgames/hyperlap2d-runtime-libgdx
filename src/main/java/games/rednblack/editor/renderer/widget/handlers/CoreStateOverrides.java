package games.rednblack.editor.renderer.widget.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import games.rednblack.editor.renderer.SceneLoader;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.systems.WidgetStateSystem;
import games.rednblack.editor.renderer.utils.ABAtlasRegion;
import games.rednblack.editor.renderer.widget.InterpolableOverrideHandler;
import games.rednblack.editor.renderer.widget.StateOverrideHandler;

/**
 * Overridable properties every runtime supports out of the box. Extensions add their own through
 * {@link games.rednblack.editor.renderer.commons.IExternalItemType#registerStateOverrideHandlers}.
 */
public final class CoreStateOverrides {
    private CoreStateOverrides() {
    }

    public static final String VISIBLE = "visible";
    public static final String TINT = "tint";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String SCALE_X = "scaleX";
    public static final String SCALE_Y = "scaleY";
    public static final String ROTATION = "rotation";
    public static final String REGION = "region";
    public static final String SPRITE_ANIMATION = "spriteAnimation";
    public static final String LABEL_TEXT = "labelText";

    public static void registerAll(WidgetStateSystem system) {
        system.registerHandler(VISIBLE, new Visible());
        system.registerHandler(TINT, new Tint());
        system.registerHandler(X, new Transform(Transform.Field.X));
        system.registerHandler(Y, new Transform(Transform.Field.Y));
        system.registerHandler(SCALE_X, new Transform(Transform.Field.SCALE_X));
        system.registerHandler(SCALE_Y, new Transform(Transform.Field.SCALE_Y));
        system.registerHandler(ROTATION, new Transform(Transform.Field.ROTATION));
        system.registerHandler(REGION, new Region());
        system.registerHandler(SPRITE_ANIMATION, new SpriteAnimation());
        system.registerHandler(LABEL_TEXT, new LabelText());
    }

    public static class Visible implements StateOverrideHandler {
        protected ComponentMapper<MainItemComponent> mainItemCM;

        @Override
        public boolean supports(int entity) {
            return mainItemCM.has(entity);
        }

        @Override
        public String capture(int entity) {
            return Boolean.toString(mainItemCM.get(entity).visible);
        }

        @Override
        public void apply(int entity, String value) {
            mainItemCM.get(entity).visible = Boolean.parseBoolean(value);
        }
    }

    /** Value is an {@code rrggbbaa} hex string. */
    public static class Tint implements InterpolableOverrideHandler {
        protected ComponentMapper<TintComponent> tintCM;
        private final Color tmpColor = new Color();

        @Override
        public int getChannelCount() {
            return 4;
        }

        @Override
        public void captureChannels(int entity, float[] out) {
            Color color = tintCM.get(entity).color;
            out[0] = color.r;
            out[1] = color.g;
            out[2] = color.b;
            out[3] = color.a;
        }

        @Override
        public boolean parseChannels(String value, float[] out) {
            try {
                Color.valueOf(value, tmpColor);
            } catch (RuntimeException e) {
                return false;
            }
            out[0] = tmpColor.r;
            out[1] = tmpColor.g;
            out[2] = tmpColor.b;
            out[3] = tmpColor.a;
            return true;
        }

        @Override
        public void applyChannels(int entity, float[] values) {
            tintCM.get(entity).color.set(values[0], values[1], values[2], values[3]);
        }

        @Override
        public boolean supports(int entity) {
            return tintCM.has(entity);
        }

        @Override
        public String capture(int entity) {
            return tintCM.get(entity).color.toString();
        }

        @Override
        public void apply(int entity, String value) {
            try {
                Color.valueOf(value, tintCM.get(entity).color);
            } catch (RuntimeException e) {
                Gdx.app.error("WidgetState", "invalid tint override: " + value);
            }
        }
    }

    /** Absolute values, in the local space of the parent like the transform itself. */
    public static class Transform implements InterpolableOverrideHandler {
        public enum Field {X, Y, SCALE_X, SCALE_Y, ROTATION}

        protected ComponentMapper<TransformComponent> transformCM;
        private final Field field;

        public Transform(Field field) {
            this.field = field;
        }

        @Override
        public boolean supports(int entity) {
            return transformCM.has(entity);
        }

        @Override
        public String capture(int entity) {
            TransformComponent t = transformCM.get(entity);
            switch (field) {
                case X: return Float.toString(t.x);
                case Y: return Float.toString(t.y);
                case SCALE_X: return Float.toString(t.scaleX);
                case SCALE_Y: return Float.toString(t.scaleY);
                default: return Float.toString(t.rotation);
            }
        }

        @Override
        public void apply(int entity, String value) {
            float v;
            try {
                v = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                Gdx.app.error("WidgetState", "invalid " + field + " override: " + value);
                return;
            }
            set(entity, v);
        }

        @Override
        public int getChannelCount() {
            return 1;
        }

        @Override
        public void captureChannels(int entity, float[] out) {
            TransformComponent t = transformCM.get(entity);
            switch (field) {
                case X: out[0] = t.x; break;
                case Y: out[0] = t.y; break;
                case SCALE_X: out[0] = t.scaleX; break;
                case SCALE_Y: out[0] = t.scaleY; break;
                default: out[0] = t.rotation; break;
            }
        }

        @Override
        public boolean parseChannels(String value, float[] out) {
            try {
                out[0] = Float.parseFloat(value);
                return true;
            } catch (NumberFormatException | NullPointerException e) {
                return false;
            }
        }

        @Override
        public void applyChannels(int entity, float[] values) {
            set(entity, values[0]);
        }

        private void set(int entity, float v) {
            TransformComponent t = transformCM.get(entity);
            switch (field) {
                case X: t.x = v; break;
                case Y: t.y = v; break;
                case SCALE_X: t.scaleX = v; break;
                case SCALE_Y: t.scaleY = v; break;
                default: t.rotation = v; break;
            }
        }
    }

    /**
     * Swaps the texture region of an image. Dimensions are left untouched, the regions of the
     * different states are expected to share the same size.
     */
    public static class Region implements StateOverrideHandler {
        protected ComponentMapper<TextureRegionComponent> textureRegionCM;
        protected ComponentMapper<SpriteAnimationComponent> spriteAnimationCM;
        protected ComponentMapper<NormalMapRendering> normalMapRenderingCM;
        protected SceneLoader sceneLoader;

        @Override
        public boolean supports(int entity) {
            // animated sprites own their region frame by frame
            return textureRegionCM.has(entity) && !spriteAnimationCM.has(entity);
        }

        @Override
        public String capture(int entity) {
            return textureRegionCM.get(entity).regionName;
        }

        @Override
        public void apply(int entity, String value) {
            IResourceRetriever rm = sceneLoader.getRm();
            if (!rm.hasTextureRegion(value)) {
                Gdx.app.error("WidgetState", "missing texture region override: " + value);
                return;
            }

            TextureRegionComponent component = textureRegionCM.get(entity);
            TextureRegion region = rm.getTextureRegion(value);
            if (normalMapRenderingCM.has(entity) && rm.hasTextureRegion(value + ".normal")) {
                region = new ABAtlasRegion((TextureAtlas.AtlasRegion) region,
                        (TextureAtlas.AtlasRegion) rm.getTextureRegion(value + ".normal"),
                        normalMapRenderingCM.get(entity));
            }
            component.regionName = value;
            component.region = region;
            if (component.repeatablePolygonSprite != null) {
                component.repeatablePolygonSprite.setTextureRegion(region);
            }
        }
    }

    /** Value is the name of one of the frame ranges of the sprite animation. */
    public static class SpriteAnimation implements StateOverrideHandler {
        protected ComponentMapper<SpriteAnimationComponent> spriteAnimationCM;
        protected ComponentMapper<SpriteAnimationStateComponent> spriteAnimationStateCM;

        @Override
        public boolean supports(int entity) {
            return spriteAnimationCM.has(entity) && spriteAnimationStateCM.has(entity);
        }

        @Override
        public String capture(int entity) {
            return spriteAnimationCM.get(entity).currentAnimation;
        }

        @Override
        public void apply(int entity, String value) {
            SpriteAnimationComponent animation = spriteAnimationCM.get(entity);
            if (value == null || !animation.frameRangeMap.containsKey(value)) {
                Gdx.app.error("WidgetState", "unknown sprite animation override: " + value);
                return;
            }
            animation.currentAnimation = value;
            spriteAnimationStateCM.get(entity).set(animation);
        }
    }

    public static class LabelText implements StateOverrideHandler {
        protected ComponentMapper<LabelComponent> labelCM;

        @Override
        public boolean supports(int entity) {
            return labelCM.has(entity);
        }

        @Override
        public String capture(int entity) {
            return labelCM.get(entity).getText().toString();
        }

        @Override
        public void apply(int entity, String value) {
            labelCM.get(entity).setText(value);
        }
    }
}
