package ir.hanzodev1375.components.glass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.example.liquidglass.GlassMaterial;
import com.example.liquidglass.LiquidGlassTabBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import ir.theme.M3Theme;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * {@link LiquidGlassTabBar} that wires its own backdrop and keeps the bar a clean pill.
 *
 * <p>Two things a hand-rolled tab bar gets wrong:
 *
 * <ul>
 *   <li>The bar lives in a separate window, so it needs the host Activity's content view as its
 *       backdrop instead of its (transparent) parent.
 *   <li>Writing system insets into the bar's padding makes the panel far taller than its
 *       content, and a pill's curve scales with the panel's height — so the ends curve so deeply
 *       that the first and last tab appear to poke out through the corner. The inset belongs on
 *       the margin, and the content needs an inset of its own so the tabs stay clear of the
 *       curve.
 * </ul>
 *
 * <p>When the glass mode is turned off the bar hosts a real Material 3 {@link
 * BottomNavigationView} instead. Because the library's tab setters are final, the glass and the
 * fallback are driven through the {@code setCompat*} methods, which the callers use instead of the
 * inherited ones.
 */
public class TabBarCompat extends LiquidGlassTabBar {

  /** Corner radius that renders as a full pill, whatever the bar's height. */
  private static final float PILL_CORNER_RADIUS = 999f;

  private static final float MIN_END_PADDING_DP = 8f;

  private static final int MENU_ID_BASE = 0x7f0f0001;

  private float endPaddingDp = -1f;
  private int baseBottomMargin = -1;

  private BottomNavigationView materialNav;
  private List<TabItem> compatTabs;
  private Function1<Integer, Unit> compatListener;
  private Integer compatSelectedTint;
  private int compatSelectedIndex = -1;

  public TabBarCompat(Context context) {
    this(context, null, 0);
  }

