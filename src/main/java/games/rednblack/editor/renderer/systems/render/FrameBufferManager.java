package games.rednblack.editor.renderer.systems.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import java.util.HashMap;
import java.util.Stack;

public class FrameBufferManager {

    private static FrameBufferManager sInstance = null;

    public static FrameBufferManager getInstance() {
        if (sInstance == null)
            sInstance = new FrameBufferManager();
        return sInstance;
    }

    private final HashMap<String, FrameBuffer> frameBuffers;
    private final Stack<FrameBuffer> stack;

    private FrameBufferManager() {
        frameBuffers = new HashMap<>();
        stack = new Stack<>();
    }

    public void createFBO(String tag, int width, int height, boolean replace) {
        createFBO(tag, Pixmap.Format.RGBA8888, width, height, false, replace);
    }

    public void createFBO(String tag, int width, int height) {
        createFBO(tag, Pixmap.Format.RGBA8888, width, height, false, false);
    }

    public void createFBO(String tag, Pixmap.Format format, int width, int height, boolean hasDepth, boolean replace) {
        if (frameBuffers.get(tag) != null) {
            if (replace)
                dispose(tag);
            else
                throw new IllegalArgumentException("FBO '" + tag + "' already exists.");
        }

        FrameBuffer fbo = new FrameBuffer(format, width, height, hasDepth);
        frameBuffers.put(tag, fbo);
    }

    public void begin(String tag) {
        if (!stack.isEmpty()) {
            stack.peek().end();
        }
        FrameBuffer buffer = frameBuffers.get(tag);
        if (buffer == null)
            throw new IllegalArgumentException("FBO '" + tag + "' does not exists.");

        stack.push(buffer).begin();
    }

    public void endCurrent() {
        if (stack.isEmpty())
            return;

        stack.pop().end();
        if (!stack.isEmpty()) {
            stack.peek().begin();
        }
    }

    public boolean isActive(String tag) {
        FrameBuffer buffer = frameBuffers.get(tag);
        if (buffer == null)
            return false;

        return !stack.isEmpty() && stack.peek() == buffer;
    }

    public void dispose(String tag) {
        FrameBuffer buffer = frameBuffers.get(tag);
        if (buffer == null)
            throw new IllegalArgumentException("FBO '" + tag + "' does not exists.");

        buffer.dispose();
        frameBuffers.remove(tag);
    }

    public Texture getColorBufferTexture(String tag) {
        FrameBuffer buffer = frameBuffers.get(tag);
        if (buffer == null)
            throw new IllegalArgumentException("FBO '" + tag + "' does not exists.");

        return buffer.getColorBufferTexture();
    }
}
