package games.rednblack.editor.renderer.systems;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.Pool;
import games.rednblack.editor.renderer.components.ParentNodeComponent;
import games.rednblack.editor.renderer.components.widget.WidgetComponent;
import games.rednblack.editor.renderer.components.widget.WidgetPartComponent;
import games.rednblack.editor.renderer.ecs.ComponentMapper;
import games.rednblack.editor.renderer.ecs.annotations.All;
import games.rednblack.editor.renderer.ecs.systems.IteratingSystem;
import games.rednblack.editor.renderer.widget.InterpolableOverrideHandler;
import games.rednblack.editor.renderer.widget.SequencedOverrideHandler;
import games.rednblack.editor.renderer.widget.StateOverrideHandler;
import games.rednblack.editor.renderer.widget.StateTween;
import games.rednblack.editor.renderer.widget.handlers.CoreStateOverrides;

import java.util.Objects;

/**
 * Makes widget parts look like the current state of the widget they belong to.
 *
 * Each part follows the nearest ancestor carrying a {@link WidgetComponent}. Work is only done when
 * that state changes, or while a property is still travelling: every property heads for the value
 * the new state overrides it with, or else back to its base value, which is remembered the moment
 * a state first touches it. A part therefore always ends up holding <i>base look + overrides of
 * one state</i>, never a mix of states.
 */
@All(WidgetPartComponent.class)
public class WidgetStateSystem extends IteratingSystem {

    protected ComponentMapper<WidgetComponent> widgetCM;
    protected ComponentMapper<WidgetPartComponent> partCM;
    protected ComponentMapper<ParentNodeComponent> parentNodeCM;

    private final OrderedMap<String, StateOverrideHandler> handlers = new OrderedMap<>();
    private boolean initialized = false;

    private final Array<String> tmpKeys = new Array<>();
    private final Array<String> chain = new Array<>();
    private final ObjectMap<String, String> mergedPatch = new ObjectMap<>();
    /** How far up a state may build on others, well beyond anything sensible. */
    private static final int MAX_INHERITANCE = 8;
    private final Pool<StateTween> tweenPool = new Pool<StateTween>() {
        @Override
        protected StateTween newObject() {
            return new StateTween();
        }
    };

    public WidgetStateSystem() {
        CoreStateOverrides.registerAll(this);
    }

    @Override
    protected void initialize() {
        initialized = true;
        for (StateOverrideHandler handler : handlers.values()) {
            engine.inject(handler);
        }
    }

    /**
     * Registers (or replaces) the handler of an overridable property. Can be called both before and
     * after the engine is built.
     */
    public void registerHandler(String key, StateOverrideHandler handler) {
        handlers.put(key, handler);
        if (initialized) engine.inject(handler);
    }

    public StateOverrideHandler getHandler(String key) {
        return handlers.get(key);
    }

    /** @return registered property keys, in registration order */
    public String[] getHandlerKeys() {
        return handlers.orderedKeys().toArray(String.class);
    }

    @Override
    protected void process(int entity) {
        process(entity, true);
    }

    private void process(int entity, boolean animate) {
        WidgetPartComponent part = partCM.get(entity);

        int widgetEntity = findWidget(entity);
        WidgetComponent widget = widgetEntity == -1 ? null : widgetCM.get(widgetEntity);
        String state = widget == null ? null : widget.getState();

        if (part.dirty || !Objects.equals(state, part.appliedState)) {
            applyState(entity, part, widget, state, animate && !part.snapNext);
        }
        part.snapNext = false;

        if (part.tweens.size > 0) {
            if (animate) advance(entity, part, engine.getDelta());
            else finishTransitions(entity, part);
        }
    }

    @Override
    protected void removed(int entityId) {
        WidgetPartComponent part = partCM.get(entityId);
        if (part == null) return;
        tweenPool.freeAll(part.tweens);
        part.tweens.clear();
    }

    /** @return the nearest ancestor that is a widget, -1 if there is none */
    public int findWidget(int entity) {
        ParentNodeComponent parentNode = parentNodeCM.get(entity);
        while (parentNode != null && parentNode.parentEntity != -1) {
            int parent = parentNode.parentEntity;
            if (widgetCM.has(parent)) return parent;
            parentNode = parentNodeCM.get(parent);
        }
        return -1;
    }