  public TabBarCompat(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TabBarCompat(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    if (!Glass.isGlassEnabled(getContext())) {
      applyMaterialFallback();
      return;
    }
    // A full pill: the curve is what makes a floating bar read as glass rather than a slab.
    setCornerRadius(PILL_CORNER_RADIUS);
    setMaterial(GlassMaterial.REGULAR);
    setEnableSensorHighlight(true);
    setEnableAdaptiveTint(true);
    refresh();
    applyEndPadding();
  }

  /** Re-applies the glass tint, or a real Material 3 navigation bar when glass is turned off. */
  public void refresh() {
    if (!Glass.isGlassEnabled(getContext())) {
      applyMaterialFallback();
      return;
    }
    Glass.applyThemeTint(this);
  }

  // ---------------------------------------------------------------------
  // Compat API: drives the glass bar or the Material fallback.
  // ---------------------------------------------------------------------

  /** The tab entries, whichever bar is active. */
  public void setCompatTabs(List<TabItem> tabs) {
    compatTabs = tabs;
    if (materialNav != null) {
      populateMaterialMenu(tabs);
      return;
    }
    setTabs(tabs);
  }

  /** The selection callback, whichever bar is active. */
  public void setCompatOnTabSelected(Function1<Integer, Unit> listener) {
    compatListener = listener;
    if (materialNav != null) {
      return;
    }
    setOnTabSelected(listener);
  }

  /** Selects a tab, whichever bar is active. */
  public void setCompatSelectedIndex(int index) {
    compatSelectedIndex = index;
    if (materialNav != null) {
      if (index >= 0) {
        materialNav.setSelectedItemId(MENU_ID_BASE + index);
      }
      return;
    }
    setSelectedIndex(index);
  }

  /** The highlight colour, whichever bar is active. */
  public void setCompatSelectedTintColor(Integer color) {
    compatSelectedTint = color;
    if (materialNav != null) {
      applyMaterialTint();
      return;
    }
    setSelectedTintColor(color);
  }

  // ---------------------------------------------------------------------
  // Material 3 fallback
  // ---------------------------------------------------------------------

  private void applyMaterialFallback() {
    Glass.stripGlassEffects(this);
    if (materialNav == null) {
      // The library's tab row and droplet are children of this (Frame)layout. Hide them first so
      // the Material navigation bar below is the only visible child and replaces the glass bar
      // instead of being painted over it.
      Glass.hideGlassChildren(this, null);
      materialNav = new BottomNavigationView(getContext());
      materialNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
      materialNav.setItemHorizontalTranslationEnabled(false);
      addView(
          materialNav,
          new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
      materialNav.setOnItemSelectedListener(
          item -> {
            dispatchMaterialSelection(item);
            return true;
          });
      materialNav.setOnItemReselectedListener(item -> dispatchMaterialSelection(item));
      if (compatTabs != null) {
        populateMaterialMenu(compatTabs);
      }
    }
    applyMaterialTint();
    if (compatSelectedIndex >= 0) {
      materialNav.setSelectedItemId(MENU_ID_BASE + compatSelectedIndex);
    }
  }

  private void dispatchMaterialSelection(MenuItem item) {
    int index = item.getItemId() - MENU_ID_BASE;
    if (compatListener != null && index >= 0) {
      compatListener.invoke(index);
    }
  }

  private void populateMaterialMenu(List<TabItem> tabs) {
    if (materialNav == null || tabs == null) {
      return;
    }
    Menu menu = materialNav.getMenu();
    menu.clear();
    for (int i = 0; i < tabs.size() && i < 5; i++) {
      TabItem tab = tabs.get(i);
      MenuItem item = menu.add(Menu.NONE, MENU_ID_BASE + i, i, tab.getTitle());
      item.setIcon(tab.getIcon());
    }
  }

  private void applyMaterialTint() {
    if (materialNav == null) {
      return;
    }
    M3Theme.bottomNav(materialNav);
    if (compatSelectedTint != null) {
      ColorStateList tint =
          new ColorStateList(
              new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
              new int[] {compatSelectedTint, Glass.onSurfaceVariantColor()});
      materialNav.setItemIconTintList(tint);
      materialNav.setItemTextColor(tint);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (materialNav != null) {
      return;
    }
    // The host Activity's content view has no size yet at attach time, so capturing it here
    // yields an empty backdrop and the panel renders at the wrong scale.
    post(this::refreshGlass);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (materialNav != null) {
      return;
    }
    applyEndPadding();
    // applyEndPadding() can change the padding, which re-enters layout. The pass that settles the
    // final size still has to redraw the panel, otherwise the items keep the geometry of an
    // intermediate size until something else forces a resize.
    post(this::refreshGlass);
  }

  /**
   * Rebuilds the backdrop capture and redraws the panel once the current layout pass is over.
   *
   * <p>{@code LiquidGlassView.onSizeChanged} recycles the cached backdrop and displacement maps, so
   * the capture itself is redone on the next draw. What is missing is that draw: without it the
   * bar keeps painting the last size it was measured at.
   */
  private void refreshGlass() {
    if (!isAttachedToWindow() || materialNav != null) {
      return;
    }
    Glass.autoBackdrop(this);
    invalidate();
  }

  /**
   * Sets how far the first and last tab stay from the ends of the bar. Defaults to half the
   * corner radius, so a rounder bar insets its tabs further.
   */
  public void setEndPaddingDp(float value) {
    endPaddingDp = Math.max(MIN_END_PADDING_DP, value);
    applyEndPadding();
  }

  /**
   * Keeps the system navigation bar away from the glass panel.
   *
   * <p>Applied to the bottom margin, not the padding: padding would grow the panel, and a pill's
   * curve scales with the panel's height, so the ends would eat the outer tabs.
   *
   * <p>Insets can be dispatched more than once (rotation, keyboard, multi-window), so the
   * requested amount is remembered and the margin recomputed from its original value rather
   * than grown on every pass.
   */
  public void applySystemBottomInset(int inset) {
    if (!(getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
      return;
    }
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
    if (baseBottomMargin < 0) {
      baseBottomMargin = params.bottomMargin;
    }

    params.bottomMargin = baseBottomMargin + Math.max(0, inset);
    setLayoutParams(params);
  }

  private void applyEndPadding() {
    if (materialNav != null) {
      return;
    }
    if (endPaddingDp >= 0f) {
      int padding = Glass.dp(this, endPaddingDp);
      setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
      return;
    }
    float radius = getCornerRadius();
    // A pill reports 999f; the curve it actually draws is half the bar's height.
    float effectiveRadius = radius >= PILL_CORNER_RADIUS ? getHeight() / 2f : radius;
    int padding = Glass.dp(this, Math.max(MIN_END_PADDING_DP, effectiveRadius * 0.5f));
    if (getPaddingLeft() < padding) {
      setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
    }
  }
}
