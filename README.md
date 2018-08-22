# GLJFX
Embed JavaFX 8 Scene in an OpenGL+GLFW context.

This project uses internal Java8 APIs and __will not__ work on Java9+

## Using this code/Licence
This project is probably not going to receive much maintenance and can be
considered abandoned. It is therefore licensed under The Unlicense as 
__public domain__. If you want to include any of this code in _any_ project, 
please copy and paste the code from the _demo_ package into your package structure.

## How it Works
JavaFX has an _Embedded Window_ object that can be extended to grab redraw events.

A _getPixels_ operation is done on redraw and uploaded to an OpenGL texture.

## Beyond Java8
It would probably be best to look at [OpenJFX Github](https://github.com/javafxports/openjdk-jfx/blob/96bbea07f2675af93212af1a097628814f86680e/modules/javafx.swing/src/main/java/javafx/embed/swing/JFXPanel.java)
GLFXStage depends on com.sun.javafx.embed package from javafx.graphics module.

This would require adding the Java Command Line Argument:
``` bash
--add-exports javafx.graphics/com.sun.javafx.embed=name_of_this_module
```

For distribution use, this looks like it would require using a bootstrap compiler
on this module to add the needed export. Then to programmatically access the 
internal API: in a static block, fetch the Module javafx.graphics and add export
to this module.
Probably something like:
``` java
ModuleLayer.boot()
  .findModule("javafx.graphics")
  .orElseThrow(() -> new RuntimeException("Unable to find module \"javafx.graphics\"!")
  .addExports("com.sun.javafx.embed", GLFXStage.class.getModule());
```

Besides that, it looks like the internal API didn't change much from OpenJFX8 to OpenJFX11.