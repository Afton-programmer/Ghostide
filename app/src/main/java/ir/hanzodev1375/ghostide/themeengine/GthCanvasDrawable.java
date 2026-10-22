package ir.hanzodev1375.ghostide.themeengine;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import ir.hanzodev1375.filetreelib.vectormaster.utilities.legacyparser.PathParser;
import ir.theme.M3Theme;

/** Paints the ghost palette glyph using a theme's primary, secondary and tertiary colors. */
public final class GthCanvasDrawable extends Drawable {

  private static final float VIEWPORT = 24f;
  private static final float CONTENT_SCALE = 0.94f;
  private static final int INTRINSIC_DP = 24;

  private static final int FALLBACK_PRIMARY = 0xFF6750A4;
  private static final int FALLBACK_SECONDARY = 0xFF625B71;
  private static final int FALLBACK_TERTIARY = 0xFF7D5260;
  private static final int FALLBACK_BASE = 0xFFE6E0E9;

  private static final Path BODY_PATH =
      PathParser.createPathFromPathData(
          "M12,22C6.49,22 2,17.51 2,12S6.49,2 12,2s10,4.04 10,9c0,3.31 -2.69,6 -6,6h-1.77c-0.28,0 -0.5,0.22 -0.5,0.5 0,0.12 0.05,0.23 0.13,0.33 0.41,0.47 0.64,1.06 0.64,1.67 0,1.38 -1.12,2.5 -2.5,2.5zM12,4c-4.41,0 -8,3.59 -8,8s3.59,8 8,8c0.28,0 0.5,-0.22 0.5,-0.5 0,-0.16 -0.08,-0.28 -0.14,-0.35 -0.41,-0.46 -0.63,-1.05 -0.63,-1.65 0,-1.38 1.12,-2.5 2.5,-2.5L16,15c2.21,0 4,-1.79 4,-4 0,-3.86 -3.59,-7 -8,-7z");

  private static final Path[] DOT_PATHS = {
    PathParser.createPathFromPathData("M6.5,11.5m-1.5,0a1.5,1.5 0,1 1,3 0a1.5,1.5 0,1 1,-3 0"),
    PathParser.createPathFromPathData("M9.5,7.5m-1.5,0a1.5,1.5 0,1 1,3 0a1.5,1.5 0,1 1,-3 0"),
    PathParser.createPathFromPathData("M14.5,7.5m-1.5,0a1.5,1.5 0,1 1,3 0a1.5,1.5 0,1 1,-3 0"),
    PathParser.createPathFromPathData("M17.5,11.5m-1.5,0a1.5,1.5 0,1 1,3 0a1.5,1.5 0,1 1,-3 0")
  };

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Matrix matrix = new Matrix();
  private final Path scaledBody = new Path();
  private final Path[] scaledDots = new Path[DOT_PATHS.length];
  private final int[] dotColors = new int[DOT_PATHS.length];
  private final int intrinsicSize;

  private final int base;
  private final int primary;
  private final int secondary;
  private final int tertiary;

  private int drawAlpha = 255;

  public GthCanvasDrawable(
      @Nullable Integer primary, @Nullable Integer secondary, @Nullable Integer tertiary) {
    this(primary, secondary, tertiary, null);
  }

  public GthCanvasDrawable(
      @Nullable Integer primary,
      @Nullable Integer secondary,
      @Nullable Integer tertiary,
      @Nullable Integer base) {
    this.primary = resolve(primary, M3Theme.primary(), FALLBACK_PRIMARY);
    this.secondary = resolve(secondary, M3Theme.secondary(), FALLBACK_SECONDARY);
    this.tertiary = resolve(tertiary, M3Theme.tertiary(), FALLBACK_TERTIARY);
    this.base = resolve(base, M3Theme.surfaceContainerHighest(), FALLBACK_BASE);
    for (int i = 0; i < scaledDots.length; i++) {
      scaledDots[i] = new Path();
      dotColors[i] = dotColor(i);
    }
    float density = Resources.getSystem().getDisplayMetrics().density;
    intrinsicSize = Math.round(INTRINSIC_DP * density);
  }

  @Override
  protected void onBoundsChange(Rect newBounds) {
    super.onBoundsChange(newBounds);
    if (newBounds.isEmpty()) return;
    float scale =
        Math.min(newBounds.width(), newBounds.height()) / VIEWPORT * CONTENT_SCALE;
    float dx = newBounds.left + (newBounds.width() - VIEWPORT * scale) * 0.5f;
    float dy = newBounds.top + (newBounds.height() - VIEWPORT * scale) * 0.5f;
    matrix.setScale(scale, scale);
    matrix.postTranslate(dx, dy);
    BODY_PATH.transform(matrix, scaledBody);
    for (int i = 0; i < DOT_PATHS.length; i++) {
      DOT_PATHS[i].transform(matrix, scaledDots[i]);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    if (getBounds().isEmpty()) return;
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(null);

    paint.setColor(withAlpha(base, 255));
    canvas.drawPath(scaledBody, paint);

    for (int i = 0; i < scaledDots.length; i++) {
      paint.setColor(withAlpha(dotColors[i], 255));
      canvas.drawPath(scaledDots[i], paint);
    }
  }

  @Override
  public int getIntrinsicWidth() {
    return intrinsicSize;
  }

  @Override
  public int getIntrinsicHeight() {
    return intrinsicSize;
  }

  @Override
  public void setAlpha(int alpha) {
    drawAlpha = Math.max(0, Math.min(255, alpha));
    invalidateSelf();
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    paint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  private int withAlpha(int color, int alpha) {
    int scaled = alpha * drawAlpha / 255;
    return (color & 0x00FFFFFF) | (scaled << 24);
  }

  private static int resolve(Integer value, Integer themeValue, int fallback) {
    if (value != null) return value;
    if (themeValue != null) return themeValue;
    return fallback;
  }

  /** Cycles the theme's three accents across the four dots of the palette glyph. */
  private int dotColor(int index) {
    if (index == 1) return secondary;
    if (index == 2) return tertiary;
    return primary;
  }
}
