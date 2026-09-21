package games.rednblack.editor.renderer.widget.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import games.rednblack.editor.renderer.SceneLoader;
import games.rednblack.editor.renderer.components.MainItemComponent;
import games.rednblack.editor.renderer.components.TextureRegionComponent;
import games.rednblack.editor.renderer.components.LayoutComponent;
import games.rednblack.editor.renderer.components.TintComponent;
import games.rednblack.editor.renderer.components.TransformComponent;
import games.rednblack.editor.renderer.components.label.LabelComponent;
import games.rednblack.editor.renderer.components.normal.NormalMapRendering;
import games.rednblack.editor.renderer.components.particle.ParticleComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationComponent;
import games.rednblack.editor.renderer.components.sprite.SpriteAnimationStateComponent;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.resources.IResourceRetriever;
import games.rednblack.editor.renderer.systems.WidgetStateSystem;
import games.rednblack.editor.renderer.utils.ABAtlasRegion;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.widget.ChoiceOverrideHandler;
import games.rednblack.editor.renderer.widget.ColorPreviewHandler;
import games.rednblack.editor.renderer.widget.InterpolableOverrideHandler;
import games.rednblack.editor.renderer.widget.SequencedOverrideHandler;
import games.rednblack.editor.renderer.widget.StateOverrideHandler;
import games.rednblack.editor.renderer.widget.ToggleOverrideHandler;

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
    public static final String EMITTING = "emitting";
    public static final String MARGIN_LEFT = "marginLeft";
    public static final String MARGIN_RIGHT = "marginRight";
    public static final String MARGIN_TOP = "marginTop";
    public static final String MARGIN_BOTTOM = "marginBottom";
    public static final String HORIZONTAL_BIAS = "horizontalBias";
    public static final String VERTICAL_BIAS = "verticalBias";

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
        system.registerHandler(EMITTING, new Emitting());

        system.registerHandler(MARGIN_LEFT, new Margin(LayoutComponent.ConstraintSide.LEFT));
        system.registerHandler(MARGIN_RIGHT, new Margin(LayoutComponent.ConstraintSide.RIGHT));
        system.registerHandler(MARGIN_TOP, new Margin(LayoutComponent.ConstraintSide.TOP));
        system.registerHandler(MARGIN_BOTTOM, new Margin(LayoutComponent.ConstraintSide.BOTTOM));
        system.registerHandler(HORIZONTAL_BIAS, new Bias(true));
        system.registerHandler(VERTICAL_BIAS, new Bias(false));
    }

    public static class Visible implements ToggleOverrideHandler {
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
    public static class Tint implements InterpolableOverrideHandler, ColorPreviewHandler {
        protected ComponentMapper<TintComponent> tintCM;
        private final Color tmpColor = new Color();

        @Override
        public boolean toColor(int entity, String value, Color out) {
            try {
                Color.valueOf(value, out);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

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
    public static class SpriteAnimation implements SequencedOverrideHandler, ChoiceOverrideHandler {
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
            applySequence(entity, value, null, null);
        }

        /**
         * The animation the state settles on is the one the component reports, whatever is playing
         * on the way there, so what gets captured and saved is never a passing frame range.
         */
        @Override
        public void applySequence(int entity, String value, String enter, String exit) {
            SpriteAnimationComponent animation = spriteAnimationCM.get(entity);
            if (!has(animation, value)) {
                Gdx.app.error("WidgetState", "unknown sprite animation override: " + value);
                return;
            }
            animation.currentAnimation = value;

            SpriteAnimationStateComponent state = spriteAnimationStateCM.get(entity);
            state.queue.clear();
            if (has(animation, exit)) state.queue.add(exit);
            if (has(animation, enter)) state.queue.add(enter);
            state.queue.add(value);

            String first = state.queue.removeIndex(0);
            boolean last = state.queue.size == 0;
            state.set(animation.frameRangeMap.get(first), animation.fps,
                    last ? animation.playMode : com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
        }

        @Override
        public Array<String> getChoices(int entity) {
            Array<String> names = new Array<>();
            for (String name : spriteAnimationCM.get(entity).frameRangeMap.keySet()) names.add(name);
            names.sort();
            return names;
        }

        private boolean has(SpriteAnimationComponent animation, String name) {
            return name != null && !name.isEmpty() && animation.frameRangeMap.containsKey(name);
        }
    }

    /**
     * Whether the emitters of a particle effect are making new particles. Turning them off lets the
     * particles already on screen live out their lives rather than freezing them where they are, and
     * turning them back on starts the effect over, so a burst fires again every time a state begins.
     */
    public static class Emitting implements ToggleOverrideHandler {
        protected ComponentMapper<ParticleComponent> particleCM;

        @Override
        public boolean supports(int entity) {
            ParticleComponent component = particleCM.get(entity);
            return component != null && component.particleEffect != null;
        }

        @Override
        public String capture(int entity) {
            return Boolean.toString(particleCM.get(entity).emitting);
        }

        @Override
        public void apply(int entity, String value) {
            ParticleComponent component = particleCM.get(entity);
            boolean emitting = Boolean.parseBoolean(value);
            if (emitting == component.emitting) return;

            component.emitting = emitting;
            if (emitting) component.particleEffect.start();
            else component.particleEffect.allowCompletion();
        }
    }

    /**
     * How far a constrained side sits from what it is constrained to. A property only while that
     * side is constrained, since there is nothing to keep a distance from otherwise.
     */
    public static class Margin implements InterpolableOverrideHandler {
        protected ComponentMapper<LayoutComponent> layoutCM;
        private final LayoutComponent.ConstraintSide side;

        public Margin(LayoutComponent.ConstraintSide side) {
            this.side = side;
        }

        private LayoutComponent.ConstraintData constraint(int entity) {
            LayoutComponent layout = layoutCM.get(entity);
            if (layout == null) return null;
            switch (side) {
                case LEFT: return layout.left;
                case RIGHT: return layout.right;
                case TOP: return layout.top;
                default: return layout.bottom;
            }
        }

        @Override
        public boolean supports(int entity) {
            return constraint(entity) != null;
        }

        @Override
        public String capture(int entity) {
            return Float.toString(constraint(entity).margin);
        }

        @Override
        public void apply(int entity, String value) {
            try {
                constraint(entity).margin = Float.parseFloat(value);
            } catch (NumberFormatException | NullPointerException e) {
                Gdx.app.error("WidgetState", "invalid " + side + " margin override: " + value);
            }
        }

        @Override
        public int getChannelCount() {
            return 1;
        }

        @Override
        public void captureChannels(int entity, float[] out) {
            out[0] = constraint(entity).margin;
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
            constraint(entity).margin = values[0];
        }
    }

    /** Where the item sits between the two sides it is constrained to, from zero to one. */
    public static class Bias implements InterpolableOverrideHandler {
        protected ComponentMapper<LayoutComponent> layoutCM;
        private final boolean horizontal;

        public Bias(boolean horizontal) {
            this.horizontal = horizontal;
        }

        @Override
        public boolean supports(int entity) {
            return layoutCM.get(entity) != null;
        }

        @Override
        public String capture(int entity) {
            LayoutComponent layout = layoutCM.get(entity);
            return Float.toString(horizontal ? layout.horizontalBias : layout.verticalBias);
        }

        @Override
        public void apply(int entity, String value) {
            try {
                set(entity, Float.parseFloat(value));
            } catch (NumberFormatException | NullPointerException e) {
                Gdx.app.error("WidgetState", "invalid bias override: " + value);
            }
        }

        @Override
        public int getChannelCount() {
            return 1;
        }

        @Override
        public void captureChannels(int entity, float[] out) {
            LayoutComponent layout = layoutCM.get(entity);
            out[0] = horizontal ? layout.horizontalBias : layout.verticalBias;
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

        private void set(int entity, float value) {
            LayoutComponent layout = layoutCM.get(entity);
            if (horizontal) layout.horizontalBias = value;
            else layout.verticalBias = value;
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
