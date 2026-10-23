package ir.hanzodev1375.components.views;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

public class GradientFrameLayout extends FrameLayout {

  private static final float HOLD_FRACTION = 0.35f;
  private static final long COLOR_ANIMATION_DURATION = 420L;

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
  private int startColor = Color.TRANSPARENT;
  private boolean rtl;
  private ValueAnimator colorAnimator;

  public GradientFrameLayout(Context context) {
    super(context);
    init();
  }

  public GradientFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public GradientFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    paint.setDither(true);
    setWillNotDraw(false);
  }

  public int getStartColor() {
    return startColor;
  }

  public void setStartColor(int color) {
    if (colorAnimator != null) {
      colorAnimator.cancel();
      colorAnimator = null;
    }
    startColor = color;
    rebuildShader();
    invalidate();
  }

  public void animateToColor(int color, long duration) {
    if (color == startColor && (colorAnimator == null || !colorAnimator.isRunning())) {
      return;
    }
    if (colorAnimator != null) {
      colorAnimator.cancel();
    }
    colorAnimator = ValueAnimator.ofObject(argbEvaluator, startColor, color);
    colorAnimator.setDuration(duration > 0 ? duration : COLOR_ANIMATION_DURATION);
    colorAnimator.setInterpolator(new DecelerateInterpolator());
    colorAnimator.addUpdateListener(
        animation -> {
          startColor = (int) animation.getAnimatedValue();
          rebuildShader();
          invalidate();
        });
    colorAnimator.start();
  }

  public void animateToColor(int color) {
    animateToColor(color, COLOR_ANIMATION_DURATION);
  }

  private void rebuildShader() {
    int width = getWidth();
    int height = getHeight();
    if (width <= 0 || height <= 0 || startColor == Color.TRANSPARENT) {
      paint.setShader(null);
      return;
    }
    float x0 = rtl ? width : 0f;
    float x1 = rtl ? 0f : width;
    paint.setShader(
        new LinearGradient(
            x0,
            0f,
            x1,
            0f,
            new int[] {startColor, startColor, Color.TRANSPARENT},
            new float[] {0f, HOLD_FRACTION, 1f},
            Shader.TileMode.CLAMP));
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    rebuildShader();
  }

  @Override
  public void onRtlPropertiesChanged(int layoutDirection) {
    super.onRtlPropertiesChanged(layoutDirection);
    rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL;
    rebuildShader();
    invalidate();
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (paint.getShader() != null) {
      canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
    }
    super.dispatchDraw(canvas);
  }

  @Override
  protected void onDetachedFromWindow() {
    if (colorAnimator != null) {
      colorAnimator.cancel();
      colorAnimator = null;
    }
    super.onDetachedFromWindow();
  }
}
