# GLJFX
Embed JavaFX 8 Scene in an OpenGL+GLFW context.

This project uses internal Java8 APIs and __will not__ work on Java9+

## How it Works
JavaFX has an _Embedded Window_ object that can be extended to grab redraw events.

A _getPixels_ operation is done on redraw and uploaded to an OpenGL texture.