    /**
     * Puts every overridden property of the entity back to its base value, at once. To be called
     * before the entity is serialized or its base look is edited, so that state overrides never
     * leak into it. The state comes back on the next pass, at once too: the look was only taken off
     * to be read, which is not something to animate.
     */
    public void restoreBase(int entity) {
        WidgetPartComponent part = partCM.get(entity);
        if (part == null) return;
        applyState(entity, part, null, null, false);
        part.dirty = true;
        part.snapNext = true;
    }

    /**
     * Brings the entity to the look of the current state of its widget right away: not on the next
     * pass, and with no transition left running. For callers that need to read the resulting look
     * immediately (e.g. the editor recorder).
     */
    public void applyNow(int entity) {
        if (partCM.has(entity)) process(entity, false);
    }

    /** Forces the current state to be applied again, e.g. after the overrides have been edited. */
    public void refresh(int entity) {
        WidgetPartComponent part = partCM.get(entity);
        if (part != null) part.dirty = true;
    }

    /**
     * Sends every property the previous or the new state has a say on towards its new value: the
     * one the new state overrides it with, or else the base one. A property travels there if it is
     * made of numbers and a transition applies, the one of the new state or else, on the way back
     * to a value the new state has nothing to say about, the one of the state being left.
     */
    private void applyState(int entity, WidgetPartComponent part, WidgetComponent widget, String state, boolean animate) {
        ObjectMap<String, String> patch = overridesOf(part, widget, state);
        String previousState = part.appliedState;

        tmpKeys.clear();
        for (String key : part.baseSnapshot.keys()) tmpKeys.add(key);
        if (patch != null) {
            for (String key : patch.keys()) {
                if (!part.baseSnapshot.containsKey(key)) tmpKeys.add(key);
            }
        }

        for (int i = 0; i < tmpKeys.size; i++) {
            String key = tmpKeys.get(i);
            StateOverrideHandler handler = handlers.get(key);
            if (handler == null || !handler.supports(entity)) continue;

            boolean overridden = patch != null && patch.containsKey(key);
            StateTween running = findTween(part, key);

            // A property not in the snapshot sits at its base value, which is remembered before it
            // is touched. One still travelling back to base is in there, so an in-flight value is
            // never mistaken for the base one.
            if (!part.baseSnapshot.containsKey(key)) part.baseSnapshot.put(key, handler.capture(entity));
            String target = overridden ? patch.get(key) : part.baseSnapshot.get(key);

            // already on its way there (e.g. still returning to base when yet another state that
            // does not care about it comes in): let it finish
            if (running != null && animate && Objects.equals(running.target, target)) continue;

            // What plays around the value: the entry of the state coming in, and the exit of the one
            // going out, which is why a press can read as start, loop and then end.
            String enter = overridden ? enterOf(sequenceOf(part, widget, state, key)) : null;
            String exit = Objects.equals(previousState, state) ? null : exitOf(sequenceOf(part, widget, previousState, key));
            boolean sequenced = handler instanceof SequencedOverrideHandler && (enter != null || exit != null);

            // already there (a state applied again after its look was taken off to be read, or two
            // states agreeing on a value): nothing to travel, unless something has to play first
            if (!sequenced && running == null && Objects.equals(handler.capture(entity), target)) {
                if (!overridden) part.baseSnapshot.remove(key);
                continue;
            }

            WidgetPartComponent.Transition transition = transitionOf(part, widget, state, key);
            if (transition == null && !overridden) transition = transitionOf(part, widget, previousState, key);

            if (running != null) removeTween(part, running);

            if (animate && transition != null && transition.duration > 0 && handler instanceof InterpolableOverrideHandler
                    && startTween(entity, part, key, (InterpolableOverrideHandler) handler, target, !overridden, transition)) {
                continue;
            }

            if (sequenced) ((SequencedOverrideHandler) handler).applySequence(entity, target, enter, exit);
            else handler.apply(entity, target);

            if (!overridden) part.baseSnapshot.remove(key);
        }

        part.appliedState = state;
        part.dirty = false;
    }

