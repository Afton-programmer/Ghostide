package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.content.Context;
import com.example.liquidglass.LiquidGlassView;
import android.util.AttributeSet;
import ir.hanzodev1375.components.glass.Glass;
import ir.hanzodev1375.components.utils.ComponentsPrefs;
import ir.theme.M3Theme;

public class GlassCompat extends LiquidGlassView {
  private ComponentsPrefs setting;

  public GlassCompat(Context c) {
    super(c);
    init();
  }

  public GlassCompat(Context c, AttributeSet s) {
    super(c, s);
    init();
  }

  public GlassCompat(Context c, AttributeSet s, int defStyle) {
    super(c, s, defStyle);
    init();
  }

  void init() {
    setting = new ComponentsPrefs(getContext());
    refresh();
  }

  /** Re-applies the glass tint, or the Material fallback when glass is turned off. */
  public void refresh() {
    if (setting == null) {
      setting = new ComponentsPrefs(getContext());
    }
    if (!setting.isGlassEnabled() || !setting.isGlassMaterialColor()) {
      Glass.applyFallback(this);
      return;
    }
    setGlassColorByCustomHint(M3Theme.surface());
  }

  public void setGlassColorByCustomHint(Integer color) {
    if (color == null || !Glass.isGlassEnabled(getContext())) {
      return;
    }
    setGlassTint(color, setting.getGlassTint());
  }
}
