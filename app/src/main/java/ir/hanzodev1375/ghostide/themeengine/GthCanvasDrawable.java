package ir.hanzodev1375.ghostide.themeengine;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import ir.theme.M3Theme;

/** Paints a small abstract canvas from a theme's primary and tertiary colors. */
public final class GthCanvasDrawable extends Drawable {

  private static final int FALLBACK_PRIMARY = 0xFF6750A4;
  private static final int FALLBACK_SECONDARY = 0xFF625B71;
  private static final int FALLBACK_TERTIARY = 0xFF7D5260;

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF bounds = new RectF();
  private final Path clip = new Path();

  private final int primary;
  private final int secondary;
  private final int tertiary;

  private float cornerRadius;
  private int drawAlpha = 255;
  private Shader shader;

  public GthCanvasDrawable(
      @Nullable Integer primary, @Nullable Integer secondary, @Nullable Integer tertiary) {
    this.primary = resolve(primary, M3Theme.primary(), FALLBACK_PRIMARY);
    this.secondary = resolve(secondary, M3Theme.secondary(), FALLBACK_SECONDARY);
    this.tertiary = resolve(tertiary, M3Theme.tertiary(), FALLBACK_TERTIARY);
  }

  @Override
  protected void onBoundsChange(android.graphics.Rect newBounds) {
    super.onBoundsChange(newBounds);
    bounds.set(newBounds);
    float min = Math.min(bounds.width(), bounds.height());
    cornerRadius = min * 0.24f;
    clip.reset();
    clip.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);
    if (bounds.width() > 0f && bounds.height() > 0f) {
      shader =
          new LinearGradient(
              bounds.left,
              bounds.top,
              bounds.right,
              bounds.bottom,
              new int[] {primary, ColorUtils.blendARGB(primary, tertiary, 0.55f), tertiary},
              null,
              Shader.TileMode.CLAMP);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    if (bounds.isEmpty()) return;
    float width = bounds.width();
    float height = bounds.height();
    float min = Math.min(width, height);

    int save = canvas.save();
    canvas.clipPath(clip);

    paint.setShader(shader);
    paint.setAlpha(drawAlpha);
    paint.setStyle(Paint.Style.FILL);
    canvas.drawRect(bounds, paint);

    paint.setShader(null);
    paint.setAlpha(255);

    paint.setColor(withAlpha(tertiary, 96));
    canvas.drawCircle(
        bounds.left + width * 0.82f, bounds.top + height * 0.2f, min * 0.34f, paint);

    paint.setColor(withAlpha(primary, 72));
    canvas.drawCircle(
        bounds.left + width * 0.14f, bounds.bottom - height * 0.12f, min * 0.3f, paint);

    paint.setColor(withAlpha(secondary, 150));
    canvas.drawCircle(
        bounds.left + width * 0.72f, bounds.bottom - height * 0.28f, min * 0.08f, paint);

    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(min * 0.09f);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setColor(withAlpha(tertiary, 120));
    canvas.drawLine(
        bounds.left + width * 0.08f,
        bounds.bottom - height * 0.24f,
        bounds.right - width * 0.1f,
        bounds.top + height * 0.3f,
        paint);
    paint.setStyle(Paint.Style.FILL);

    canvas.restoreToCount(save);
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
}
