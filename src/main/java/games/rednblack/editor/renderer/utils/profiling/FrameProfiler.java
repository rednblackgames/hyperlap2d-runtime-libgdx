package games.rednblack.editor.renderer.utils.profiling;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Arrays;
import java.util.function.IntSupplier;

/**
 * Splits a frame into the phases that pay for it and keeps a rolling window of what each one cost,
 * in wall time and in GL work.
 * <p>
 * A phase is a <i>slot</i>. Slot {@link #SLOT_SCENE} is reserved for the pass that runs the engine -
 * the part of the frame a game is made of. Every other slot is the host's to name, through
 * {@link #defineSlot(int, String)}: an editor charges its panels to one, a game may charge a HUD or a
 * debug overlay drawn outside the engine. Whatever is left of the frame after the slots is reported by
 * {@link #untrackedMillis(int)}.
 * <p>
 * Nothing is measured, allocated or written while the profiler is off, and it allocates nothing at all
 * once it is on: a profiler that feeds the garbage collector is measuring itself.
 * <p>
 * Everything here runs on the rendering thread and is not synchronized.
 */
public final class FrameProfiler {

    /** The engine pass: what the scene itself costs. */
    public static final int SLOT_SCENE = 0;

    public static final int MAX_SLOTS = 8;

    /** Counters that only a {@link GLProfiler} can give, and only at a price. See {@link #setGLDetail}. */
    public static final int COUNTER_GL_CALLS = 0;
    public static final int COUNTER_GL_DRAW_CALLS = 1;
    public static final int COUNTER_SHADER_SWITCHES = 2;
    public static final int COUNTER_TEXTURE_BINDINGS = 3;
    public static final int COUNTER_VERTICES = 4;
    /** Batch flushes, read straight off the batch: the draw call count, and it costs nothing. */
    public static final int COUNTER_FLUSHES = 5;
    public static final int COUNTERS = 6;

    /** Making a window's GL context current. On some drivers this is the most expensive call in a loop. */
    public static final int LOOP_CONTEXT = 0;
    /** The main window's whole turn: its listener's render, and then its buffer swap. */
    public static final int LOOP_MAIN = 1;
    /** Every other window's turn. */
    public static final int LOOP_WINDOWS = 2;
    public static final int LOOP_EVENTS = 3;
    public static final int LOOP_RUNNABLES = 4;
    /** Whatever paces the loop: the frame rate limiter, or the idle sleep. */
    public static final int LOOP_WAIT = 5;
    public static final int LOOP_PHASES = 6;

    /** How many frames are kept. At 60 fps this is the last five seconds. */
    public static final int WINDOW = 300;

    private static final float NANOS_TO_MILLIS = 1f / 1000000f;

    private static final String[] labels = new String[MAX_SLOTS];

    //what the frame being measured has spent so far
    private static final long[] slotStart = new long[MAX_SLOTS];
    private static final long[] slotNanos = new long[MAX_SLOTS];
    /** One mark per slot, so a slot opened inside another still diffs against its own start. */
    private static final float[] glMark = new float[MAX_SLOTS * COUNTERS];
    private static final float[] slotGl = new float[MAX_SLOTS * COUNTERS];

    //the window, newest sample at head
    private static final float[] frameMillis = new float[WINDOW];
    private static final float[] periodMillis = new float[WINDOW];
    private static final float[] slotMillis = new float[MAX_SLOTS * WINDOW];
    private static final float[] glSamples = new float[MAX_SLOTS * COUNTERS * WINDOW];

    private static final long[] loopPending = new long[LOOP_PHASES];
    private static final float[] loopMillis = new float[LOOP_PHASES];

    private static GLProfiler glProfiler;
    /** True when the GL profiler was ours to enable, and so ours to switch off again. */
    private static boolean ownsGLProfiler;
    /** Where the free draw call count comes from, when the host has one to give. */
    private static IntSupplier flushCounter;

    private static boolean enabled;
    private static int slotCount;
    private static int framesPerSecond;
    private static int refreshRate;
    private static long frameStart;
    private static long previousFrameStart;
    private static long period;
    private static int head = -1;
    private static int sampleCount;

    static {
        labels[SLOT_SCENE] = "Scene";
        slotCount = 1;
    }

    private FrameProfiler() {
    }

    /**
     * Names a slot and makes it part of the report. Slots are expected to be declared once, before
     * profiling starts, and to keep their meaning for as long as it runs.
     */
    public static void defineSlot(int slot, String label) {
        if (slot < 0 || slot >= MAX_SLOTS)
            throw new IllegalArgumentException("Slot out of range: " + slot);

        labels[slot] = label;
        if (slot >= slotCount) slotCount = slot + 1;
    }

    /** How many slots have been declared, {@link #SLOT_SCENE} included. */
    public static int slotCount() {
        return slotCount;
    }

    public static String label(int slot) {
        return labels[slot];
    }

    /**
     * Turns measuring on or off. Turning it on clears the window, so what is read afterwards is never a mix
     * of two sessions. Timing costs a few reads of the clock a frame; the counters that cost more than that
     * are behind {@link #setGLDetail(boolean)} and are off.
     */
    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;

