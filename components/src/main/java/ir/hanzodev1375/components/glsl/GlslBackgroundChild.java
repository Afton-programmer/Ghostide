package ir.hanzodev1375.components.glsl;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import ir.hanzodev1375.components.animators.AnimationManager;
import ir.hanzodev1375.components.childern.IChild;

/**
 * {@link IChild} that turns a {@code glsl://<effect>} background path into a live GPU background.
 *
 * <p>The three shader colors are always derived from {@link GlslPalette} (which reads {@code
 * M3Theme}) and darkened so text drawn on top stays readable. When animations are disabled (low
 * battery, per {@link AnimationManager}) the shader is frozen on a single frame instead of running
 * continuously.
 */
public final class GlslBackgroundChild implements IChild {

  private final FrameLayout container;
  private final GLSLShaderView shader;
  private final String path;

  GlslBackgroundChild(Context context, String path) {
    this.path = path;
    container = new FrameLayout(context);
    shader = new GLSLShaderView(context, effectOf(path));
    int[] colors = GlslPalette.background();
    shader.setColors(colors[0], colors[1], colors[2]);
    boolean animated = AnimationManager.getInstance(context).areAnimationsEnabled();
    shader.setAnimated(animated);
    container.addView(
        shader,
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
  }

  @Override
  public View view() {
    return container;
  }

  @Override
  public String pathTheme() {
    return path;
  }

  @Override
  public void release() {
    shader.release();
    container.removeAllViews();
  }

  /** Extracts the effect name from a {@code glsl://name} path. */
  private static String effectOf(String path) {
    if (path == null) {
      return GlslEffects.LIQUID;
    }
    int separator = path.indexOf("://");
    String name = separator >= 0 ? path.substring(separator + 3) : path;
    int cut = name.indexOf('/');
    if (cut >= 0) {
      name = name.substring(0, cut);
    }
    return name.trim().isEmpty() ? GlslEffects.LIQUID : name;
  }
}
