package demo.gljfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.junit.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL45C;

public class TestGLJFX {
    @Test
    public void runTest() {
        GLFW.glfwInit();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);

        final long window = GLFW.glfwCreateWindow(640, 480, "Test GLJFX", 0L, 0L);

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        while (GL45C.glGetError() != GL45C.GL_NO_ERROR) {}

        final GLFXStage primaryStage = new GLFXStage(640, 480);
        {
            final GridPane grid = new GridPane();

            grid.setAlignment(Pos.CENTER);
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(25, 25, 25, 25));

            final Text sceneTitle = new Text("Welcome");
            sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
            grid.add(sceneTitle, 0, 0, 2, 1);

            final Label userName = new Label("User Name:");
            grid.add(userName, 0, 1);

            final TextField userTextField = new TextField();
            grid.add(userTextField, 1, 1);

            final Label pw = new Label("Password:");
            grid.add(pw, 0, 2);

            final PasswordField pwBox = new PasswordField();
            grid.add(pwBox, 1, 2);

            final Button btn = new Button("Sign in");
            final HBox hbBtn = new HBox(10);

            hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
            hbBtn.getChildren().add(btn);

            grid.add(hbBtn, 1, 4);

            final Text actionTarget = new Text();

            grid.add(actionTarget, 1, 6);

            btn.setOnAction(event -> {
                actionTarget.setFill(Color.FIREBRICK);
                actionTarget.setText("Sign in button pressed");
            });

            final Scene scene = new Scene(grid, 640, 480);

            primaryStage.setScene(scene);
        }

        primaryStage.setParentWindowSize(640, 480);

        GLFW.glfwSetMouseButtonCallback(window, (hwnd, button, action, mods) -> primaryStage.mouseButtonEvent(button, action, mods));
        GLFW.glfwSetCursorPosCallback(window, (hwnd, xpos, ypos) -> primaryStage.mouseMoveEvent(xpos, ypos));
        GLFW.glfwSetFramebufferSizeCallback(window, (hwnd, w, h) -> primaryStage.setParentWindowSize(w, h));
        GLFW.glfwSetKeyCallback(window, (hwnd, key, scancode, action, mods) -> primaryStage.keyEvent(key, scancode, action, mods));
        GLFW.glfwSetCharCallback(window, (hwnd, codepoint) -> primaryStage.keyCharEvent(codepoint));

        while (!GLFW.glfwWindowShouldClose(window)) {
            GL45C.glClear(GL45C.GL_COLOR_BUFFER_BIT);

            primaryStage.drawGL();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
