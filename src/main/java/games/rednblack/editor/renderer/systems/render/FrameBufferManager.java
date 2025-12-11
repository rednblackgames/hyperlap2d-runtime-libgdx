package games.rednblack.editor.renderer.systems.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Stack;

public class FrameBufferManager implements Disposable {
    private final ObjectMap<String, FBOContainer> frameBuffers;
    private final Stack<FBOContainer> stack;
    private final int samples;

    public static int GL_MAX_TEXTURE_SIZE = 4096;

    public FrameBufferManager(int samples) {
        IntBuffer intBuffer = BufferUtils.newIntBuffer(16);
        Gdx.gl20.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intBuffer);
        GL_MAX_TEXTURE_SIZE = intBuffer.get();
        frameBuffers = new ObjectMap<>();
        stack = new Stack<>();
        this.samples = Gdx.gl30 != null ? samples : 0;
    }

    public void createFBO(String tag, int width, int height, boolean replace) {
        createFBO(tag, Pixmap.Format.RGBA8888, width, height, false, false, replace);
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

        FBOContainer container = new FBOContainer(format, width, height, hasDepth, hasStencil, samples);
        frameBuffers.put(tag, container);
    }

    public void createIfNotExists(String tag, int width, int height, boolean hasDepth, boolean hasStencil) {
        FBOContainer container = frameBuffers.get(tag);
        if (container == null) {
            createFBO(tag, width, height);
        } else if (container.getWidth() != width || container.getHeight() != height) {
            createFBO(tag, width, height, hasDepth, hasStencil, true);
        }
    }

    public void begin(String tag) {
        if (!stack.isEmpty()) {
            stack.peek().end();
        }

        FBOContainer container = frameBuffers.get(tag);
        if (container == null)
            throw new IllegalArgumentException("FBO '" + tag + "' does not exists.");

        stack.push(container).begin();
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
        FBOContainer container = frameBuffers.get(tag);
        if (container == null)
            return false;

        return !stack.isEmpty() && stack.peek() == container;
    }

    public void dispose(String tag) {
        FBOContainer container = frameBuffers.get(tag);
        if (container == null)
            return;

        container.dispose();
        frameBuffers.remove(tag);
    }

    @Override
    public void dispose() {
        for (FBOContainer container : frameBuffers.values())
            container.dispose();
        frameBuffers.clear();
    }

    public Texture getColorBufferTexture(String tag) {
        FBOContainer container = frameBuffers.get(tag);
        if (container == null)
            throw new IllegalArgumentException("FBO '" + tag + "' does not exists.");

        return container.getTexture();
    }

    private static class FBOContainer implements Disposable {
        private final FrameBuffer standardFbo;
        private FrameBuffer msaaFbo;
        private final boolean isMsaa;

        private final int width;
        private final int height;

        public FBOContainer(Pixmap.Format format, int width, int height, boolean hasDepth, boolean hasStencil, int samples) {
            this.width = width;
            this.height = height;
            this.isMsaa = samples > 0;

            FrameBufferBuilder standardBuilder = new FrameBufferBuilder(width, height);
            standardBuilder.addBasicColorTextureAttachment(format);
            if (hasDepth) standardBuilder.addBasicDepthRenderBuffer();
            if (hasStencil) standardBuilder.addBasicStencilRenderBuffer();
            this.standardFbo = standardBuilder.build();

            if (isMsaa) {
                FrameBufferBuilder msaaBuilder = new FrameBufferBuilder(width, height);
                int internalFormat = getGlInternalFormat(format);
                msaaBuilder.addColorRenderBuffer(internalFormat);
                if (hasDepth) msaaBuilder.addBasicDepthRenderBuffer();
                if (hasStencil) msaaBuilder.addBasicStencilRenderBuffer();
                msaaBuilder.samples = samples;
                this.msaaFbo = msaaBuilder.build();
            }
        }

        private static int getGlInternalFormat(Pixmap.Format format) {
            switch (format) {
                case RGBA8888:
                    return GL30.GL_RGBA8;
                case RGB888:
                    return GL30.GL_RGB8;
                case RGB565:
                    return GL30.GL_RGB565;
                case RGBA4444:
                    return GL30.GL_RGBA4;
                case Alpha:
                    return GL30.GL_ALPHA;
                default:
                    return GL30.GL_RGBA8;
            }
        }

        public void begin() {
            if (isMsaa) {
                msaaFbo.begin();
            } else {
                standardFbo.begin();
            }
        }

        public void end() {
            if (isMsaa) {
                msaaFbo.end();
                msaaFbo.transfer(standardFbo);
            } else {
                standardFbo.end();
            }
        }

        public Texture getTexture() {
            return standardFbo.getColorBufferTexture();
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }

        @Override
        public void dispose() {
            if (standardFbo != null) standardFbo.dispose();
            if (msaaFbo != null) msaaFbo.dispose();
        }
    }
}