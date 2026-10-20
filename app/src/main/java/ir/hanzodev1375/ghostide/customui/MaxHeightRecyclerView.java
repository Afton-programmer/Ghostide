package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView that measures its wrapped content only up to a maximum height.
 *
 * <p>Inside a {@code NestedScrollView} a plain {@code wrap_content} RecyclerView is measured with an
 * unbounded height, so it inflates and lays out every row at once and never recycles. Capping the
 * measured height keeps the viewport finite: only the visible rows are bound and the remaining ones
 * are reached by scrolling the list itself (nested scrolling), like a regular list.
 */
public class MaxHeightRecyclerView extends RecyclerView {

  private int maxHeight;

  public MaxHeightRecyclerView(@NonNull Context context) {
    super(context);
  }

  public MaxHeightRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public MaxHeightRecyclerView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /** Caps the measured height in pixels; {@code 0} or less disables the cap. */
  public void setMaxHeight(int maxHeight) {
    if (this.maxHeight == maxHeight) return;
    this.maxHeight = maxHeight;
    requestLayout();
  }

  public int getMaxHeight() {
    return maxHeight;
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int mode = MeasureSpec.getMode(heightSpec);
    if (maxHeight > 0 && mode != MeasureSpec.EXACTLY) {
      int limit = maxHeight;
      if (mode == MeasureSpec.AT_MOST) {
        limit = Math.min(MeasureSpec.getSize(heightSpec), maxHeight);
      }
      heightSpec = MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST);
    }
    super.onMeasure(widthSpec, heightSpec);
  }
}
