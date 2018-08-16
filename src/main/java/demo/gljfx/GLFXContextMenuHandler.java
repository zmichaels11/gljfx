package demo.gljfx;

import com.sun.javafx.embed.EmbeddedSceneInterface;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.PickResult;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

final class GLFXContextMenuHandler {
    private final EmbeddedSceneInterface scene;
    private final GLFXStage stage;

    GLFXContextMenuHandler(final EmbeddedSceneInterface scene, final GLFXStage stage) {
        this.scene = scene;
        this.stage = stage;
    }

    void fireContextMenuFromMouse(final double mouseX, final double mouseY, final double mouseAbsX, final double mouseAbsY) {
        this.stage.getRootNode().ifPresent(root ->forAllUnder(root, mouseX, mouseY, node -> fireContextMenuEventFromMouse(node, mouseX, mouseY, mouseAbsX, mouseAbsY)));
    }

    void fireContextMenuFromKeyboard() {
        Platform.runLater(() -> {
            final Optional<Node> focusOwner = stage.getScene()
                    .map(Scene::getFocusOwner);

            focusOwner.ifPresent(this::fireContextMenuEventFromKeyboard);
        });
    }

    void fireContextMenuEventFromKeyboard(Node node) {
        Platform.runLater(() -> {
            final Bounds tabBounds = node.getBoundsInLocal();
            final double centerX = tabBounds.getMinX() + tabBounds.getWidth() * 0.5;
            final double centerY = tabBounds.getMinY() + tabBounds.getHeight() * 0.5;
            final Point2D pos = node.localToScene(centerX, centerY);
            final double x = pos.getX();
            final double y = pos.getY();
            final Event contextMenuEvent = new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, centerX, centerY, x, y, true, new PickResult(node, x, y));

            Event.fireEvent(node, contextMenuEvent);
        });
    }

    void fireContextMenuEventFromMouse(final Node node, final double x, final double y, final double mouseAbsX, final double mouseAbsY) {
        Event contextMenuEvent = new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, x, y, mouseAbsX, mouseAbsY, false, new PickResult(node, x, y));
        Event.fireEvent(node, contextMenuEvent);
    }



    private static void forAllUnder(Node node, final double sceneX, final double sceneY, final Consumer<Node> onUnder) {
        Point2D p = node.sceneToLocal(sceneX, sceneY, true);

        if (!node.contains(p)) {
            return;
        }

        if (node instanceof Parent) {
            Node bestMatchingChild = null;
            List<Node> children = ((Parent) node).getChildrenUnmodifiable();

            for (int i = children.size() - 1; i >= 0; i--) {
                final Node child = children.get(i);
                p = child.sceneToLocal(sceneX, sceneY, true);

                if (child.isVisible() && !child.isMouseTransparent() && child.contains(p)) {
                    bestMatchingChild = child;
                    break;
                }
            }

            if (bestMatchingChild != null) {
                onUnder.accept(bestMatchingChild);
                forAllUnder(bestMatchingChild, sceneX, sceneY, onUnder);
            }
        }
    }
}
