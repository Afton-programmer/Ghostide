package ir.hanzodev1375.components.glsl;

import android.graphics.Color;
import ir.theme.M3Theme;

/**
 * Derives the three {@link GLSLShaderView} colors from the active {@link M3Theme}.
 *
 * <p>Every GLSL effect receives the same triple ({@code colorA}, {@code colorB}, {@code colorC}) so
 * shaders stay palette-agnostic. The background variant is darkened to keep on-screen text
 * readable, while the accent variant keeps the full saturation for glow-style effects.
 */
public final class GlslPalette {

  private GlslPalette() {}

  /** Dark surface + two tinted accents, tuned for full-screen backgrounds. */
  public static int[] background() {
    return new int[] {
      scale(or(M3Theme.surfaceContainer(), Color.BLACK), 0.55f),
      scale(or(M3Theme.primary(), M3Theme.surfaceContainer()), 0.5f),
      scale(or(M3Theme.tertiary(), M3Theme.primary()), 0.45f)
    };
  }

  /** Brighter surface + accents, tuned for overlays drawn on top of existing UI. */
  public static int[] accent() {
    return new int[] {
      scale(or(M3Theme.surfaceVariant(), Color.BLACK), 0.7f),
      or(M3Theme.primary(), M3Theme.tertiary()),
      or(M3Theme.secondary(), M3Theme.primary())
    };
  }

  private static int or(Integer first, Integer fallback) {
    if (first != null) {
      return first;
    }
    return fallback != null ? fallback : Color.BLACK;
  }

  private static int scale(int color, float factor) {
    int r = Math.round(Color.red(color) * factor);
    int g = Math.round(Color.green(color) * factor);
    int b = Math.round(Color.blue(color) * factor);
    return Color.argb(255, clamp(r), clamp(g), clamp(b));
  }

  private static int clamp(int value) {
    return value < 0 ? 0 : (value > 255 ? 255 : value);
  }
}
