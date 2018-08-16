package demo.gljfx;

import com.sun.glass.events.KeyEvent;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.stage.EmbeddedWindow;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class GLFXStage {
    private static final Logger LOGGER = LoggerFactory.getLogger(GLFXStage.class);

    static {
        PlatformImpl.startup(() -> LOGGER.debug("JavaFX initialized!"));
    }

    private static final class MousePos {
        private double x, y;

        private MousePos(final double x, final double y) {
            this.x = x;
            this.y = y;
        }
    }

    private GLFXDNDHandler dndHandler;
    private GLFXContextMenuHandler contextMenuHandler;
    private int windowWidth;
    private int windowHeight;
    private int width;
    private int height;
    private boolean focus = true;
    private boolean applyCursors = true;
    private CursorType cursorType = CursorType.DEFAULT;
    private boolean shift, alt, ctrl, meta;
    private EmbeddedWindow stage;
    private EmbeddedSceneInterface emScene;
    private EmbeddedStageInterface emStage;
    private float scaleFactor = 1.0F;
    private int texture;
    private ByteBuffer tBuffer;
    private final int vPos;
    private final int vUVs;
    private final int vao;
    private boolean needsUpdate;
    private boolean needsRecreate = true;
    private int oldEmX;
    private int oldEmY;
    private boolean leftButton, rightButton, middleButton;
    private int mouseX, mouseY, mouseAbsX, mouseAbsY;
    private final HostInterface hostContainer = new HostInterface() {

        @Override
        public void setEmbeddedStage(EmbeddedStageInterface embeddedStage) {
            if (GLFXStage.this.width > 0 && GLFXStage.this.height > 0) {
                embeddedStage.setSize(GLFXStage.this.width, GLFXStage.this.height);
            }

            embeddedStage.setLocation(0, 0);
            GLFXStage.this.emStage = embeddedStage;
        }

        @Override
        public void setEmbeddedScene(EmbeddedSceneInterface embeddedScene) {
            if (GLFXStage.this.emScene == embeddedScene) {
                return;
            }

            if (GLFXStage.this.width > 0 && GLFXStage.this.height > 0) {
                embeddedScene.setSize(GLFXStage.this.width, GLFXStage.this.height);
            }

            embeddedScene.setPixelScaleFactor(GLFXStage.this.scaleFactor);
            GLFXStage.this.emScene = embeddedScene;
            GLFXStage.this.dndHandler = new GLFXDNDHandler(embeddedScene);
            GLFXStage.this.contextMenuHandler = new GLFXContextMenuHandler(embeddedScene, GLFXStage.this);
        }

        @Override
        public boolean requestFocus() {
            return false;
        }

        @Override
        public boolean traverseFocusOut(boolean forward) {
            return false;
        }

        @Override
        public void repaint() {
            GLFXStage.this.needsUpdate = true;
        }

        @Override
        public void setPreferredSize(int width, int height) {

        }

        @Override
        public void setEnabled(boolean enabled) {

        }

        @Override
        public void setCursor(CursorFrame cursorFrame) {
            GLFXStage.this.cursorType = cursorFrame.getCursorType();

            if (GLFXStage.this.applyCursors) {
                switch (GLFXStage.this.cursorType) {
                    case DEFAULT:
                }
            }
        }

        @Override
        public boolean grabFocus() {
            return false;
        }

        @Override
        public void ungrabFocus() {

        }
    };

    public GLFXStage(final int width, final int height) {
        assert width >= 1 : "Width must be at least 1!";
        assert height >= 1 : "Height must be at least 1!";

        this.resize(width, height);

        this.vPos = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(this.vPos, new float[] {0F, 0F, 0F, 1F, 1F, 0F, 1F, 1F}, 0);

        this.vUVs = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(this.vUVs, new float[] {0F, 0F, 0F, 1F, 1F, 0F, 1F, 1F}, 0);

        this.vao = GL45C.glCreateVertexArrays();
        GL45C.glEnableVertexArrayAttrib(this.vao, 0);
        GL45C.glVertexArrayAttribFormat(this.vao, 0, 2, GL45C.GL_FLOAT, false, 0);
        GL45C.glVertexArrayAttribBinding(this.vao, 0, 0);
        GL45C.glVertexArrayVertexBuffer(this.vao, 0, this.vPos, 0, 2 * Float.BYTES);
        GL45C.glEnableVertexArrayAttrib(this.vao, 1);
        GL45C.glVertexArrayAttribFormat(this.vao, 1, 2, GL45C.GL_FLOAT, false, 0);
        GL45C.glVertexArrayAttribBinding(this.vao, 1, 1);
        GL45C.glVertexArrayVertexBuffer(this.vao, 1, this.vUVs, 0, 2 * Float.BYTES);
    }

    public void setStageAbsLocation(int x, int y) {
        if (this.oldEmX == x && this.oldEmY == y) {
            return;
        }

        this.emStage.setLocation(x, y);
        this.oldEmX = x;
        this.oldEmY = y;
    }

    private void setSceneImpl(final Scene scene) {
        if ((this.stage != null) && (scene == null)) {
            this.stage.hide();
            this.stage = null;
        }

        if ((this.stage == null) && (scene != null)) {
            this.stage = new EmbeddedWindow(this.hostContainer);
        }

        if ((this.stage != null)) {

            this.stage.setScene(scene);

            if (!this.stage.isShowing()) {
                this.stage.show();

            }
        }
    }

    public Optional<Scene> getScene() {
        if (this.stage != null) {
            return Optional.of(this.stage.getScene());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Parent> getRootNode() {
        return this.getScene().map(Scene::getRoot);
    }

    public ObservableList<Node> getRootChildren() {
        final Optional<Parent> rootNode = this.getRootNode();

        if (rootNode.isPresent()) {
            final Parent root = rootNode.get();

            if (root instanceof Group) {
                return ((Group) root).getChildren();
            } else if (root instanceof Pane) {
                return ((Pane) root).getChildren();
            }
        }

        return FXCollections.emptyObservableList();
    }

    public void setScene(final Scene scene) {
        if (Toolkit.getToolkit().isFxUserThread()) {
            this.setSceneImpl(scene);
        } else {
            final CountDownLatch initLatch = new CountDownLatch(1);

            Platform.runLater(() -> {
                this.setSceneImpl(scene);
                initLatch.countDown();
            });

            try {
                initLatch.await();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void setParentWindowSize(int newWidth, int newHeight) {
        assert newWidth > 0;
        assert newHeight > 0;

        this.windowWidth = newWidth;
        this.windowHeight = newHeight;
    }

    public void resize(int newWidth, int newHeight) {
        assert newWidth > 0;
        assert newHeight > 0;

        this.width = newWidth;
        this.height = newHeight;

        if (this.emScene != null) {
            this.emScene.setSize(newWidth, newHeight);
        }

        if (this.emStage != null) {
            this.emStage.setSize(newWidth, newHeight);
        }

        this.needsRecreate = true;
    }

    public void mouseScrollEvent(final double dx, final double dy) {
        this.emScene.mouseEvent(
                AbstractEvents.MOUSEEVENT_WHEEL, AbstractEvents.MOUSEEVENT_NONE_BUTTON,
                leftButton, middleButton, rightButton,
                mouseX, mouseY, mouseX, mouseY,
                shift, ctrl, alt, meta, -(int) dy, false);
    }

    private void updateTexture() {
        if (this.emScene == null) {
            return;
        }

        final int neededSize = this.width * this.height * Integer.BYTES;

        if (neededSize == 0) {
            return;
        }

        if (this.tBuffer == null || neededSize > this.tBuffer.capacity()) {
            if (this.tBuffer != null) {
                MemoryUtil.memFree(this.tBuffer);
                this.tBuffer = null;
            }

            this.tBuffer = MemoryUtil.memAlloc(neededSize);
        }

        this.tBuffer.clear();
        this.emScene.getPixels(this.tBuffer.asIntBuffer(), this.width, this.height);
        this.tBuffer.limit(neededSize);
    }

    private int getAndUpdateTexture() {
        if (this.needsRecreate) {
            if (this.width == 0 || this.height == 0) {
                return this.texture;
            }

            if (this.texture != 0) {
                GL45C.glDeleteTextures(this.texture);
                this.texture = 0;
            }

            this.texture = GL45C.glCreateTextures(GL45C.GL_TEXTURE_2D);
            GL45C.glTextureParameteri(this.texture, GL45C.GL_TEXTURE_WRAP_S, GL45C.GL_CLAMP_TO_EDGE);
            GL45C.glTextureParameteri(this.texture, GL45C.GL_TEXTURE_WRAP_T, GL45C.GL_CLAMP_TO_EDGE);
            GL45C.glTextureParameteri(this.texture, GL45C.GL_TEXTURE_MAG_FILTER, GL45C.GL_LINEAR);
            GL45C.glTextureParameteri(this.texture, GL45C.GL_TEXTURE_MIN_FILTER, GL45C.GL_LINEAR);
            GL45C.glTextureStorage2D(this.texture, 1, GL45C.GL_RGBA8, this.width, this.height);
            this.needsRecreate = false;
            this.needsUpdate = true;
        }

        if (this.needsUpdate) {
            this.updateTexture();

            GL45C.glTextureSubImage2D(this.texture, 0, 0, 0, this.width, this.height, GL45C.GL_BGRA, GL45C.GL_UNSIGNED_BYTE, this.tBuffer);
            this.needsUpdate = false;
        }

        return this.texture;
    }

    public void drawGL() {
        try (MemoryStack mem = MemoryStack.stackPush()) {
            final FloatBuffer proj = mem.mallocFloat(16);

            final Matrix4f projection = new Matrix4f()
                    .ortho(0F, ((float) this.windowWidth) / ((float) this.width), ((float) this.windowHeight) / ((float) this.height), 0F, -1F, 1F);

            projection.get(proj);

            final Program program = Program.getInstance();

            GL45C.glUseProgram(program.handle);
            GL45C.glUniformMatrix4fv(program.projectionBinding, false, proj);
            GL45C.glUniform1i(program.textureBinding, 0);
            GL45C.glBindTextureUnit(0, this.getAndUpdateTexture());
            GL45C.glBindVertexArray(this.vao);

            GL45C.glDrawArrays(GL45C.GL_TRIANGLE_STRIP, 0, 4);
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isFocus() {
        return this.focus;
    }

    public void setFocus(final boolean focus) {
        if (this.focus == focus) {
            return;
        }

        this.focus = focus;

        if (!focus) {
            this.shift = false;
            this.alt = false;
            this.ctrl = false;
            this.meta = false;


        }
    }

    public void free() {
        GL45C.glDeleteTextures(this.texture);
        GL45C.glDeleteVertexArrays(this.vao);
        GL45C.glDeleteBuffers(this.vPos);
        GL45C.glDeleteBuffers(this.vUVs);
    }

    public void mouseMoveEvent(double x, double y) {
        if (this.emScene == null) {
            return;
        }

        this.mouseX = (int) x;
        this.mouseY = (int) y;
        this.mouseAbsX = this.mouseX + this.oldEmX;
        this.mouseAbsY = this.mouseY + this.oldEmY;

        if (this.leftButton) {
            this.emScene.mouseEvent(
                    AbstractEvents.MOUSEEVENT_DRAGGED, AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON,
                    true, false, false,
                    this.mouseX, this.mouseY, this.mouseAbsX, this.mouseAbsY,
                    this.shift, this.ctrl, this.alt, this.meta,
                    0, false);
        } else if (this.rightButton) {
            this.emScene.mouseEvent(
                    AbstractEvents.MOUSEEVENT_DRAGGED, AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON,
                    false, false, true,
                    this.mouseX, this.mouseY, this.mouseAbsX, this.mouseAbsY,
                    this.shift, this.ctrl, this.alt, this.meta,
                    0, false);
        } else if (this.middleButton) {
            this.emScene.mouseEvent(
                    AbstractEvents.MOUSEEVENT_DRAGGED, AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON,
                    false, true, false,
                    this.mouseX, this.mouseY, this.mouseAbsX, this.mouseAbsY,
                    this.shift, this.ctrl, this.alt, this.meta,
                    0, false);
        } else {
            this.emScene.mouseEvent(
                    AbstractEvents.MOUSEEVENT_MOVED, AbstractEvents.MOUSEEVENT_NONE_BUTTON,
                    false, false, false,
                    this.mouseX, this.mouseY, this.mouseAbsX, this.mouseAbsY,
                    this.shift, this.ctrl, this.alt, this.meta,
                    0, false);
        }

        this.dndHandler.mousePosition(this.mouseX, this.mouseY, this.mouseAbsX, this.mouseAbsY);
    }

    public void keyCharEvent(int c) {
        if (!this.focus) {
            return;
        }

        final int mods = this.ctrl ? AbstractEvents.MODIFIER_CONTROL : 0;

        this.emScene.keyEvent(AbstractEvents.KEYEVENT_TYPED, KeyEvent.VK_UNDEFINED, new char[] {(char)c}, mods);
    }

    public void keyEvent(int key, int scanCode, int action, int mods) {
        if (!focus) {
            return;
        }

        int keyId = -1;

        switch (key) {
            case GLFW.GLFW_KEY_ENTER:
                keyId = KeyEvent.VK_ENTER;
                break;
            case GLFW.GLFW_KEY_BACKSPACE:
                keyId = KeyEvent.VK_BACKSPACE;
                break;
            case GLFW.GLFW_KEY_LEFT:
                keyId = KeyEvent.VK_LEFT;
                break;
            case GLFW.GLFW_KEY_RIGHT:
                keyId = KeyEvent.VK_RIGHT;
                break;
            case GLFW.GLFW_KEY_UP:
                keyId = KeyEvent.VK_UP;
                break;
            case GLFW.GLFW_KEY_DOWN:
                keyId = KeyEvent.VK_DOWN;
                break;
            case GLFW.GLFW_KEY_TAB:
                keyId = KeyEvent.VK_TAB;
                break;
            case GLFW.GLFW_KEY_DELETE:
                keyId = KeyEvent.VK_DELETE;
                break;
            case GLFW.GLFW_KEY_HOME:
                keyId = KeyEvent.VK_HOME;
                break;
            case GLFW.GLFW_KEY_END:
                keyId = KeyEvent.VK_END;
                break;
            case GLFW.GLFW_KEY_PAGE_UP:
                keyId = KeyEvent.VK_PAGE_UP;
                break;
            case GLFW.GLFW_KEY_PAGE_DOWN:
                keyId = KeyEvent.VK_PAGE_DOWN;
                break;
            case GLFW.GLFW_KEY_INSERT:
                keyId = KeyEvent.VK_INSERT;
                break;
            case GLFW.GLFW_KEY_ESCAPE:
                keyId = KeyEvent.VK_ESCAPE;
                break;
            case GLFW.GLFW_KEY_CAPS_LOCK:
                keyId = KeyEvent.VK_CAPS_LOCK;
                break;
            case GLFW.GLFW_KEY_PAUSE:
                keyId = KeyEvent.VK_PAUSE;
                break;
            case GLFW.GLFW_KEY_PRINT_SCREEN:
                keyId = KeyEvent.VK_PRINTSCREEN;
                break;
            case GLFW.GLFW_KEY_LEFT_SHIFT:
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                keyId = KeyEvent.VK_SHIFT;
                break;
            case GLFW.GLFW_KEY_LEFT_ALT:
            case GLFW.GLFW_KEY_RIGHT_ALT:
                keyId = KeyEvent.VK_ALT;
                break;
            case GLFW.GLFW_KEY_NUM_LOCK:
                keyId = KeyEvent.VK_NUM_LOCK;
                break;
            case GLFW.GLFW_KEY_SCROLL_LOCK:
                keyId = KeyEvent.VK_SCROLL_LOCK;
                break;
            case 348:
                keyId = KeyEvent.VK_CONTEXT_MENU;
                break;
            default:
                if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
                    keyId = KeyEvent.VK_NUMPAD0 + (key - GLFW.GLFW_KEY_KP_0);
                } else if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) {
                    keyId = KeyEvent.VK_F1 + (key - GLFW.GLFW_KEY_F1);
                } else if (key > 0) {
                    keyId = key;
                }
                break;
        }

        this.shift = (mods & GLFW.GLFW_MOD_SHIFT) == GLFW.GLFW_MOD_SHIFT;
        this.alt = (mods & GLFW.GLFW_MOD_ALT) == GLFW.GLFW_MOD_ALT;
        this.ctrl = (mods & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL;
        this.meta = (mods & GLFW.GLFW_MOD_SUPER) == GLFW.GLFW_MOD_SUPER;

        switch (action) {
            case GLFW.GLFW_PRESS:
            case GLFW.GLFW_REPEAT:
                if (keyId > -1) {
                    this.emScene.keyEvent(AbstractEvents.KEYEVENT_PRESSED, keyId, new char[]{}, mods);
                }
                break;
            case GLFW.GLFW_RELEASE:
                if (keyId > -1) {
                    if (this.shift && keyId == KeyEvent.VK_F10) {
                        this.contextMenuHandler.fireContextMenuFromKeyboard();
                    } else if (keyId == KeyEvent.VK_CONTEXT_MENU) {
                        this.contextMenuHandler.fireContextMenuFromKeyboard();
                    }

                    this.emScene.keyEvent(AbstractEvents.KEYEVENT_RELEASED, keyId, new char[] {}, mods);
                }
                break;
            default:
                break;
        }
    }

    public void mouseButtonEvent(int button, int action, int mods) {
        this.shift = (mods & GLFW.GLFW_MOD_SHIFT) == GLFW.GLFW_MOD_SHIFT;
        this.ctrl = (mods & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL;
        this.alt = (mods & GLFW.GLFW_MOD_ALT) == GLFW.GLFW_MOD_ALT;
        this.meta = (mods & GLFW.GLFW_MOD_SUPER) == GLFW.GLFW_MOD_SUPER;

        int buttonId;

        if (GLFW.GLFW_PRESS == action) {
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT:
                    buttonId = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
                    leftButton = true;
                    break;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
                    buttonId = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
                    rightButton = true;
                    break;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE:
                    buttonId = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
                    middleButton = true;
                    break;
                default:
                    // other mouse button?
                    return;
            }

            this.emScene.mouseEvent(
                    AbstractEvents.MOUSEEVENT_PRESSED, buttonId,
                    this.leftButton, this.middleButton, this.rightButton,
                    mouseX, mouseY, mouseAbsX, mouseAbsY,
                    this.shift, this.ctrl, this.alt, this.meta,
                    0, false);
        } else if (GLFW.GLFW_RELEASE == action) {
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT:
                    buttonId = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
                    leftButton = true;
                    dndHandler.mouseReleased(mouseX, mouseY, mouseX, mouseY);
                    break;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
                    buttonId = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
                    rightButton = true;
                    contextMenuHandler.fireContextMenuFromMouse(mouseX, mouseY, mouseX, mouseY);
                    break;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE:
                    buttonId = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
                    middleButton = true;
                    break;
                default:
                    return;
            }

            this.emScene.mouseEvent(
                    AbstractEvents.MOUSEEVENT_RELEASED, buttonId,
                    leftButton, middleButton, rightButton,
                    mouseX, mouseY, mouseAbsX, mouseAbsY,
                    this.shift, this.ctrl, this.alt, this.meta,
                    0, false);
        }
    }
}
