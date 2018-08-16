package demo.gljfx;

import com.sun.javafx.embed.EmbeddedSceneDSInterface;
import com.sun.javafx.embed.EmbeddedSceneDTInterface;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.HostDragStartListener;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.input.TransferMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class GLFXDNDHandler {
    private static final class DropInfo {
        private final EmbeddedSceneDSInterface source;
        private final TransferMode action;

        private DropInfo(final EmbeddedSceneDSInterface source, final TransferMode action) {
            this.source = source;
            this.action = action;
        }
    }

    private static final class DropHandlerData {
        private int x, y, sx, sy;
        private final EmbeddedSceneDTInterface target;

        private DropHandlerData(final EmbeddedSceneDTInterface target) {
            this.target = target;
        }

        private void handleDragLeave() {
            this.target.handleDragLeave();
        }

        private void handleDragEnter() {
           this.target.handleDragEnter(x, y, sx, sy, STATIC_DROP_INFO.action, STATIC_DROP_INFO.source);
       }

        private void handleDragOver() {
            this.target.handleDragOver(x, y, sx, sy, STATIC_DROP_INFO.action);
        }

        private TransferMode handleDragDrop() {
            return this.target.handleDragOver(x, y, sx, sy, STATIC_DROP_INFO.action);
        }
    }

    private static DropInfo STATIC_DROP_INFO = null;
    private static final List<DropHandlerData> DROP_TARGETS = new ArrayList<>();

    private final EmbeddedSceneInterface scene;
    private DropInfo dropInfo;
    private EmbeddedSceneDTInterface dropTarget = null;

    GLFXDNDHandler(final EmbeddedSceneInterface scene) {
        this.scene = scene;

        final HostDragStartListener dragStartListener = (source, action) -> {
            if (!Toolkit.getToolkit().isFxUserThread()) {
                throw new IllegalStateException("Not on FX Thread!");
            }

            Objects.requireNonNull(source);

            this.dropInfo = new DropInfo(source, action);

            STATIC_DROP_INFO = this.dropInfo;
        };

        this.scene.setDragStartListener(dragStartListener);
    }

    private DropHandlerData getDropTarget(int x, int y, int sx, int sy) {
        final DropHandlerData dt = DROP_TARGETS.stream()
                .filter(data -> data.target == this.dropTarget)
                .findFirst()
                .orElseGet(() -> {
                    this.dropTarget = this.scene.createDropTarget();

                    final DropHandlerData newDropHandler = new DropHandlerData(this.dropTarget);

                    DROP_TARGETS.add(newDropHandler);
                    return newDropHandler;
                });

        dt.x = x;
        dt.y = y;
        dt.sx = sx;
        dt.sy = sy;

        return dt;
    }

    void mousePosition(final int x, final int y, final int sx, final int sy) {
        if (STATIC_DROP_INFO != null) {
            final DropHandlerData dt = this.getDropTarget(x, y, sx, sy);

            dt.handleDragLeave();
            dt.handleDragEnter();
            dt.handleDragOver();
        }
    }

    void mouseReleased(final int x, final int y, final int sx, final int sy) {
       if (STATIC_DROP_INFO != null && this.dropInfo == STATIC_DROP_INFO) {
            TransferMode finalMode = null;

            for (DropHandlerData dt : DROP_TARGETS) {
                final TransferMode newMode = dt.handleDragDrop();

                if (newMode != null) {
                    finalMode = newMode;
                }
            }

            STATIC_DROP_INFO.source.dragDropEnd(finalMode);

            this.dropInfo = null;
            STATIC_DROP_INFO = null;
            DROP_TARGETS.clear();
        }

        this.dropTarget = null;
    }
}
