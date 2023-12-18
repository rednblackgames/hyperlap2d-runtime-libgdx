package games.rednblack.editor.renderer.systems.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Stack;

public class FrameBufferManager {
    private final HashMap<String, FrameBuffer> frameBuffers;
    private final Stack<FrameBuffer> stack;
    public static int GL_MAX_TEXTURE_SIZE = 4096;

    public FrameBufferManager() {
        IntBuffer intBuffer = BufferUtils.newIntBuffer(16);
        Gdx.gl20.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intBuffer);
        GL_MAX_TEXTURE_SIZE = intBuffer.get();
        frameBuffers = new HashMap<>();
        stack = new Stack<>();
    }

    public void createFBO(String tag, int width, int height, boolean replace) {
        createFBO(tag, Pixmap.Format.RGBA8888, width, height, false, false,replace);
    }

    public void createFBO(String tag, int width, int height) {
        createFBO(tag, Pixmap.Format.RGBA8888, width, height, false, false, false);
    }

    public void createFBO(String tag, int width, int height, boolean hasDepth, boolean hasStencil, boolean replace) {
        createFBO(tag, Pixmap.Format.RGBA8888, width, height, hasDepth, hasStencil, replace);
    }

    public void createFBO(String tag, Pixmap.Format format, int width, int height, boolean hasDepth, boolean hasStencil, boolean replace) {
        if (frameBuffers.get(tag) != null) {
            if (replace)
                dispose(tag);
            else
                throw new IllegalArgumentException("FBO '" + tag + "' already exists.");
        }

        // Check if either width or height exceeds max_size
        if (width > GL_MAX_TEXTURE_SIZE || height > GL_MAX_TEXTURE_SIZE) {
            // Calculate the aspect ratio
            double aspectRatio = (double) width / height;

            // Adjust dimensions while maintaining the aspect ratio
            if (width > height) {
                width = GL_MAX_TEXTURE_SIZE;
                height = (int) (GL_MAX_TEXTURE_SIZE / aspectRatio);
            } else {
                height = GL_MAX_TEXTURE_SIZE;
                width = (int) (GL_MAX_TEXTURE_SIZE * aspectRatio);
            }
        }

        FrameBuffer fbo = new FrameBuffer(format, width, height, hasDepth, hasStencil);
        frameBuffers.put(tag, fbo);
    }

    public void createIfNotExists(String tag, int width, int height, boolean hasDepth, boolean hasStencil) {
        FrameBuffer buffer = frameBuffers.get(tag);
        if (buffer == null) {
            createFBO(tag, width, height);
        } else if (buffer.getWidth() != width || buffer.getHeight() != height) {
                createFBO(tag, width, height, hasDepth, hasStencil, true);
        }
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
            return;

        buffer.dispose();
        frameBuffers.remove(tag);
    }

    public void disposeAll() {
        for (FrameBuffer buffer : frameBuffers.values())
            buffer.dispose();
        frameBuffers.clear();
    }

    public Texture getColorBufferTexture(String tag) {
        FrameBuffer buffer = frameBuffers.get(tag);
        if (buffer == null)
            throw new IllegalArgumentException("FBO '" + tag + "' does not exists.");

        return buffer.getColorBufferTexture();
    }
}
