package games.rednblack.editor.renderer.utils.profiling;

import com.badlogic.gdx.utils.TimeUtils;

import java.util.Arrays;

/**
 * What each system of the engine costs, frame by frame.
 * <p>
 * An invocation strategy describes its systems once, in the order it calls them, and then brackets every
 * call. A system may run more than once in a frame, or not at all - a fixed timestep loop decides that
 * from the accumulator - so what is reported per frame is the total time and the number of runs behind it.
 * <p>
 * Alongside the systems it keeps the cost of keeping entity subscriptions in step, which is paid between
 * one system and the next and belongs to no system in particular.
 * <p>
 * Everything here runs on the thread that processes the engine and is not synchronized. It allocates only
 * while systems are being described.
 */
public class SystemProfiler {

    public static final int BUCKET_LOGIC = 0;
    public static final int BUCKET_INTERPOLATION = 1;
    public static final int BUCKET_RENDER = 2;

    private static final float NANOS_TO_MILLIS = 1f / 1000000f;
    /** Enough to settle a bar within a handful of frames, not enough to hide a spike. */
    private static final float SMOOTHING = 0.15f;

    private String[] names = new String[0];
    private int[] buckets = new int[0];

    private long[] nanos = new long[0];
    private int[] runs = new int[0];
    private float[] millis = new float[0];
    private float[] averageMillis = new float[0];
    private int[] lastRuns = new int[0];
    private float[] averageRuns = new float[0];

    private int count;
    private long start;

    private long syncNanos;
    private long syncStart;
    private float syncMillis;
    private float syncAverageMillis;

    /** Starts a fresh description. Every {@link #describeSystem(String, int)} after this one is kept. */
    public void describeBegin() {
        count = 0;
    }

    /**
     * Adds one system to the report, in the order it is called.
     *
     * @return the index to bracket that system's calls with
     */
    public int describeSystem(String name, int bucket) {
        if (count == names.length) grow(count + 8);

        names[count] = name;
        buckets[count] = bucket;
        nanos[count] = 0L;
        runs[count] = 0;
        millis[count] = 0f;
        averageMillis[count] = 0f;
        lastRuns[count] = 0;
        averageRuns[count] = 0f;
        return count++;
    }

    public void beginFrame() {
        Arrays.fill(nanos, 0, count, 0L);
        Arrays.fill(runs, 0, count, 0);
        syncNanos = 0L;
    }

    /** Opens the system that the next {@link #end(int)} closes. Calls do not nest. */
    public void begin() {
        start = TimeUtils.nanoTime();
    }

    public void end(int system) {
        nanos[system] += TimeUtils.nanoTime() - start;
        runs[system]++;
    }

    public void beginSync() {
        syncStart = TimeUtils.nanoTime();
    }

    public void endSync() {
        syncNanos += TimeUtils.nanoTime() - syncStart;
    }

    public void endFrame() {
        for (int i = 0; i < count; i++) {
            millis[i] = nanos[i] * NANOS_TO_MILLIS;
            averageMillis[i] += (millis[i] - averageMillis[i]) * SMOOTHING;
            averageRuns[i] += (runs[i] - averageRuns[i]) * SMOOTHING;
            lastRuns[i] = runs[i];
        }

        syncMillis = syncNanos * NANOS_TO_MILLIS;
        syncAverageMillis += (syncMillis - syncAverageMillis) * SMOOTHING;
    }

    public int systemCount() {
        return count;
    }

    public String name(int system) {
        return names[system];
    }

    public int bucket(int system) {
        return buckets[system];
    }

    /** What the system cost in the frame just closed. */
    public float millis(int system) {
        return millis[system];
    }

    /** The same, smoothed, so a sorted list does not jump around between frames. */
    public float averageMillis(int system) {
        return averageMillis[system];
    }

    /** How many times the system ran in the frame just closed. */
    public int runs(int system) {
        return lastRuns[system];
    }

    /**
     * How often the system runs, smoothed over frames. A fixed timestep system runs on some frames and not
     * on others - at 170 frames a second against a 60 Hz step, on one in three - so whether it ran in the
     * frame just closed says nothing about whether it is doing anything. This does.
     */
    public float averageRuns(int system) {
        return averageRuns[system];
    }

    /**
     * The smoothed costs added up, sync included: what the engine pass is made of.
     * <p>
     * A share has to be taken of this rather than of the frame's own average, or the two are smoothed over
     * different windows and a system that just got expensive reports more than all of the time there was.
     */
    public float totalAverageMillis() {
        float total = syncAverageMillis;
        for (int i = 0; i < count; i++) {
            total += averageMillis[i];
        }
        return total;
    }

    public float syncMillis() {
        return syncMillis;
    }

    public float syncAverageMillis() {
        return syncAverageMillis;
    }

    private void grow(int capacity) {
        names = Arrays.copyOf(names, capacity);
        buckets = Arrays.copyOf(buckets, capacity);
        nanos = Arrays.copyOf(nanos, capacity);
        runs = Arrays.copyOf(runs, capacity);
        millis = Arrays.copyOf(millis, capacity);
        averageMillis = Arrays.copyOf(averageMillis, capacity);
        lastRuns = Arrays.copyOf(lastRuns, capacity);
        averageRuns = Arrays.copyOf(averageRuns, capacity);
    }
}
