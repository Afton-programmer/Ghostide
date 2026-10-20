package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
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
 *
 * <p>The section list is a {@link MaxHeightRecyclerView} so its viewport stays finite even inside a
 * {@code NestedScrollView}; expanding/collapsing only fades and slides the already laid out list
 * instead of animating its height frame by frame.
 */
public class ExpandableLayout extends GlassCompat {

  private static final int ANIM_DURATION = 200;
  private static final float CORNER_RADIUS_DP = 12f;
  private static final float CONTENT_OFFSET_DP = 8f;
  private static final float MAX_CONTENT_MIN_DP = 240f;
  private static final float MAX_CONTENT_MAX_DP = 420f;
  private static final float MAX_CONTENT_SCREEN_RATIO = 0.62f;

  private TextView titleView;
  private ImageView arrowIcon;
  private MaxHeightRecyclerView recyclerView;
  private boolean isExpanded = false;
  private float contentOffsetPx;

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
    contentOffsetPx = CONTENT_OFFSET_DP * density;
    applyContentHeightCap(density);
    findViewById(R.id.expandable_header).setOnClickListener(v -> toggle());
    recyclerView.setVisibility(GONE);

    M3Theme.textView(titleView);
    M3Theme.imageView(arrowIcon);
  }

  private void applyContentHeightCap(float density) {
    int screenHeight = getResources().getDisplayMetrics().heightPixels;
    int cap =
        (int) Math.min(MAX_CONTENT_MAX_DP * density, screenHeight * MAX_CONTENT_SCREEN_RATIO);
    recyclerView.setMaxHeight(Math.max(cap, (int) (MAX_CONTENT_MIN_DP * density)));
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

  public void expand() {
    if (isExpanded) return;
    isExpanded = true;
    recyclerView.animate().cancel();

    if (!AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      recyclerView.setVisibility(View.VISIBLE);
      recyclerView.setAlpha(1f);
      recyclerView.setTranslationY(0f);
      arrowIcon.setRotation(90f);
      return;
    }

    recyclerView.setVisibility(View.VISIBLE);
    recyclerView.setAlpha(0f);
    recyclerView.setTranslationY(contentOffsetPx);
    recyclerView
        .animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(ANIM_DURATION)
        .setInterpolator(new DecelerateInterpolator())
        .start();
    arrowIcon.animate().rotation(90f).setDuration(ANIM_DURATION).start();
  }

  public void collapse() {
    if (!isExpanded) return;
    isExpanded = false;
    recyclerView.animate().cancel();

    if (!AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      recyclerView.setVisibility(View.GONE);
      recyclerView.setAlpha(1f);
      recyclerView.setTranslationY(0f);
      arrowIcon.setRotation(0f);
      return;
    }

    recyclerView
        .animate()
        .alpha(0f)
        .translationY(contentOffsetPx)
        .setDuration(ANIM_DURATION)
        .setInterpolator(new DecelerateInterpolator())
        .withEndAction(
            () -> {
              if (isExpanded) return;
              recyclerView.setVisibility(View.GONE);
              recyclerView.setAlpha(1f);
              recyclerView.setTranslationY(0f);
            })
        .start();
    arrowIcon.animate().rotation(0f).setDuration(ANIM_DURATION).start();
  }

  public boolean isExpanded() {
    return isExpanded;
  }
}
