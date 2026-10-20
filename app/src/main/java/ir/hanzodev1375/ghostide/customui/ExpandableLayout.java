package ir.hanzodev1375.ghostide.customui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.liquidglass.GlassMaterial;
import ir.hanzodev1375.components.animators.AnimationManager;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;
import ir.hanzodev1375.ghostide.R;
import ir.theme.M3Theme;

/**
 * Collapsible settings card rendered as a {@link GlassCompat} surface.
 *
 * <p>The class is itself the glass card: {@code GlassCompat} is a {@code LiquidGlassView} (i.e. a
 * {@link android.widget.FrameLayout}), so the header and the section list live inside a vertical
 * container inflated from {@code layout_expandable}. The glass samples a backdrop, so the host must
 * call {@link #setBackdropSource(View)} with the view painted behind this card.
 */
public class ExpandableLayout extends GlassCompat {

  private static final int ANIM_DURATION = 200;
  private static final float CORNER_RADIUS_DP = 12f;

  private TextView titleView;
  private ImageView arrowIcon;
  private RecyclerView recyclerView;
  private boolean isExpanded = false;
  private ValueAnimator heightAnimator;

  public ExpandableLayout(@NonNull Context context) {
    super(context);
    init(context);
  }

  public ExpandableLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public ExpandableLayout(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    float density = getResources().getDisplayMetrics().density;
    setCornerRadius(CORNER_RADIUS_DP * density);
    setRefractionHeight(40f * density);
    setBevelWidth(8f * density);
    setDispersionStrength(0.1f);
    setMaterial(GlassMaterial.REGULAR);
    setEnableDynamicBackground(true);
    setEnableSensorHighlight(false);
    setEnableAdaptiveTint(true);
    refresh();

    LayoutInflater.from(context).inflate(R.layout.layout_expandable, this, true);
    titleView = findViewById(R.id.expandable_title);
    arrowIcon = findViewById(R.id.expandable_arrow);
    recyclerView = findViewById(R.id.expandable_recycler);
    findViewById(R.id.expandable_header).setOnClickListener(v -> toggle());
    recyclerView.setVisibility(GONE);

    M3Theme.textView(titleView);
    M3Theme.imageView(arrowIcon);
  }

  public void setTitle(String title) {
    titleView.setText(title);
  }

  public RecyclerView getRecyclerView() {
    return recyclerView;
  }

  public void toggle() {
    if (isExpanded) collapse();
    else expand();
  }

  private void stopRunningAnimation() {
    if (heightAnimator != null && heightAnimator.isRunning()) {
      heightAnimator.cancel();
    }
    heightAnimator = null;
    arrowIcon.animate().cancel();
  }

  public void expand() {
    expand(true);
  }

  /**
   * @param animate pass {@code false} to skip the height animation; use when several sections
   *     expand at once (e.g. search) so the glass cards are not re-rendered many times per frame.
   */
  public void expand(boolean animate) {
    if (isExpanded) return;
    isExpanded = true;
    stopRunningAnimation();

    if (!animate || !AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      resetRecyclerViewHeight();
      recyclerView.setVisibility(View.VISIBLE);
      arrowIcon.setRotation(90f);
      return;
    }

    recyclerView.setVisibility(View.VISIBLE);
    recyclerView.post(
        () -> {
          if (!isExpanded) return;
          int targetHeight = recyclerView.getHeight();
          if (targetHeight <= 0) {
            // The list was never laid out (e.g. it became visible this frame); measure it
            // instead of silently skipping the expansion.
            recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED);
            targetHeight = recyclerView.getMeasuredHeight();
          }
          if (targetHeight <= 0) {
            resetRecyclerViewHeight();
            return;
          }
          LinearLayout.LayoutParams lp =
              (LinearLayout.LayoutParams) recyclerView.getLayoutParams();
          lp.height = 0;
          recyclerView.setLayoutParams(lp);
          heightAnimator = ValueAnimator.ofInt(0, targetHeight);
          heightAnimator
              .setDuration(ANIM_DURATION)
              .setInterpolator(new DecelerateInterpolator());
          heightAnimator.addUpdateListener(
              a -> {
                lp.height = (int) a.getAnimatedValue();
                recyclerView.setLayoutParams(lp);
              });
          heightAnimator.addListener(
              new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                  // Back to wrap_content so later content changes (filtering, adding rows)
                  // are never clipped by a stale fixed height.
                  if (isExpanded) resetRecyclerViewHeight();
                }
              });
          heightAnimator.start();
        });
    arrowIcon.animate().rotation(90f).setDuration(ANIM_DURATION).start();
  }

  public void collapse() {
    collapse(true);
  }

  /** @param animate pass {@code false} to skip the height animation (see {@link #expand(boolean)}). */
  public void collapse(boolean animate) {
    if (!isExpanded) return;
    isExpanded = false;
    stopRunningAnimation();

    if (!animate || !AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      recyclerView.setVisibility(View.GONE);
      resetRecyclerViewHeight();
      arrowIcon.setRotation(0f);
      return;
    }

    LinearLayout.LayoutParams lp =
        (LinearLayout.LayoutParams) recyclerView.getLayoutParams();
    int currentHeight = recyclerView.getHeight();
    if (currentHeight <= 0) {
      recyclerView.setVisibility(View.GONE);
      resetRecyclerViewHeight();
      arrowIcon.setRotation(0f);
      return;
    }
    heightAnimator = ValueAnimator.ofInt(currentHeight, 0);
    heightAnimator.setDuration(ANIM_DURATION).setInterpolator(new DecelerateInterpolator());
    heightAnimator.addUpdateListener(
        a -> {
          lp.height = (int) a.getAnimatedValue();
          recyclerView.setLayoutParams(lp);
        });
    heightAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            recyclerView.setVisibility(View.GONE);
            resetRecyclerViewHeight();
          }
        });
    heightAnimator.start();
    arrowIcon.animate().rotation(0f).setDuration(ANIM_DURATION).start();
  }

  private void resetRecyclerViewHeight() {
    LinearLayout.LayoutParams lp =
        (LinearLayout.LayoutParams) recyclerView.getLayoutParams();
    lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    recyclerView.setLayoutParams(lp);
  }

  public boolean isExpanded() {
    return isExpanded;
  }
}
