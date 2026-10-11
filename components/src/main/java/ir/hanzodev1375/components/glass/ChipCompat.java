package ir.hanzodev1375.components.glass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import com.example.liquidglass.GlassMaterial;
import com.example.liquidglass.LiquidGlassChip;
import com.google.android.material.chip.Chip;
import ir.theme.M3Theme;
import java.util.Locale;

/**
 * {@link LiquidGlassChip} that wires its own backdrop, so chips look right inside dialogs and
 * bottom sheets without any manual {@code setBackdropSource(...)} call.
 *
 * <p>When the glass mode is turned off the chip stops rendering glass and hosts a real Material 3
 * {@link Chip} instead, so the widget becomes a genuine Material component rather than a recoloured
 * glass view.
 */
public class ChipCompat extends LiquidGlassChip {

  /**
   * Accent a chip can carry instead of the plain theme surface.
   *
   * <p>The colour itself is resolved in {@link Glass} from the active theme, so a screen only ever
   * names the tone and never picks a hex value.
   */
  public enum Tone {
    DEFAULT,
    GOLD,
    GREEN
  }

  private Tone tone = Tone.DEFAULT;
  private int iconRes;
  private Chip materialChip;

  public ChipCompat(Context context) {
    super(context);
    init();
  }

  public ChipCompat(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ChipCompat(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setEnableDynamicBackground(true);
    setMaterial(GlassMaterial.REGULAR);
    setEnableSensorHighlight(true);
    setEnableAdaptiveTint(true);
    setPressScale(0.96f);
    setTextSize(13f);
    refresh();
  }

  /** Re-applies the glass material, or the Material 3 chip when glass is turned off. */
  public void refresh() {
    Glass.applyChip(this);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    Glass.autoBackdrop(this);
  }

  /**
   * Gives the chip an accent tone, re-resolving the text, icon and glass colours for the current
   * theme. Pass {@link Tone#DEFAULT} to go back to the plain surface.
   */
  public ChipCompat setTone(Tone tone) {
    this.tone = tone == null ? Tone.DEFAULT : tone;
    refresh();
    return this;
  }

  public Tone getTone() {
    return tone;
  }

  /** The leading icon this chip was last given, or 0 when it has none. */
  public int getIconRes() {
    return iconRes;
  }

  /** Convenience alias for {@code setText}. */
  public void setLabel(CharSequence text) {
    setText(text);
  }

  /**
   * Renders a leading icon. Any tint baked into the drawable is cleared so it follows the chip's
   * adaptive light/dark foreground instead of fighting it.
   */
  public void setIconResource(int iconRes) {
    this.iconRes = iconRes;
    if (iconRes == 0) {
      setChipIcon(null);
      hideLibraryIcon();
      syncMaterialIcon();
      return;
    }
    Drawable drawable = AppCompatResources.getDrawable(getContext(), iconRes);
    if (drawable == null) {
      return;
    }
    DrawableCompat.setTintList(drawable, null);
    setChipIcon(drawable);
    hideLibraryIcon();
    syncMaterialIcon();
  }

  /** Icon plus a compact label, e.g. a star count or an issue count. */
  public void setStat(int iconRes, long value) {
    setIconResource(iconRes);
    setText(formatStat(value));
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    syncMaterialState();
    super.dispatchDraw(canvas);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (materialChip != null) {
      materialChip.setEnabled(enabled);
    }
  }

  /** Swaps the glass rendering for a real Material 3 chip. Called by {@link Glass}. */
  void applyMaterialFallback() {
    Glass.stripGlassEffects(this);
    // The library's label, icon and close button live inside its own row; hiding the container
    // removes all of them from layout, so only the Material chip below is measured and drawn.
    Glass.hideGlassChildren(this, materialChip);
    TextView label = getTextView();
    if (materialChip == null) {
      materialChip = new Chip(getContext());
      materialChip.setCheckable(true);
      materialChip.setClickable(false);
      materialChip.setFocusable(false);
      materialChip.setCheckedIconVisible(true);
      addView(materialChip, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      if (label != null) {
        label.addTextChangedListener(
            new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

              @Override
              public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (materialChip != null) {
                  materialChip.setText(s);
                }
              }

              @Override
              public void afterTextChanged(Editable s) {}
            });
      }
    }
    materialChip.setText(getText());
    materialChip.setChecked(isChecked());
    syncMaterialIcon();
    styleMaterialChip();
  }

  private void styleMaterialChip() {
    if (materialChip == null) {
      return;
    }
    M3Theme.chipView(materialChip);
    switch (tone) {
      case GOLD:
        materialChip.setChipBackgroundColor(ColorStateList.valueOf(Glass.gold()));
        materialChip.setTextColor(0xFF1C1B17);
        break;
      case GREEN:
        materialChip.setChipBackgroundColor(ColorStateList.valueOf(Glass.green()));
        materialChip.setTextColor(0xFF0C1F0C);
        break;
      case DEFAULT:
      default:
        break;
    }
  }

  private void syncMaterialIcon() {
    if (materialChip != null) {
      materialChip.setChipIcon(iconRes == 0 ? null : getChipIcon());
    }
  }

  /** Hides the library's own icon view only when it has an icon, so text-only chips stay tight. */
  private void hideLibraryIcon() {
    ImageView iconView = getChipIconView();
    if (iconView != null && iconView.getVisibility() == VISIBLE) {
      iconView.setVisibility(INVISIBLE);
    }
  }

  private void syncMaterialState() {
    if (materialChip == null) {
      return;
    }
    CharSequence label = getText();
    if (!TextUtils.equals(label, materialChip.getText())) {
      materialChip.setText(label);
    }
    if (isChecked() != materialChip.isChecked()) {
      materialChip.setChecked(isChecked());
    }
  }

  /** 1234 becomes "1,234" and anything above 9999 becomes "12.3k". */
  public static String formatStat(long value) {
    if (value < 0) {
      return "-";
    }
    if (value < 10_000) {
      return String.format(Locale.US, "%,d", value);
    }
    return String.format(Locale.US, "%.1fk", value / 1000f);
  }
}
