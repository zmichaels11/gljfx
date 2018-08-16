package demo.gljfx;

import org.lwjgl.opengl.GL45C;

final class Program {
    private static final String FRAGMENT_SHADER_SOURCE =
            "#version 450 core\n" +
            "in vec2 uvs;\n" +
            "uniform sampler2D fxTexture;\n" +
            "out vec4 fColor;\n" +
            "void main() {\n" +
            "    fColor = texture(fxTexture, uvs);    \n" +
            "}";

    private static final String VERTEX_SHADER_SOURCE =
            "#version 450 core\n" +
            "in vec2 vPos;\n" +
            "in vec2 vUVs;\n" +
            "uniform mat4 vProj;\n" +
            "out vec2 uvs;\n" +
            "void main() {\n" +
            "    gl_Position = vProj * vec4(vPos, 0.0, 1.0);\n" +
            "    uvs = vUVs;\n" +
            "}";

    final int handle;
    final int projectionBinding;
    final int textureBinding;

    private Program() {
        final int vsh = GL45C.glCreateShader(GL45C.GL_VERTEX_SHADER);

        GL45C.glShaderSource(vsh, VERTEX_SHADER_SOURCE);
        GL45C.glCompileShader(vsh);

        if (GL45C.GL_TRUE != GL45C.glGetShaderi(vsh, GL45C.GL_COMPILE_STATUS)) {
            throw new RuntimeException(GL45C.glGetShaderInfoLog(vsh));
        }

        final int fsh = GL45C.glCreateShader(GL45C.GL_FRAGMENT_SHADER);

        GL45C.glShaderSource(fsh, FRAGMENT_SHADER_SOURCE);
        GL45C.glCompileShader(fsh);

        if (GL45C.GL_TRUE != GL45C.glGetShaderi(fsh, GL45C.GL_COMPILE_STATUS)) {
            throw new RuntimeException(GL45C.glGetShaderInfoLog(fsh));
        }

        this.handle = GL45C.glCreateProgram();
        GL45C.glAttachShader(this.handle, vsh);
        GL45C.glAttachShader(this.handle, fsh);
        GL45C.glLinkProgram(this.handle);

        if (GL45C.GL_TRUE != GL45C.glGetProgrami(this.handle, GL45C.GL_LINK_STATUS)) {
            throw new RuntimeException(GL45C.glGetProgramInfoLog(this.handle));
        }

        GL45C.glDetachShader(this.handle, fsh);
        GL45C.glDetachShader(this.handle, vsh);
        GL45C.glDeleteShader(fsh);
        GL45C.glDeleteShader(vsh);

        GL45C.glUseProgram(this.handle);
        this.projectionBinding = GL45C.glGetUniformLocation(this.handle, "vProj");
        this.textureBinding = GL45C.glGetUniformLocation(this.handle, "fxTexture");

        assert this.projectionBinding >= 0;
        assert this.textureBinding >= 0;
    }

    void free() {
        GL45C.glDeleteProgram(this.handle);
    }

    private static final class ProgramHolder {
        private ProgramHolder() {}

        private static final Program INSTANCE = new Program();
    }

    static Program getInstance() {
        return ProgramHolder.INSTANCE;
    }
}
