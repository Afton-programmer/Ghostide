package ir.hanzodev1375.components.glsl;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * A {@link GLSurfaceView} that renders one of the {@link GlslEffects} fragment shaders full-screen.
 *
 * <p>It is intentionally self-contained: the view manages its own GL context across
 * attach/detach, so callers only need to set the effect and (optionally) the theme colors. All
 * colors come from the caller; this class never hard-codes a palette.
 *
 * <p>The GL thread is paused and the context released when the view leaves the window, and the
 * renderer is destroyed with it, so no GPU context outlives its view.
 */
public class GLSLShaderView extends GLSurfaceView {

  private static final String TAG = "GLSLShaderView";

  private static final int FLOAT_BYTES = 4;

  /** Full-screen triangle in clip space; larger than the viewport so it always covers it. */
  private static final float[] QUAD = {-1f, -1f, 3f, -1f, -1f, 3f};

  private final ShaderRenderer renderer;

  private boolean resumed;

  public GLSLShaderView(Context context, String effectName) {
    super(context);
    setEGLContextClientVersion(2);
    setPreserveEGLContextOnPause(false);
    renderer = new ShaderRenderer(GlslEffects.fragmentShader(effectName));
    setRenderer(renderer);
    setRenderMode(RENDERMODE_CONTINUOUSLY);
  }

  public GLSLShaderView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setEGLContextClientVersion(2);
    setPreserveEGLContextOnPause(false);
    renderer = new ShaderRenderer(GlslEffects.fragmentShader(GlslEffects.LIQUID));
    setRenderer(renderer);
    setRenderMode(RENDERMODE_CONTINUOUSLY);
  }

  /** Sets the three theme colors (ARGB) blended by the fragment shader. */
  public void setColors(int colorA, int colorB, int colorC) {
    renderer.setColors(colorA, colorB, colorC);
    requestRender();
  }

  /** Enables/disables continuous animation. When disabled a single frame is drawn. */
  public void setAnimated(boolean animated) {
    setRenderMode(animated ? RENDERMODE_CONTINUOUSLY : RENDERMODE_WHEN_DIRTY);
    if (!animated) {
      requestRender();
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (!resumed) {
      resumed = true;
      onResume();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (resumed) {
      resumed = false;
      onPause();
    }
    super.onDetachedFromWindow();
  }

  /** Pauses the GL thread. Safe to call more than once and from the UI thread. */
  public void release() {
    if (resumed) {
      resumed = false;
      onPause();
    }
  }

  private static final class ShaderRenderer implements GLSurfaceView.Renderer {

    private final String fragmentSource;

    private int program;
    private int aPosition;

    private int uResolution;
    private int uTime;
    private int uColorA;
    private int uColorB;
    private int uColorC;

    private int width;
    private int height;

    private long startNanos;

    private FloatBuffer vertexBuffer;

    private volatile int colorA = Color.BLACK;
    private volatile int colorB = Color.BLACK;
    private volatile int colorC = Color.BLACK;

    ShaderRenderer(String fragmentSource) {
      this.fragmentSource = fragmentSource;
    }

    void setColors(int a, int b, int c) {
      colorA = a;
      colorB = b;
      colorC = c;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
      startNanos = System.nanoTime();

      vertexBuffer = ByteBuffer.allocateDirect(QUAD.length * FLOAT_BYTES)
          .order(ByteOrder.nativeOrder())
          .asFloatBuffer();
      vertexBuffer.put(QUAD).position(0);

      program = buildProgram(GlslEffects.vertexShader(), fragmentSource);
      if (program == 0) {
        return;
      }
      aPosition = GLES20.glGetAttribLocation(program, "aPosition");
      uResolution = GLES20.glGetUniformLocation(program, "uResolution");
      uTime = GLES20.glGetUniformLocation(program, "uTime");
      uColorA = GLES20.glGetUniformLocation(program, "uColorA");
      uColorB = GLES20.glGetUniformLocation(program, "uColorB");
      uColorC = GLES20.glGetUniformLocation(program, "uColorC");

      GLES20.glDisable(GLES20.GL_DEPTH_TEST);
      GLES20.glDisable(GLES20.GL_BLEND);
      GLES20.glClearColor(0f, 0f, 0f, 1f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int surfaceWidth, int surfaceHeight) {
      width = surfaceWidth;
      height = surfaceHeight;
      GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
      if (program == 0 || vertexBuffer == null) {
        return;
      }
      GLES20.glUseProgram(program);

      GLES20.glUniform2f(uResolution, width, height);
      GLES20.glUniform1f(uTime, (System.nanoTime() - startNanos) * 1e-9f);
      setColorUniform(uColorA, colorA);
      setColorUniform(uColorB, colorB);
      setColorUniform(uColorC, colorC);

      if (aPosition >= 0) {
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
      }
      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
      if (aPosition >= 0) {
        GLES20.glDisableVertexAttribArray(aPosition);
      }
    }

    private static void setColorUniform(int location, int argb) {
      GLES20.glUniform3f(
          location, Color.red(argb) / 255f, Color.green(argb) / 255f, Color.blue(argb) / 255f);
    }

    private static int buildProgram(String vertexSource, String fragmentSource) {
      int vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
      int fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
      if (vertex == 0 || fragment == 0) {
        if (vertex != 0) GLES20.glDeleteShader(vertex);
        if (fragment != 0) GLES20.glDeleteShader(fragment);
        return 0;
      }
      int program = GLES20.glCreateProgram();
      GLES20.glAttachShader(program, vertex);
      GLES20.glAttachShader(program, fragment);
      GLES20.glLinkProgram(program);

      int[] status = new int[1];
      GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
      GLES20.glDeleteShader(vertex);
      GLES20.glDeleteShader(fragment);
      if (status[0] == 0) {
        Log.e(TAG, "Shader link failed: " + GLES20.glGetProgramInfoLog(program));
        GLES20.glDeleteProgram(program);
        return 0;
      }
      return program;
    }

    private static int compileShader(int type, String source) {
      int shader = GLES20.glCreateShader(type);
      if (shader == 0) {
        return 0;
      }
      GLES20.glShaderSource(shader, source);
      GLES20.glCompileShader(shader);
      int[] status = new int[1];
      GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
      if (status[0] == 0) {
        Log.e(TAG, "Shader compile failed: " + GLES20.glGetShaderInfoLog(shader));
        GLES20.glDeleteShader(shader);
        return 0;
      }
      return shader;
    }
  }
}
