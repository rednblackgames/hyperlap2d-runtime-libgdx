package games.rednblack.editor.renderer.systems.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import java.util.Stack;

public class FrameBufferManager {
    private final Stack<FrameBuffer> stack = new Stack<>();

    public void begin(FrameBuffer buffer) {
        if (!stack.isEmpty()) {
            stack.peek().end();
        }
        stack.push(buffer).begin();
    }

    public void end(FrameBuffer buffer) {
        if (stack.isEmpty())
            return;

        stack.pop().end();
        if (!stack.isEmpty()) {
            stack.peek().begin();
        }
    }

    public boolean isActive(FrameBuffer buffer) {
        return !stack.isEmpty() && stack.peek() == buffer;
    }
}
