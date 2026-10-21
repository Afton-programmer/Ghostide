package ir.hanzodev1375.components.input;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.liquidglass.GlassMaterial;
import com.google.android.material.textfield.TextInputLayout;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;

/**
 * Glass variant of Material's {@link TextInputLayout}.
 *
 * <p>It keeps the Material behaviour (floating hint, error text, counters) but draws no Material
 * box of its own: a {@link GlassCompat} panel is inserted at the bottom of the layout's input frame
 * so the field reads as a Material text field over glass. The panel samples the host activity
 * content once, which is the same backdrop the glass dialog around it uses.
 */
public class TextInputGlass extends TextInputLayout {

  private static final float CORNER_RADIUS_DP = 12f;
  private static final float REFRACTION_HEIGHT_DP = 28f;
  private static final float BEVEL_WIDTH_DP = 6f;
  private static final float DISPERSION_STRENGTH = 0.1f;

  private GlassCompat glass;

  public TextInputGlass(@NonNull Context context) {
    super(context);
    init();
  }

  public TextInputGlass(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public TextInputGlass(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setBoxBackgroundMode(BOX_BACKGROUND_NONE);
    setBoxBackgroundColor(Color.TRANSPARENT);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    installGlass();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    installGlass();
  }

  private void installGlass() {
    if (glass != null) {
      return;
    }
    ViewGroup inputFrame = findInputFrame();
    if (inputFrame == null) {
      return;
    }
    GlassCompat glassView = createGlass();
    inputFrame.addView(
        glassView,
        0,
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    glass = glassView;
    attachBackdropSource(glassView);
  }

  private GlassCompat createGlass() {
    float density = getResources().getDisplayMetrics().density;
    GlassCompat glassView = new GlassCompat(getContext());
    glassView.setCornerRadius(CORNER_RADIUS_DP * density);
    glassView.setRefractionHeight(REFRACTION_HEIGHT_DP * density);
    glassView.setBevelWidth(BEVEL_WIDTH_DP * density);
    glassView.setDispersionStrength(DISPERSION_STRENGTH);
    glassView.setMaterial(GlassMaterial.REGULAR);
    glassView.setEnableDynamicBackground(false);
    glassView.setEnableSensorHighlight(false);
    glassView.setEnableAdaptiveTint(true);
    glassView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    return glassView;
  }

  /** The internal frame that hosts the EditText, so the glass spans exactly the input box. */
  @Nullable
  private ViewGroup findInputFrame() {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child instanceof FrameLayout && containsEditText((ViewGroup) child)) {
        return (ViewGroup) child;
      }
    }
    return null;
  }

  private static boolean containsEditText(ViewGroup group) {
    for (int i = 0; i < group.getChildCount(); i++) {
      if (group.getChildAt(i) instanceof EditText) {
        return true;
      }
    }
    return false;
  }

  private void attachBackdropSource(GlassCompat glassView) {
    Activity activity = resolveActivity(getContext());
    if (activity == null) {
      return;
    }
    View content = activity.findViewById(android.R.id.content);
    if (content != null) {
      glassView.setBackdropSource(content);
    }
  }

  @Nullable
  private static Activity resolveActivity(Context context) {
    while (context instanceof ContextWrapper) {
      if (context instanceof Activity) {
        return (Activity) context;
      }
      context = ((ContextWrapper) context).getBaseContext();
    }
    return null;
  }
}