        if (value) reset();
        else setGLDetail(false);
    }

    /**
     * Turns on the counters that need a {@link GLProfiler}: shader switches, texture bindings and vertices.
     * <p>
     * It is off by default, and it should stay off unless someone is looking at those three numbers. The
     * profiler asks the driver for an error after <i>every</i> GL call, and that question is answered
     * synchronously: on a GLES or ANGLE driver it can cost more than the whole rest of the frame. Draw calls
     * do not need it - they are counted off the batch for nothing.
     * <p>
     * Call it with the window being measured current: the profiler wraps whatever graphics that is.
     */
    public static void setGLDetail(boolean value) {
        if (value == (glProfiler != null && glProfiler.isEnabled())) return;

        if (value) {
            if (glProfiler == null && Gdx.graphics != null) {
                glProfiler = new GLProfiler(Gdx.graphics);
                ownsGLProfiler = true;
            }
            if (glProfiler != null) glProfiler.enable();
        } else if (glProfiler != null && ownsGLProfiler) {
            glProfiler.disable();
        }
    }

    public static boolean isGLDetail() {
        return glProfiler != null && glProfiler.isEnabled();
    }

    /**
     * Hands over the number of times the host's batches have flushed, which is what a draw call is. Read at
     * the same boundaries as everything else, so each slot is charged with the flushes it caused.
     */
    public static void setFlushCounter(IntSupplier counter) {
        flushCounter = counter;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Hands over a GL profiler the host already keeps. Its enabled state stays the host's business; pass
     * {@code null} to go back to one of ours.
     */
    public static void setGLProfiler(GLProfiler profiler) {
        if (glProfiler != null && ownsGLProfiler) glProfiler.disable();
        glProfiler = profiler;
        ownsGLProfiler = false;
    }

    /**
     * Closes one turn of the application's loop and opens the next. What the phases add up to is the whole
     * of a frame, including everything that happens with no window rendering at all - which is the only
     * place a frame can go when the render itself is fast and the frame rate is not.
     */
    public static void beginLoop() {
        if (!enabled) return;

        for (int phase = 0; phase < LOOP_PHASES; phase++) {
            loopMillis[phase] += (loopPending[phase] * NANOS_TO_MILLIS - loopMillis[phase]) * 0.1f;
            loopPending[phase] = 0L;
        }
    }

    /** @return a timestamp to measure the next phase from, or 0 while measuring is off */
    public static long now() {
        return enabled ? TimeUtils.nanoTime() : 0L;
    }

    /** Charges the time since {@code from} to a phase of the loop. */
    public static long mark(int phase, long from) {
        if (!enabled) return 0L;
        if (from == 0L) return TimeUtils.nanoTime();

        long now = TimeUtils.nanoTime();
        loopPending[phase] += now - from;
        return now;
    }

    /** What that phase of the loop costs, smoothed. */
    public static float loopPhaseMillis(int phase) {
        return loopMillis[phase];
    }

    /** Drops every sample taken so far. */
    public static void reset() {
        Arrays.fill(frameMillis, 0f);
        Arrays.fill(periodMillis, 0f);
        Arrays.fill(slotMillis, 0f);
        Arrays.fill(glSamples, 0f);
        Arrays.fill(slotNanos, 0L);
        Arrays.fill(slotGl, 0f);
        Arrays.fill(loopPending, 0L);
        Arrays.fill(loopMillis, 0f);
        head = -1;
        sampleCount = 0;
    }

    /** Opens a frame. Whatever is measured from here to {@link #endFrame()} becomes one sample. */
    public static void beginFrame() {
        if (!enabled) return;

        //taken here, inside the frame being measured, because Gdx.graphics is whichever window is
        //rendering: read from anywhere else - another window, another thread - it is another window's rate
        if (refreshRate == 0 && Gdx.graphics != null && Gdx.graphics.getDisplayMode() != null) {
            refreshRate = Gdx.graphics.getDisplayMode().refreshRate;
        }

        if (glProfiler != null && glProfiler.isEnabled()) glProfiler.reset();
        Arrays.fill(slotNanos, 0L);
        Arrays.fill(slotGl, 0f);
        Arrays.fill(glMark, 0f);

        frameStart = TimeUtils.nanoTime();
        //one frame to the next, so what the loop spends outside this render can be seen at all
        period = previousFrameStart == 0 ? 0 : frameStart - previousFrameStart;
        previousFrameStart = frameStart;
    }

    /**
     * Opens a slot. A slot may be opened and closed several times within one frame - a fixed timestep
     * loop does exactly that - and what the frame reports is the sum. Slots may also be nested, and then
     * the inner one is counted in both: a host that wants them apart subtracts.
     */
    public static void begin(int slot) {
        if (!enabled) return;

        markGL(slot);
        slotStart[slot] = TimeUtils.nanoTime();
    }

    public static void end(int slot) {
        if (!enabled) return;

        slotNanos[slot] += TimeUtils.nanoTime() - slotStart[slot];
        accumulateGL(slot);
    }

    /** Closes the frame and writes it into the window. */
    public static void endFrame() {
        if (!enabled) return;

        long total = TimeUtils.nanoTime() - frameStart;

        head = head + 1 == WINDOW ? 0 : head + 1;
        frameMillis[head] = total * NANOS_TO_MILLIS;
        periodMillis[head] = period * NANOS_TO_MILLIS;

        for (int slot = 0; slot < slotCount; slot++) {
            slotMillis[slot * WINDOW + head] = slotNanos[slot] * NANOS_TO_MILLIS;
            for (int counter = 0; counter < COUNTERS; counter++) {
                glSamples[(slot * COUNTERS + counter) * WINDOW + head] = slotGl[slot * COUNTERS + counter];
            }
        }

        if (sampleCount < WINDOW) sampleCount++;
        framesPerSecond = Gdx.graphics == null ? 0 : Gdx.graphics.getFramesPerSecond();
    }

    /**
     * The rate of the window being measured, as its own graphics reports it. Read through here rather than
     * from {@code Gdx.graphics}, which answers for whoever is rendering at the time.
     */
    public static int framesPerSecond() {
        return framesPerSecond;
    }

    /** The refresh rate of the display the measured window is on, or 0 before the first frame. */
    public static int refreshRate() {
        return refreshRate;
    }

    /** How many frames of the window are filled, at most {@link #WINDOW}. */
    public static int sampleCount() {
        return sampleCount;
    }

    /** The whole frame, in milliseconds. {@code framesAgo} counts back from the newest sample, 0. */
    public static float frameMillis(int framesAgo) {
        if (framesAgo < 0 || framesAgo >= sampleCount) return 0f;
        return frameMillis[indexOf(framesAgo)];
    }

    /**
     * One frame to the next, start to start. Everything the application does that is not this window's
     * render lands in the difference: another window drawing, the buffer swaps, the compositor, the sleep
     * that paces the loop.
     */
    public static float periodMillis(int framesAgo) {
        if (framesAgo < 0 || framesAgo >= sampleCount) return 0f;
        return periodMillis[indexOf(framesAgo)];
    }

    public static float slotMillis(int slot, int framesAgo) {
        if (framesAgo < 0 || framesAgo >= sampleCount) return 0f;
        return slotMillis[slot * WINDOW + indexOf(framesAgo)];
    }

    /** What the frame spent outside every declared slot: clearing, swapping, the driver's own time. */
    public static float untrackedMillis(int framesAgo) {
        if (framesAgo < 0 || framesAgo >= sampleCount) return 0f;

        int index = indexOf(framesAgo);
        float rest = frameMillis[index];
        for (int slot = 0; slot < slotCount; slot++) {
            rest -= slotMillis[slot * WINDOW + index];
        }
        return rest > 0f ? rest : 0f;
    }

    /**
     * A GL counter charged to one slot. Batched drawing pays at the flush, so the slot that flushes is
     * the slot that is billed - which is also the slot that caused the work.
     */
    public static float counter(int slot, int counter, int framesAgo) {
        if (framesAgo < 0 || framesAgo >= sampleCount) return 0f;
        return glSamples[(slot * COUNTERS + counter) * WINDOW + indexOf(framesAgo)];
    }

    private static int indexOf(int framesAgo) {
        int index = head - framesAgo;
        return index < 0 ? index + WINDOW : index;
    }

    private static void markGL(int slot) {
        int base = slot * COUNTERS;
        if (flushCounter != null) glMark[base + COUNTER_FLUSHES] = flushCounter.getAsInt();
        if (glProfiler == null || !glProfiler.isEnabled()) return;

        glMark[base + COUNTER_GL_CALLS] = glProfiler.getCalls();
        glMark[base + COUNTER_GL_DRAW_CALLS] = glProfiler.getDrawCalls();
        glMark[base + COUNTER_SHADER_SWITCHES] = glProfiler.getShaderSwitches();
        glMark[base + COUNTER_TEXTURE_BINDINGS] = glProfiler.getTextureBindings();
        glMark[base + COUNTER_VERTICES] = glProfiler.getVertexCount().total;
    }

    private static void accumulateGL(int slot) {
        int base = slot * COUNTERS;
        if (flushCounter != null) slotGl[base + COUNTER_FLUSHES] += flushCounter.getAsInt() - glMark[base + COUNTER_FLUSHES];
        if (glProfiler == null || !glProfiler.isEnabled()) return;

        slotGl[base + COUNTER_GL_CALLS] += glProfiler.getCalls() - glMark[base + COUNTER_GL_CALLS];
        slotGl[base + COUNTER_GL_DRAW_CALLS] += glProfiler.getDrawCalls() - glMark[base + COUNTER_GL_DRAW_CALLS];
        slotGl[base + COUNTER_SHADER_SWITCHES] += glProfiler.getShaderSwitches() - glMark[base + COUNTER_SHADER_SWITCHES];
        slotGl[base + COUNTER_TEXTURE_BINDINGS] += glProfiler.getTextureBindings() - glMark[base + COUNTER_TEXTURE_BINDINGS];
        slotGl[base + COUNTER_VERTICES] += glProfiler.getVertexCount().total - glMark[base + COUNTER_VERTICES];
    }
}