    /**
     * What a state overrides, its own overrides laid over the ones of the state it builds on, and so
     * on up: a checked hover shows the check mark of checked plus the tint of its own.
     *
     * @return null when the state, and everything it builds on, overrides nothing
     */
    private ObjectMap<String, String> overridesOf(WidgetPartComponent part, WidgetComponent widget, String state) {
        if (state == null) return null;
        if (widget == null || widget.parentOf(state) == null) return part.getOverrides(state);

        chain.clear();
        for (String link = state; link != null && chain.size < MAX_INHERITANCE; link = widget.parentOf(link)) {
            if (chain.contains(link, false)) break; // a state inheriting from itself
            chain.add(link);
        }

        mergedPatch.clear();
        for (int i = chain.size - 1; i >= 0; i--) {
            ObjectMap<String, String> overrides = part.getOverrides(chain.get(i));
            if (overrides != null) mergedPatch.putAll(overrides);
        }
        return mergedPatch.size == 0 ? null : mergedPatch;
    }

    /** The transition of the nearest state, from this one up, that says how the property travels. */
    private WidgetPartComponent.Transition transitionOf(WidgetPartComponent part, WidgetComponent widget, String state, String key) {
        int depth = 0;
        for (String link = state; link != null && depth++ < MAX_INHERITANCE; link = widget == null ? null : widget.parentOf(link)) {
            WidgetPartComponent.Transition transition = part.getTransition(link, key);
            if (transition != null) return transition;
        }
        return null;
    }

    /** What plays around the value, taken from the nearest state, from this one up, that says so. */
    private WidgetPartComponent.Sequence sequenceOf(WidgetPartComponent part, WidgetComponent widget, String state, String key) {
        int depth = 0;
        for (String link = state; link != null && depth++ < MAX_INHERITANCE; link = widget == null ? null : widget.parentOf(link)) {
            WidgetPartComponent.Sequence sequence = part.getSequence(link, key);
            if (sequence != null) return sequence;
        }
        return null;
    }

    private static String enterOf(WidgetPartComponent.Sequence sequence) {
        return sequence == null || sequence.enter.isEmpty() ? null : sequence.enter;
    }

    private static String exitOf(WidgetPartComponent.Sequence sequence) {
        return sequence == null || sequence.exit.isEmpty() ? null : sequence.exit;
    }

    private boolean startTween(int entity, WidgetPartComponent part, String key, InterpolableOverrideHandler handler,
                               String target, boolean toBase, WidgetPartComponent.Transition transition) {
        StateTween tween = tweenPool.obtain();
        if (!handler.parseChannels(target, tween.to)) {
            tweenPool.free(tween);
            return false;
        }
        handler.captureChannels(entity, tween.from);

        tween.key = key;
        tween.handler = handler;
        tween.target = target;
        tween.toBase = toBase;
        tween.duration = transition.duration;
        tween.interpolation = transition.getInterpolation();
        part.tweens.add(tween);
        return true;
    }

    private void advance(int entity, WidgetPartComponent part, float delta) {
        for (int i = part.tweens.size - 1; i >= 0; i--) {
            StateTween tween = part.tweens.get(i);
            tween.elapsed += delta;
            if (tween.elapsed >= tween.duration) {
                arrive(entity, part, tween);
                continue;
            }

            float alpha = tween.interpolation.apply(tween.elapsed / tween.duration);
            int channels = tween.handler.getChannelCount();
            for (int c = 0; c < channels; c++) {
                tween.current[c] = tween.from[c] + (tween.to[c] - tween.from[c]) * alpha;
            }
            tween.handler.applyChannels(entity, tween.current);
        }
    }

    private void finishTransitions(int entity, WidgetPartComponent part) {
        for (int i = part.tweens.size - 1; i >= 0; i--) {
            arrive(entity, part, part.tweens.get(i));
        }
    }

    /** The exact target is applied as written, not as the floats it was parsed to. */
    private void arrive(int entity, WidgetPartComponent part, StateTween tween) {
        tween.handler.apply(entity, tween.target);
        if (tween.toBase) part.baseSnapshot.remove(tween.key);
        removeTween(part, tween);
    }

    private StateTween findTween(WidgetPartComponent part, String key) {
        for (int i = 0; i < part.tweens.size; i++) {
            if (part.tweens.get(i).key.equals(key)) return part.tweens.get(i);
        }
        return null;
    }

    private void removeTween(WidgetPartComponent part, StateTween tween) {
        part.tweens.removeValue(tween, true);
        tweenPool.free(tween);
    }
}
